package com.prtlabs.rlalc.backend.mediacapture.services.recordings.statemanagement.manifests;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import com.prtlabs.rlalc.exceptions.RLALCExceptionCodesEnum;
import com.prtlabs.utils.exceptions.PrtTechnicalRuntimeException;
import com.prtlabs.utils.json.PrtJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for handling recording manifest files.
 */
public class RecordingManifestUtils {

    private static final Logger logger = LoggerFactory.getLogger(RecordingManifestUtils.class);
    private static final String MANIFEST_FILENAME = "recording-manifest.json";
    private static final ObjectMapper objectMapper = PrtJsonUtils.getFasterXmlObjectMapper();

    /**
     * Creates or updates the manifest file for a recording.
     *
     * @param outputDir the directory where the recording chunks are stored
     * @param status    the current status of the recording
     * @param errors    the list of errors (if any)
     * @param chunkList the list of chunk files
     * @return true if the manifest was successfully created/updated, false otherwise
     */
    public static boolean createOrUpdateManifest(String outputDir, RecordingStatus.Status status, List<String> errors, List<File> chunkList) {
        try {
            Path manifestPath = Paths.get(outputDir, MANIFEST_FILENAME);

            // Create the JSON object
            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("status", status.toString().toLowerCase());

            // Add errors if present
            if (errors != null && !errors.isEmpty()) {
                ArrayNode errorsNode = rootNode.putArray("errors");
                for (String error : errors) {
                    errorsNode.add(error);
                }
            }

            // Add chunk list
            ArrayNode chunksNode = rootNode.putArray("chunkList");
            if (chunkList != null) {
                for (File chunk : chunkList) {
                    chunksNode.add(chunk.getPath());
                }
            }

            // Write to file with pretty printing
            objectMapper.writer(new DefaultPrettyPrinter().withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE))
                .writeValue(manifestPath.toFile(), rootNode);


            return true;
        } catch (IOException e) {
            logger.error("Failed to create or update manifest file in {}: {}", outputDir, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Reads the manifest file and returns a RecordingStatus object.
     *
     * @param outputDir the directory where the recording chunks are stored
     * @return a RecordingStatus object, or null if the manifest couldn't be read
     */
    public static RecordingStatus readManifest(String outputDir) {
        try {
            Path manifestPath = Paths.get(outputDir, MANIFEST_FILENAME);

            // If the manifest doesn't exist, return a default status
            if (!Files.exists(manifestPath)) {
                logger.warn("Manifest file does not exist in {}", outputDir);
                return new RecordingStatus(RecordingStatus.Status.PENDING);
            }

            // Read the manifest file
            ObjectNode rootNode = (ObjectNode) objectMapper.readTree(manifestPath.toFile());

            // Parse status
            RecordingStatus.Status status = RecordingStatus.Status.PENDING;
            if (rootNode.has("status")) {
                String statusStr = rootNode.get("status").asText().toUpperCase();
                try {
                    status = RecordingStatus.Status.valueOf(statusStr);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid status in manifest: {}", statusStr);
                }
            }

            // Parse errors
            List<String> errors = new ArrayList<>();
            if (rootNode.has("errors")) {
                ArrayNode errorsNode = (ArrayNode) rootNode.get("errors");
                for (int i = 0; i < errorsNode.size(); i++) {
                    errors.add(errorsNode.get(i).asText());
                }
            }

            // Parse chunk list
            List<File> chunkList = new ArrayList<>();
            if (rootNode.has("chunkList")) {
                ArrayNode chunksNode = (ArrayNode) rootNode.get("chunkList");
                for (int i = 0; i < chunksNode.size(); i++) {
                    chunkList.add(new File(chunksNode.get(i).asText()));
                }
            }

            return new RecordingStatus(status, errors, chunkList);
        } catch (IOException e) {
            logger.error("Failed to read manifest file in {}: {}", outputDir, e.getMessage(), e);
            return new RecordingStatus(RecordingStatus.Status.PENDING);
        }
    }

    /**
     * Updates the status in the manifest file (without stdour/stderr capture)
     *
     * @param programDescriptor
     * @param ffmpegProcessId
     * @param ffmpegProcessExitValue
     * @param outputDirForRecordingChunks
     * @param ffmpegStdoutStdErrCapture
     * @return true if the status was successfully updated, false otherwise
     */
    public static void updateStatus(ProgramDescriptorDTO programDescriptor, Optional<Long> ffmpegProcessId, Optional<Integer> ffmpegProcessExitValue, String outputDirForRecordingChunks) {
        updateStatus(programDescriptor, ffmpegProcessId, ffmpegProcessExitValue, outputDirForRecordingChunks, null);
    }

    /**
     * Updates the status in the manifest file, with stdour/stderr capture
     *
     * @param programDescriptor
     * @param ffmpegProcessId
     * @param ffmpegProcessExitValue
     * @param outputDirForRecordingChunks
     * @param ffmpegStdoutStdErrCapture
     * @return true if the status was successfully updated, false otherwise
     */
    public static void updateStatus(ProgramDescriptorDTO programDescriptor, Optional<Long> ffmpegProcessId, Optional<Integer> ffmpegProcessExitValue, String outputDirForRecordingChunks, List<String> ffmpegStdoutStdErrCapture) {
        // Compute the RecordingStatus based on the process presence and exitValue
        //  - If we have a process exit value for ffmpeg, the status is either COMPLETED or PARTIAL_FAILURE
        //  - If we don't have an exit code, either the process is not started (the status is PENDING) or it's already started (the status is ONGOING)
        RecordingStatus.Status status = null;
        if (ffmpegProcessExitValue.isPresent()) {
            status = (ffmpegProcessExitValue.get()==0) ? RecordingStatus.Status.COMPLETED : RecordingStatus.Status.PARTIAL_FAILURE;
        } else {
            status = (ffmpegProcessId.isPresent()) ? RecordingStatus.Status.ONGOING : RecordingStatus.Status.PENDING;
        }

        // Look for audio chunks
        List<File> audioChunks = gatherChunkFile(outputDirForRecordingChunks);
        List<String> errors = ((ffmpegStdoutStdErrCapture != null) && (!ffmpegStdoutStdErrCapture.isEmpty())) ? ffmpegStdoutStdErrCapture : null;

        createOrUpdateManifest(outputDirForRecordingChunks, status, errors, audioChunks);
    }

    /**
     *
     * @param outputDir
     * @param capturedOutput
     * @param finalStatus
     */
    public static List<File> gatherChunkFile(String outputDir) {
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
            return chunkFiles;
        } catch (IOException ex) {
            throw new PrtTechnicalRuntimeException(RLALCExceptionCodesEnum.RLAC_005_FailedToAccessMediaChunks.name(), "Failed to gather audio chunk files with message=["+ex.getMessage()+"]", ex);
        }
    }

}
