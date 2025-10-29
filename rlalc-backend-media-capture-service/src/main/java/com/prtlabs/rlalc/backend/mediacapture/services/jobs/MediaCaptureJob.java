package com.prtlabs.rlalc.backend.mediacapture.services.jobs;

import com.prtlabs.rlalc.backend.mediacapture.services.recorders.IMediaRecorder;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quartz job that executes a media capture task.
 */
public class MediaCaptureJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(MediaCaptureJob.class);

    // Static map to store recording IDs by program UUID
    private static final Map<String, String> recordingIds = new ConcurrentHashMap<>();

    // Job data map keys
    public static final String KEY_PROGRAM_UUID = "programUuid";
    public static final String KEY_PROGRAM_NAME = "programName";
    public static final String KEY_STREAM_URL = "streamUrl";
    public static final String KEY_DURATION_SECONDS = "durationSeconds";
    public static final String KEY_MEDIA_RECORDER = "mediaRecorder";

    /**
     * Get the recording ID for a program.
     * 
     * @param programUuid The UUID of the program
     * @return The recording ID, or null if not found
     */
    public static String getRecordingId(String programUuid) {
        return recordingIds.get(programUuid);
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        String programUuid = dataMap.getString(KEY_PROGRAM_UUID);
        String programName = dataMap.getString(KEY_PROGRAM_NAME);
        String streamUrl = dataMap.getString(KEY_STREAM_URL);
        String durationSeconds = dataMap.getString(KEY_DURATION_SECONDS);
        IMediaRecorder mediaRecorder = (IMediaRecorder) dataMap.get(KEY_MEDIA_RECORDER);

        logger.info("Starting media capture for program [{}] with UUID [{}] from stream [{}] for [{}] seconds",
                programName, programUuid, streamUrl, durationSeconds);

        try {
            // Create program descriptor
            ProgramDescriptorDTO programDescriptor = new ProgramDescriptorDTO(
                programUuid,
                streamUrl,
                programName,
                0L,
                0L
            );

            // Create recorder specific parameters
            Map<String, String> recorderParams = new HashMap<>();
            recorderParams.put("durationSeconds", durationSeconds);

            // Start recording
            String recordingId = mediaRecorder.record(programDescriptor, recorderParams);

            // Store the recording ID in the static map
            recordingIds.put(programUuid, recordingId);

            logger.info("Media capture started successfully for program [{}] with recording ID [{}]",
                    programName, recordingId);
        } catch (Exception e) {
            logger.error("Failed to start media capture for program [{}] with UUID [{}]: {}",
                    programName, programUuid, e.getMessage(), e);
            throw new JobExecutionException("Failed to start media capture", e);
        }
    }
}
