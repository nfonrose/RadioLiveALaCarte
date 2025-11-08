package com.prtlabs.rlalc.backend.mediacapture.services.recordings.recorders.ffmpeg;

import com.prtlabs.rlalc.backend.mediacapture.services.recordings.statemanagement.IRecordingStateManagementService;
import com.prtlabs.rlalc.backend.mediacapture.utils.RLALCLocalTimeZoneTimeHelper;
import com.prtlabs.rlalc.domain.ProgramId;
import com.prtlabs.utils.exceptions.PrtBaseRuntimeException;
import com.prtlabs.utils.exceptions.PrtTechnicalRuntimeException;
import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.backend.mediacapture.services.recordings.recorders.IMediaRecorder;
import com.prtlabs.rlalc.backend.mediacapture.services.recordings.statemanagement.manifests.ManifestFileBasedRecordingStateManagementService;
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
    @Inject private IRecordingStateManagementService recordingStateManagementService;



    @Override
    public void initBeforeRecording(ProgramDescriptorDTO programDescriptor) {
        // Create a RecordingId in the 'rec-<ProgramId>-<epochSec>'
        RecordingId recordingId = buildRecordingId_UsingProgramId_andCurrentDayForProgram(programDescriptor);
        recordingIdPerProgramId.put(programDescriptor.getUuid(), recordingId);
        programIdPerRecordingId.put(recordingId, programDescriptor.getUuid());

        // Create the initial manifest file with PENDING status
        try {
            // Compute the filenames and path for the local storage of chunks
            FileInfoForRecordingStorage fileInfoForRecordingStorage = buildFileInfoForRecordingStorage(programDescriptor);
            Files.createDirectories(Paths.get(fileInfoForRecordingStorage.outputDir));
            // Initialize the recording path since it's needed to access the Manifest (to check the status of a Recording)
            recordingPaths.put(recordingId, fileInfoForRecordingStorage.outputDir);
            // Create the initial manifest file with PENDING status
            recordingStateManagementService.createOrUpdateManifest(fileInfoForRecordingStorage.outputDir, RecordingStatus.Status.PENDING, null, null);
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

            // Start the ffmpeg process
            //  - Build the output files pattern
            String audioChunksPathPattern = fileInfoForRecordingStorage.outputDir + "/" + fileInfoForRecordingStorage.recordingBaseName + "_chunk_%Y%m%d_%H%M%S.mp3";
            //  - Build the ffmpeg command
            String ffmpegCommand = String.join(" ", List.of(
                "ffmpeg",
                "-t", ""+programDescriptor.getDurationSeconds(),
                "-i", "\"" + programDescriptor.getStreamURL() + "\"",
                "-c:a", "libmp3lame",
                "-b:a", "160k",
                "-f", "mp3",
                "-f", "segment",
                "-segment_time", "10",
                "-strftime", "1",
                "-ar", "32000",
                "\"" + audioChunksPathPattern + "\""
            ));
            List<String> command = new ArrayList<>();
            command.add("bash");
            command.add("-c");
            command.add("exec " + ffmpegCommand);
            //  - Build the process and starts it
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            //  - Keep its pid associated with the
            activeProcesses.put(recordingId, process);

            // Update manifest to ONGOING status
            logger.info("Started ffmpeg with PID=[{}] and command=[{}]", process.pid(), String.join(" ", command));
            recordingStateManagementService.updateStatus(programDescriptor, Optional.of(process.pid()), Optional.empty(), fileInfoForRecordingStorage.outputDir);

            // Register a thread to collect the process output
            //  - Initialize a buffer
            List<String> outputLines = new ArrayList<>();
            processOutputs.put(recordingId, outputLines);
            //  - Start a thread to read the process output
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

            // Register a callback for when the process exits
            process.onExit().thenAccept((theProcess) -> {
                logger.info(" ============ FFmpeg process=[{}]  exited with code=[{}]. ==================================== ", theProcess.pid(), theProcess.exitValue());
                recordingStateManagementService.updateStatus(programDescriptor, Optional.of(process.pid()), Optional.of(theProcess.exitValue()), fileInfoForRecordingStorage.outputDir, outputLines);
            });

            logger.info("Recording started for program [{}] with recording ID [{}]", programDescriptor.getTitle(), recordingId);
        } catch (IOException e) {
            String message = String.format("Failed to start recording for program=[{}] with message=[{}]", programDescriptor.getTitle(), e.getMessage());
            logger.error(message, e);
            throw new PrtTechnicalRuntimeException(RLALCExceptionCodesEnum.RLAC_006_FailedToStartRecordingForProgram.name(), message, e);
        }
    }


    @Override
    public void stopRecording(ProgramId programId) {
        if (programId == null) { logger.warn(" -> Cannot stop recording with null recording ID"); return; }

        logger.info("Stopping recording with programId=[{}]", programId);

        // Get the path for this recording
        Process process = activeProcesses.get(programId);
        if (process != null) {
            logger.warn("Active FFMPEG recording process found (which is not expected since it's supposed to auto exit after the defined duration");
            // Send SIGTERM to allow ffmpeg to clean up resources and wait for the process to terminate
            process.destroy();
            boolean terminated = false;
            try { process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException e) { throw new PrtTechnicalRuntimeException(RLALCExceptionCodesEnum.RLAC_008_FailedToStopRecordingProcessForUnkownReason.name(), e.getMessage(), e); }
            if (!terminated) {
                // If it didn't terminate gracefully, force it
                logger.warn(" -> FFMPEG process did not terminate gracefully, forcing termination");
                process.destroyForcibly();
            } else {
                logger.warn(" -> FFMPEG process did terminate gracefully (it was probably just a timing issue where the stop Trigger kicked-in a little bit early)");
                logger.warn("Active ffmpeg recording process found (which is expected since it's supposed to auto exit after the defined duration");
            }
        } else {
            logger.info("No active FFMPEG recording process found (which is expected since it's supposed to auto exit after the defined duration");
        }

        // Clean up
        processOutputs.remove(programId);

        // Collect the files which have been generated and update the status
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
                RecordingStatus status = recordingStateManagementService.readRecordingState(outputDir);
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
        Path dirPath = Paths.get(outputDir);
        if (!Files.exists(dirPath)) { throw new PrtBaseRuntimeException(RLALCExceptionCodesEnum.RLAC_004_NoRecordingsStorageFoundForProgram.name(), "Recording storage path=["+outputDir+"] not found"); }

        // Get the path for the day, with date format=YYYYMMDD
        String dayDirName = this.getDirNameForDay(day);
        Path dirForDayPath = dirPath.resolve(dayDirName);
        if (!Files.exists(dirForDayPath)) { return new ArrayList<>(); /* throw new PrtBaseRuntimeException(RLALCExceptionCodesEnum.RLAC_004_NoRecordingsStorageFoundForProgram.name(), "Recording storage path for that day=["+dirForDayPath.toFile().getAbsolutePath()+"] not found");*/ }

        // Get all MP3 files in the directory
        List<File> chunkFiles = new ArrayList<>();
        try {
            try (Stream<Path> files = Files.list(dirForDayPath)) {
                chunkFiles = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".mp3"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
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
