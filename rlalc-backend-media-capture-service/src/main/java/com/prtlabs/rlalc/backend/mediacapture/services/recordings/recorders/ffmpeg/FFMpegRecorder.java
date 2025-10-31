package com.prtlabs.rlalc.backend.mediacapture.services.recordings.recorders.ffmpeg;

import com.prtlabs.rlalc.backend.mediacapture.utils.RLALCLocalTimeZoneTimeHelper;
import com.prtlabs.rlalc.domain.ProgramId;
import com.prtlabs.utils.exceptions.PrtBaseRuntimeException;
import com.prtlabs.utils.exceptions.PrtTechnicalRuntimeException;
import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.backend.mediacapture.services.recordings.recorders.IMediaRecorder;
import com.prtlabs.rlalc.backend.mediacapture.utils.RecordingManifestUtils;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import com.prtlabs.rlalc.exceptions.RLALCExceptionCodesEnum;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class FFMpegRecorder implements IMediaRecorder {

    private static final Logger logger = LoggerFactory.getLogger(FFMpegRecorder.class);

    private static final String PRTLABS_BASEDIR = System.getProperty("prt.rlalc.baseDir", "/opt/prtlabs");

    // Maps to store recording information
    private static final Map<ProgramId, RecordingId> recordingIdPerProgramId = new ConcurrentHashMap<>();    // There can only be one Recording at a time for a Program (hence the type of recordingIdPerProgramId)
    private static final Map<RecordingId, ProgramId> programIdPerRecordingId = new ConcurrentHashMap<>();
    private static final Map<RecordingId, Process> activeProcesses = new ConcurrentHashMap<>();
    private static final Map<RecordingId, String> recordingPaths = new ConcurrentHashMap<>();
    private static final Map<RecordingId, List<String>> processOutputs = new ConcurrentHashMap<>();

    @Inject private RLALCLocalTimeZoneTimeHelper rLALCLocalTimeZoneTimeHelper;



    @Override
    public void initBeforeRecording(ProgramDescriptorDTO programDescriptor, Map<String, String> recorderSpecificParameters) {
        // Create a RecordingId in the 'rec-<ProgramId>-<epochSec>'
        RecordingId recordingId = buildRecordingId_UsingProgramId_andCurrentDayForProgram(programDescriptor);
        recordingIdPerProgramId.put(programDescriptor.getUuid(), recordingId);
        programIdPerRecordingId.put(recordingId, programDescriptor.getUuid());

        // Create the initial manifest file with PENDING status
        try {
            // Compute the filenames and path for the local storage of chunks
            FileInfoForRecordingStorage fileInfoForRecordingStorage = buildFileInfoForRecordingStorage(programDescriptor);
            Files.createDirectories(Paths.get(fileInfoForRecordingStorage.outputDir));
            // Create the initial manifest file with PENDING status
            RecordingManifestUtils.createOrUpdateManifest(fileInfoForRecordingStorage.outputDir, RecordingStatus.Status.PENDING, null, null);
        } catch (IOException ioex) {
            throw new PrtTechnicalRuntimeException(RLALCExceptionCodesEnum.RLAC_006_FailedToStartRecordingForProgram.name(), "Failed to prepare recording for Program=["+programDescriptor.getTitle()+"] with message=["+ioex.getMessage()+"]", ioex);
        }
    }

    @Override
    public void startRecording(ProgramDescriptorDTO programDescriptor, Map<String, String> recorderSpecificParameters) {
        logger.info("Starting recording for program [{}] with UUID [{}]", programDescriptor.getTitle(), programDescriptor.getUuid());

        try {
            // Create a RecordingId in the 'rec-<ProgramId>-<epochSec>'
            RecordingId recordingId = buildRecordingId_UsingProgramId_andCurrentDayForProgram(programDescriptor);

            // Create a clean filename from the program descriptor and create the full output path for ffmpeg
            FileInfoForRecordingStorage fileInfoForRecordingStorage = buildFileInfoForRecordingStorage(programDescriptor);
            String outputPathPattern = fileInfoForRecordingStorage.outputDir + "/" + fileInfoForRecordingStorage.recordingBaseName + "_chunk_%Y%m%d_%H%M%S.mp3";

            // Check that manifest for this recording already exists (which means that the prepare has already been called)
            if (!RecordingManifestUtils.checkIfManifestExistAndHasStatus(fileInfoForRecordingStorage.outputDir, RecordingStatus.Status.PENDING)) {
                throw new PrtTechnicalRuntimeException(RLALCExceptionCodesEnum.RLAC_008_RecordingHasNotBeenInitializedForProgram.name());
            }

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
                            fileInfoForRecordingStorage.outputDir,
                            RecordingStatus.Status.PARTIAL_FAILURE, 
                            outputLines, 
                            null
                        );
                    }
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for ffmpeg process: {}", e.getMessage(), e);
                Thread.currentThread().interrupt();
            }

            // Store the process and path information
            if (!hasImmediateError) {
                activeProcesses.put(recordingId, process);
                recordingPaths.put(recordingId, fileInfoForRecordingStorage.outputDir);

                // Update manifest to ONGOING status
                RecordingManifestUtils.updateStatus(fileInfoForRecordingStorage.outputDir, RecordingStatus.Status.ONGOING);
            }

            logger.info("Recording started for program [{}] with recording ID [{}]", programDescriptor.getTitle(), recordingId);

        } catch (IOException e) {
            String message = String.format("Failed to start recording for program=[{}] with message=[{}]", programDescriptor.getTitle(), e.getMessage());
            logger.error(message, e);
            throw new PrtTechnicalRuntimeException(RLALCExceptionCodesEnum.RLAC_006_FailedToStartRecordingForProgram.name(), message, e);
        }
    }

    @Override
    public void stopRecording(ProgramId programId) {
        if (programId == null) { logger.warn("Cannot stop recording with null recording ID"); return; }

        logger.info("Stopping recording with ID [{}]", programId);

        // Get the path for this recording
        String outputDir = recordingPaths.get(programId);
        Process process = activeProcesses.get(programId);
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
                        List<String> capturedOutput = processOutputs.get(programId);

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
                        List<String> capturedOutput = processOutputs.get(programId);

                        // Update the manifest
                        RecordingManifestUtils.createOrUpdateManifest(
                            outputDir, RecordingStatus.Status.COMPLETED, capturedOutput, chunkFiles);
                    }
                }

                // Remove from active recordings and clean up
                activeProcesses.remove(programId);
                processOutputs.remove(programId);
                logger.info("Recording stopped successfully");

            } catch (InterruptedException e) {
                logger.error("Error while stopping recording: {}", e.getMessage(), e);

                // Update manifest with error if we have a path
                if (outputDir != null) {
                    // Get any captured output for this recording
                    List<String> capturedOutput = processOutputs.get(programId);

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
                processOutputs.remove(programId);
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
                List<String> capturedOutput = processOutputs.get(programId);

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
                processOutputs.remove(programId);
            }
        }
    }

    @Override
    public Map<ProgramId, RecordingStatus> getRecordingStatuses() {
        Map<ProgramId, RecordingStatus> statuses = new HashMap<>();

        // Iterate through all recording paths and read their manifests
        for (Map.Entry<RecordingId, String> entry : recordingPaths.entrySet()) {
            RecordingId recordingId = entry.getKey();
            String outputDir = entry.getValue();

            try {
                // Read the manifest file to get the recording status
                RecordingStatus status = RecordingManifestUtils.readManifest(outputDir);
                if (status != null) {
                    statuses.put(programIdPerRecordingId.get(recordingId), status);
                }
            } catch (Exception e) {
                logger.error("Error reading manifest for recording {}: {}", recordingId, e.getMessage(), e);
                // Create a status with error information
                RecordingStatus errorStatus = new RecordingStatus(RecordingStatus.Status.PARTIAL_FAILURE);
                errorStatus.addError("Failed to read manifest: " + e.getMessage());
                statuses.put(programIdPerRecordingId.get(recordingId), errorStatus);
            }
        }

        return statuses;
    }

    @Override
    public List<File> getChunkFiles(ProgramId programId, Instant day) {
        // Find the current recordingId matching that programId
        RecordingId recordingIdForProgram = recordingIdPerProgramId.get(programId);
        if (recordingIdForProgram == null) { throw new PrtBaseRuntimeException(RLALCExceptionCodesEnum.RLAC_003_NoRecordingStartedForProgram.name(), "No planned RecordingIds found"); }

        // Get the path for this recording
        String outputDir = recordingPaths.get(recordingIdForProgram);
        if (outputDir == null) { throw new PrtBaseRuntimeException(RLALCExceptionCodesEnum.RLAC_003_NoRecordingStartedForProgram.name(), "Recording storage path not created"); }

        // Look for the files
        try {
            Path dirPath = Paths.get(outputDir);
            if (!Files.exists(dirPath)) { throw new PrtBaseRuntimeException(RLALCExceptionCodesEnum.RLAC_004_NoRecordingsStorageFoundForProgram.name(), "Recording storage path=["+outputDir+"] not found"); }

            // Get the path for the day, with date format=YYYYMMDD
            String dayDirName = this.getDirNameForDay(day);
            Path dirForDayPath = dirPath.resolve(dayDirName);
            if (!Files.exists(dirForDayPath)) { throw new PrtBaseRuntimeException(RLALCExceptionCodesEnum.RLAC_004_NoRecordingsStorageFoundForProgram.name(), "Recording storage path for that day=["+dirForDayPath.toFile().getAbsolutePath()+"] not found"); }

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

            return matchingFiles;
        } catch (IOException e) {
            throw new PrtTechnicalRuntimeException(RLALCExceptionCodesEnum.RLAC_005_FailedToAccessMediaChunks.name(), "Failed to find media chunks for message=["+e.getMessage()+"]", e);
        }
    }






    //
    //
    // IMPLEMENTATION
    //
    //

    private static record RecordingId(String uuid) {}    // RecordingId is an internal concept. Outside of the Recorders, everything is identified by the ProgramIds (not RecordingIds)
    private static record FileInfoForRecordingStorage(String recordingBaseName, String outputDir) {}


    private String getDirNameForDay(Instant day) {
        return DateTimeFormatter.ofPattern("yyyyMMdd").format(day.atZone(ZoneOffset.UTC));
    }

    /**
     * Build a RecordingId that includes the programID and the currentDay for the program
     * @param programDescriptor
     * @return
     */
    private RecordingId buildRecordingId_UsingProgramId_andCurrentDayForProgram(ProgramDescriptorDTO programDescriptor) {
        String currentDayForProgramAsYYYYMMDD = rLALCLocalTimeZoneTimeHelper.getCurrentDayForProgramAsYYYYMMDD(programDescriptor.getTimeZone());
        return new RecordingId("rec-" + programDescriptor.getUuid() + "-" + currentDayForProgramAsYYYYMMDD);
    }

    /**
     * Build a "filename-compatible" name for the directory storing the recording, and the outputDir where chunks will
     * be stored for this Recording of this Program
     * @param programDescriptor
     * @return
     */
    private FileInfoForRecordingStorage buildFileInfoForRecordingStorage(ProgramDescriptorDTO programDescriptor) {
        String recordingBaseName = programDescriptor.getUuid().uuid().toLowerCase() + "-" +
            programDescriptor.getTitle().toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_");
        String outputDir = PRTLABS_BASEDIR + "/radiolivealacarte/datastore/media/mp3/" + recordingBaseName;
        return new FileInfoForRecordingStorage(recordingBaseName, outputDir);
    }

}
