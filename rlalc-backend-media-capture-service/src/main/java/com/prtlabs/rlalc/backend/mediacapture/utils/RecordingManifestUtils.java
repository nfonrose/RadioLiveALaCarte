package com.prtlabs.rlalc.backend.mediacapture.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
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
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), rootNode);

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
     * Updates the status in the manifest file.
     *
     * @param outputDir the directory where the recording chunks are stored
     * @param status    the new status
     * @return true if the status was successfully updated, false otherwise
     */
    public static boolean updateStatus(String outputDir, RecordingStatus.Status status) {
        try {
            Path manifestPath = Paths.get(outputDir, MANIFEST_FILENAME);

            // If the manifest doesn't exist, create it
            if (!Files.exists(manifestPath)) {
                return createOrUpdateManifest(outputDir, status, null, null);
            }

            // Read the existing manifest
            ObjectNode rootNode = (ObjectNode) objectMapper.readTree(manifestPath.toFile());

            // Update the status
            rootNode.put("status", status.toString().toLowerCase());

            // Write back to file
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), rootNode);

            return true;
        } catch (IOException e) {
            logger.error("Failed to update status in manifest file in {}: {}", outputDir, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Adds an error to the manifest file.
     *
     * @param outputDir the directory where the recording chunks are stored
     * @param error     the error to add
     * @return true if the error was successfully added, false otherwise
     */
    public static boolean addError(String outputDir, String error) {
        try {
            Path manifestPath = Paths.get(outputDir, MANIFEST_FILENAME);

            // If the manifest doesn't exist, create it with the error
            if (!Files.exists(manifestPath)) {
                List<String> errors = new ArrayList<>();
                errors.add(error);
                return createOrUpdateManifest(outputDir, RecordingStatus.Status.PARTIAL_FAILURE, errors, null);
            }

            // Read the existing manifest
            ObjectNode rootNode = (ObjectNode) objectMapper.readTree(manifestPath.toFile());

            // Add the error
            ArrayNode errorsNode;
            if (rootNode.has("errors")) {
                errorsNode = (ArrayNode) rootNode.get("errors");
            } else {
                errorsNode = rootNode.putArray("errors");
            }
            errorsNode.add(error);

            // Update the status to PARTIAL_FAILURE if it's not already
            rootNode.put("status", RecordingStatus.Status.PARTIAL_FAILURE.toString().toLowerCase());

            // Write back to file
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), rootNode);

            return true;
        } catch (IOException e) {
            logger.error("Failed to add error to manifest file in {}: {}", outputDir, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks if a manifest file exists in the specified directory and has the expected status.
     *
     * @param outputDir      the directory where the recording chunks are stored
     * @param expectedStatus the status to check for
     * @return true if the manifest exists and has the expected status, false otherwise
     */
    public static boolean checkIfManifestExistAndHasStatus(String outputDir, RecordingStatus.Status expectedStatus) {
        try {
            Path manifestPath = Paths.get(outputDir, MANIFEST_FILENAME);

            // Check if the manifest exists
            if (!Files.exists(manifestPath)) {
                logger.debug("Manifest file does not exist in {}", outputDir);
                return false;
            }

            // Read the manifest file
            ObjectNode rootNode = (ObjectNode) objectMapper.readTree(manifestPath.toFile());

            // Check if the status matches the expected status
            if (rootNode.has("status")) {
                String statusStr = rootNode.get("status").asText().toUpperCase();
                try {
                    RecordingStatus.Status actualStatus = RecordingStatus.Status.valueOf(statusStr);
                    return actualStatus == expectedStatus;
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid status in manifest: {}", statusStr);
                    return false;
                }
            } else {
                logger.debug("Manifest file in {} does not have a status field", outputDir);
                return false;
            }
        } catch (IOException e) {
            logger.error("Failed to read manifest file in {}: {}", outputDir, e.getMessage(), e);
            return false;
        }
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






    //
    //
    // IMPLEMENTATION
    //
    //

    /**
     *
     * @param outputDir
     * @param capturedOutput
     * @param finalStatus
     */
    private static void gatherChunkFileAndUpdateStateAndManifest(String outputDir, List<String> capturedOutput, RecordingStatus.Status finalStatus) {
        List<File> chunkFiles = gatherChunkFile(outputDir);

        // If we have chunks, mark as completed, otherwise as partial failure
        if (finalStatus == RecordingStatus.Status.COMPLETED) {
            finalStatus = chunkFiles.isEmpty() ?
                RecordingStatus.Status.PARTIAL_FAILURE : RecordingStatus.Status.COMPLETED;
        }

        if (finalStatus == RecordingStatus.Status.PARTIAL_FAILURE) {
            // Create a list for errors if we don't have one yet
            if (capturedOutput == null) {
                capturedOutput = new ArrayList<>();
            }

            // Add the error message
            capturedOutput.add("No active recording process found but recording was requested to stop");
        }

        // Update the manifest with PARTIAL_FAILURE status and errors
        RecordingManifestUtils.createOrUpdateManifest(
            outputDir,
            finalStatus,
            capturedOutput,
            chunkFiles
        );
    }

}
