package com.prtlabs.rlalc.backend.mediacapture.services.recorders.ffmpeg;

import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.backend.mediacapture.services.recorders.IMediaRecorder;
import com.prtlabs.rlalc.backend.mediacapture.utils.RecordingManifestUtils;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FFMpegRecorder implements IMediaRecorder {

    private static final Logger logger = LoggerFactory.getLogger(FFMpegRecorder.class);

    private static final String PRTLABS_BASEDIR = System.getProperty("prt.rlalc.baseDir", "/opt/prtlabs");

    // Maps to store recording information
    private static final Map<String, Process> activeProcesses = new ConcurrentHashMap<>();
    private static final Map<String, String> recordingPaths = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> processOutputs = new ConcurrentHashMap<>();


    @Override
    public String record(ProgramDescriptorDTO programDescriptor, Map<String, String> recorderSpecificParameters) {
        logger.info("Starting recording for program [{}] with UUID [{}]", programDescriptor.getTitle(), programDescriptor.getUuid());

        try {
            // Create a RecordingId
            String recordingId = UUID.randomUUID().toString();

            // Create a clean filename from the program descriptor
            String recordingBaseName = programDescriptor.getUuid().toLowerCase() + "-" +
                                      programDescriptor.getTitle().toLowerCase()
                                      .replaceAll("[^a-z0-9]", "_")
                                      .replaceAll("_+", "_");

            // Create date prefix for organization
            LocalDate today = LocalDate.now();
            String dataPrefix = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

            // Create the output directory
            String outputDir = PRTLABS_BASEDIR + "/radiolivealacarte/datastore/media/mp3/" + recordingBaseName;
            Path outputPath = Paths.get(outputDir);
            Files.createDirectories(outputPath);

            // Create the full output path for ffmpeg
            String outputPathPattern = outputDir + "/" + recordingBaseName + "_chunk_%Y%m%d_%H%M%S.mp3";

            // Create initial manifest file with PENDING status
            RecordingManifestUtils.createOrUpdateManifest(outputDir, RecordingStatus.Status.PENDING, null, null);

            // Build the ffmpeg command
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(programDescriptor.getStreamURL());
            command.add("-c:a");
            command.add("libmp3lame");
            command.add("-b:a");
            command.add("160k");
            command.add("-f");
            command.add("mp3");
            command.add("-f");
            command.add("segment");
            command.add("-segment_time");
            command.add("10");
            command.add("-strftime");      // To allow timestamp inside segment filenames
            command.add("1");
            command.add("-ar");
            command.add("32000");
            command.add(outputPathPattern);

            // Log the ffmpeg command with parameters
            logger.info("Executing ffmpeg command: [{}]", String.join(" ", command));

            // Start the ffmpeg process
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Initialize the output list for this process
            List<String> outputLines = new ArrayList<>();
            processOutputs.put(recordingId, outputLines);

            // Start a thread to read the process output
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputLines.add(line);
                        logger.debug("FFmpeg output: {}", line);
                    }
                } catch (IOException e) {
                    logger.error("Error reading ffmpeg output: {}", e.getMessage(), e);
                }
            });
            outputThread.setDaemon(true);
            outputThread.start();

            // Check for immediate errors (wait a short time to see if process fails immediately)
            boolean hasImmediateError = false;
            try {
                if (process.waitFor(2, TimeUnit.SECONDS)) {
                    int exitCode = process.exitValue();
                    if (exitCode != 0) {
                        hasImmediateError = true;
                        logger.error("FFmpeg process failed immediately with exit code: {}", exitCode);

                        // Wait a bit for output thread to collect error messages
                        Thread.sleep(100);

                        // Update manifest with PARTIAL_FAILURE status and errors
                        RecordingManifestUtils.createOrUpdateManifest(
                            outputDir, 
                            RecordingStatus.Status.PARTIAL_FAILURE, 
                            outputLines, 
                            null
                        );

                        // Return the recording ID anyway so the caller can query the status
                        return recordingId;
                    }
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for ffmpeg process: {}", e.getMessage(), e);
                Thread.currentThread().interrupt();
            }

            // Store the process and path information
            if (!hasImmediateError) {
                activeProcesses.put(recordingId, process);
                recordingPaths.put(recordingId, outputDir);

                // Update manifest to ONGOING status
                RecordingManifestUtils.updateStatus(outputDir, RecordingStatus.Status.ONGOING);
            }

            logger.info("Recording started for program [{}] with recording ID [{}]", programDescriptor.getTitle(), recordingId);
            return recordingId;

        } catch (IOException e) {
            logger.error("Failed to start recording for program [{}]: {}", programDescriptor.getTitle(), e.getMessage(), e);
            return null;
        }
    }

    @Override
    public RecordingStatus getChunkFilesForRecording(String recordingId) {
        if (recordingId == null) {
            logger.warn("Cannot get chunk files for null recording ID");
            return new RecordingStatus(RecordingStatus.Status.PENDING);
        }

        // Get the path for this recording
        String outputDir = recordingPaths.get(recordingId);
        if (outputDir == null) {
            logger.warn("No path found for recording ID");
            return new RecordingStatus(RecordingStatus.Status.PENDING);
        }

        try {
            Path dirPath = Paths.get(outputDir);
            if (!Files.exists(dirPath)) {
                logger.warn("Output directory does not exist: {}", outputDir);
                return new RecordingStatus(RecordingStatus.Status.PENDING);
            }

            // Get all MP3 files in the directory
            List<File> matchingFiles;
            try (Stream<Path> files = Files.list(dirPath)) {
                matchingFiles = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".mp3"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

                if (matchingFiles.isEmpty()) {
                    logger.warn("No chunk files found for recording in directory: {}", outputDir);
                }
            }

            // Check if there's an existing manifest file
            RecordingStatus status = RecordingManifestUtils.readManifest(outputDir);

            // Get any captured output for this recording
            List<String> capturedOutput = processOutputs.get(recordingId);

            // If no manifest exists, create one based on the current state
            if (status.getStatus() == RecordingStatus.Status.PENDING) {
                // Determine the status based on the active processes
                RecordingStatus.Status currentStatus;
                if (activeProcesses.containsKey(recordingId)) {
                    currentStatus = RecordingStatus.Status.ONGOING;
                } else if (!matchingFiles.isEmpty()) {
                    currentStatus = RecordingStatus.Status.COMPLETED;
                } else {
                    currentStatus = RecordingStatus.Status.PENDING;
                }

                // Create the manifest file
                RecordingManifestUtils.createOrUpdateManifest(outputDir, currentStatus, capturedOutput, matchingFiles);

                // Update the status object
                status.setStatus(currentStatus);
                status.setChunkList(matchingFiles);
                if (capturedOutput != null && !capturedOutput.isEmpty()) {
                    status.setErrors(capturedOutput);
                }
            } else {
                // Update the chunk list and errors in the manifest if needed
                boolean needsUpdate = !matchingFiles.equals(status.getChunkList());

                // Check if we have new output to add to errors
                if (capturedOutput != null && !capturedOutput.isEmpty()) {
                    List<String> currentErrors = status.getErrors();
                    if (currentErrors == null || currentErrors.isEmpty()) {
                        status.setErrors(capturedOutput);
                        needsUpdate = true;
                    } else if (currentErrors.size() != capturedOutput.size() || 
                              !currentErrors.containsAll(capturedOutput)) {
                        // Only update if there are new lines
                        status.setErrors(capturedOutput);
                        needsUpdate = true;
                    }
                }

                if (needsUpdate) {
                    status.setChunkList(matchingFiles);
                    RecordingManifestUtils.createOrUpdateManifest(
                        outputDir, status.getStatus(), status.getErrors(), matchingFiles);
                }
            }

            return status;

        } catch (IOException e) {
            logger.error("Error while searching for chunk files: {}", e.getMessage(), e);

            // Create a status with an error
            RecordingStatus status = new RecordingStatus(RecordingStatus.Status.PARTIAL_FAILURE);
            status.addError("Error while searching for chunk files: " + e.getMessage());

            // Try to update the manifest file
            try {
                RecordingManifestUtils.addError(outputDir, "Error while searching for chunk files: " + e.getMessage());
            } catch (Exception ex) {
                logger.error("Failed to update manifest with error: {}", ex.getMessage(), ex);
            }

            return status;
        }
    }

    @Override
    public void stopRecording(String recordingId) {
        if (recordingId == null) {
            logger.warn("Cannot stop recording with null recording ID");
            return;
        }

        logger.info("Stopping recording with ID [{}]", recordingId != null ? recordingId : "unknown");

        // Get the path for this recording
        String outputDir = recordingPaths.get(recordingId);

        Process process = activeProcesses.get(recordingId);
        if (process != null) {
            try {
                // Send SIGTERM to allow ffmpeg to clean up resources
                process.destroy();

                // Wait for the process to terminate
                boolean terminated = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

                if (!terminated) {
                    // If it didn't terminate gracefully, force it
                    logger.warn("FFMPEG process did not terminate gracefully, forcing termination");
                    process.destroyForcibly();

                    // Update manifest with partial failure if we have a path
                    if (outputDir != null) {
                        // Get any captured output for this recording
                        List<String> capturedOutput = processOutputs.get(recordingId);

                        // Create a list for errors if we don't have one yet
                        if (capturedOutput == null) {
                            capturedOutput = new ArrayList<>();
                        }

                        // Add the error message
                        capturedOutput.add("FFMPEG process did not terminate gracefully");

                        // Get the current chunk files
                        List<File> chunkFiles = new ArrayList<>();
                        try {
                            Path dirPath = Paths.get(outputDir);
                            if (Files.exists(dirPath)) {
                                try (Stream<Path> files = Files.list(dirPath)) {
                                    chunkFiles = files
                                        .filter(Files::isRegularFile)
                                        .filter(p -> p.getFileName().toString().endsWith(".mp3"))
                                        .map(Path::toFile)
                                        .collect(Collectors.toList());
                                }
                            }
                        } catch (IOException e) {
                            logger.error("Error while getting chunk files for manifest update: {}", e.getMessage(), e);
                        }

                        // Update the manifest with PARTIAL_FAILURE status and errors
                        RecordingManifestUtils.createOrUpdateManifest(
                            outputDir, 
                            RecordingStatus.Status.PARTIAL_FAILURE, 
                            capturedOutput, 
                            chunkFiles
                        );
                    }
                } else {
                    // Update manifest with completed status if we have a path
                    if (outputDir != null) {
                        // Get the current chunk files to update the manifest
                        List<File> chunkFiles = new ArrayList<>();
                        try {
                            Path dirPath = Paths.get(outputDir);
                            if (Files.exists(dirPath)) {
                                try (Stream<Path> files = Files.list(dirPath)) {
                                    chunkFiles = files
                                        .filter(Files::isRegularFile)
                                        .filter(p -> p.getFileName().toString().endsWith(".mp3"))
                                        .map(Path::toFile)
                                        .collect(Collectors.toList());
                                }
                            }
                        } catch (IOException e) {
                            logger.error("Error while getting chunk files for manifest update: {}", e.getMessage(), e);
                        }

                        // Get any captured output for this recording
                        List<String> capturedOutput = processOutputs.get(recordingId);

                        // Update the manifest
                        RecordingManifestUtils.createOrUpdateManifest(
                            outputDir, RecordingStatus.Status.COMPLETED, capturedOutput, chunkFiles);
                    }
                }

                // Remove from active recordings and clean up
                activeProcesses.remove(recordingId);
                processOutputs.remove(recordingId);
                logger.info("Recording stopped successfully");

            } catch (InterruptedException e) {
                logger.error("Error while stopping recording: {}", e.getMessage(), e);

                // Update manifest with error if we have a path
                if (outputDir != null) {
                    // Get any captured output for this recording
                    List<String> capturedOutput = processOutputs.get(recordingId);

                    // Create a list for errors if we don't have one yet
                    if (capturedOutput == null) {
                        capturedOutput = new ArrayList<>();
                    }

                    // Add the error message
                    capturedOutput.add("Error while stopping recording: " + e.getMessage());

                    // Get the current chunk files
                    List<File> chunkFiles = new ArrayList<>();
                    try {
                        Path dirPath = Paths.get(outputDir);
                        if (Files.exists(dirPath)) {
                            try (Stream<Path> files = Files.list(dirPath)) {
                                chunkFiles = files
                                    .filter(Files::isRegularFile)
                                    .filter(p -> p.getFileName().toString().endsWith(".mp3"))
                                    .map(Path::toFile)
                                    .collect(Collectors.toList());
                            }
                        }
                    } catch (IOException ex) {
                        logger.error("Error while getting chunk files for manifest update: {}", ex.getMessage(), ex);
                    }

                    // Update the manifest with PARTIAL_FAILURE status and errors
                    RecordingManifestUtils.createOrUpdateManifest(
                        outputDir, 
                        RecordingStatus.Status.PARTIAL_FAILURE, 
                        capturedOutput, 
                        chunkFiles
                    );
                }

                // Clean up
                processOutputs.remove(recordingId);
                Thread.currentThread().interrupt();
            }
        } else {
            logger.warn("No active recording process found");

            // If we have a path but no process, update the manifest anyway
            if (outputDir != null) {
                // Get the current chunk files
                List<File> chunkFiles = new ArrayList<>();
                try {
                    Path dirPath = Paths.get(outputDir);
                    if (Files.exists(dirPath)) {
                        try (Stream<Path> files = Files.list(dirPath)) {
                            chunkFiles = files
                                .filter(Files::isRegularFile)
                                .filter(p -> p.getFileName().toString().endsWith(".mp3"))
                                .map(Path::toFile)
                                .collect(Collectors.toList());
                        }
                    }
                } catch (IOException e) {
                    logger.error("Error while getting chunk files for manifest update: {}", e.getMessage(), e);
                }

                // Get any captured output for this recording
                List<String> capturedOutput = processOutputs.get(recordingId);

                // If we have chunks, mark as completed, otherwise as partial failure
                RecordingStatus.Status finalStatus = chunkFiles.isEmpty() ? 
                    RecordingStatus.Status.PARTIAL_FAILURE : RecordingStatus.Status.COMPLETED;

                if (finalStatus == RecordingStatus.Status.PARTIAL_FAILURE) {
                    // Create a list for errors if we don't have one yet
                    if (capturedOutput == null) {
                        capturedOutput = new ArrayList<>();
                    }

                    // Add the error message
                    capturedOutput.add("No active recording process found but recording was requested to stop");

                    // Update the manifest with the error
                    RecordingManifestUtils.createOrUpdateManifest(outputDir, finalStatus, capturedOutput, chunkFiles);
                } else {
                    RecordingManifestUtils.createOrUpdateManifest(outputDir, finalStatus, capturedOutput, chunkFiles);
                }

                // Clean up
                processOutputs.remove(recordingId);
            }
        }
    }
}
