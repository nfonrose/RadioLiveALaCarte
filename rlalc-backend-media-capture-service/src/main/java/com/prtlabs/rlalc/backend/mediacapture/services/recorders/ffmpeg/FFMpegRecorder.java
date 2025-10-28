package com.prtlabs.rlalc.backend.mediacapture.services.recorders.ffmpeg;

import com.prtlabs.rlalc.backend.mediacapture.services.recorders.IMediaRecorder;
import com.prtlabs.rlalc.domain.ProgramDescriptor;
import com.prtlabs.rlalc.domain.RecordingId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FFMpegRecorder implements IMediaRecorder {

    private static final Logger logger = LoggerFactory.getLogger(FFMpegRecorder.class);
    private static final String BASE_OUTPUT_DIR = "/opt/prtlabs/rlalc/datastore/media/mp3/";

    // Maps to store recording information
    private static final Map<RecordingId, Process> activeProcesses = new ConcurrentHashMap<>();
    private static final Map<RecordingId, String> recordingPaths = new ConcurrentHashMap<>();
    private static final Map<RecordingId, String> recordingIds = new ConcurrentHashMap<>();

    @Override
    public RecordingId record(ProgramDescriptor programDescriptor, Map<String, String> recorderSpecificParameters) {
        logger.info("Starting recording for program [{}] with UUID [{}]", programDescriptor.name, programDescriptor.uuid);

        try {
            // Create a RecordingId
            RecordingId recordingId = new RecordingId();
            String recordingUuid = UUID.randomUUID().toString();

            // Create a clean filename from the program descriptor
            String recordingBaseName = programDescriptor.uuid.toLowerCase() + "-" + 
                                      programDescriptor.name.toLowerCase()
                                      .replaceAll("[^a-z0-9]", "_")
                                      .replaceAll("_+", "_");

            // Create date prefix for organization
            LocalDate today = LocalDate.now();
            String dataPrefix = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

            // Create the output directory
            String outputDir = BASE_OUTPUT_DIR + recordingBaseName;
            Path outputPath = Paths.get(outputDir);
            Files.createDirectories(outputPath);

            // Create the full output path for ffmpeg
            String outputPathPattern = outputDir + "/" + recordingBaseName + "_chunk_%Y%m%d_%H%M%S.mp3";

            // Build the ffmpeg command
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(programDescriptor.streamURL);
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
            command.add("-ar");
            command.add("32000");
            command.add(outputPathPattern);

            // Start the ffmpeg process
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Store the process and path information
            activeProcesses.put(recordingId, process);
            recordingPaths.put(recordingId, outputDir);
            recordingIds.put(recordingId, recordingUuid);

            logger.info("Recording started for program [{}] with recording ID [{}]", programDescriptor.name, recordingUuid);
            return recordingId;

        } catch (IOException e) {
            logger.error("Failed to start recording for program [{}]: {}", programDescriptor.name, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<File> getChunkFilesForRecording(RecordingId recordingId) {
        if (recordingId == null) {
            logger.warn("Cannot get chunk files for null recording ID");
            return List.of();
        }

        // Get the path for this recording
        String outputDir = recordingPaths.get(recordingId);
        if (outputDir == null) {
            logger.warn("No path found for recording ID");
            return List.of();
        }

        try {
            Path dirPath = Paths.get(outputDir);
            if (!Files.exists(dirPath)) {
                logger.warn("Output directory does not exist: {}", outputDir);
                return List.of();
            }

            // Get all MP3 files in the directory
            try (Stream<Path> files = Files.list(dirPath)) {
                List<File> matchingFiles = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".mp3"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

                if (matchingFiles.isEmpty()) {
                    logger.warn("No chunk files found for recording in directory: {}", outputDir);
                }

                return matchingFiles;
            }

        } catch (IOException e) {
            logger.error("Error while searching for chunk files: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public void stopRecording(RecordingId recordingId) {
        if (recordingId == null) {
            logger.warn("Cannot stop recording with null recording ID");
            return;
        }

        String recordingUuid = recordingIds.get(recordingId);
        logger.info("Stopping recording with ID [{}]", recordingUuid != null ? recordingUuid : "unknown");

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
                }

                // Remove from active recordings
                activeProcesses.remove(recordingId);
                logger.info("Recording stopped successfully");

            } catch (InterruptedException e) {
                logger.error("Error while stopping recording: {}", e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        } else {
            logger.warn("No active recording process found");
        }
    }
}
