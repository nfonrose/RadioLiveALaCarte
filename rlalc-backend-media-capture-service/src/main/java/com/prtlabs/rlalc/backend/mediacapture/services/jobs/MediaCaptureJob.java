package com.prtlabs.rlalc.backend.mediacapture.services.jobs;

import com.prtlabs.rlalc.backend.mediacapture.services.recorders.IMediaRecorder;
import com.prtlabs.rlalc.domain.ProgramDescriptor;
import com.prtlabs.rlalc.domain.RecordingId;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Quartz job that executes a media capture task.
 */
public class MediaCaptureJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(MediaCaptureJob.class);

    // Job data map keys
    public static final String KEY_PROGRAM_UUID = "programUuid";
    public static final String KEY_PROGRAM_NAME = "programName";
    public static final String KEY_STREAM_URL = "streamUrl";
    public static final String KEY_DURATION_SECONDS = "durationSeconds";
    public static final String KEY_MEDIA_RECORDER = "mediaRecorder";

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
            ProgramDescriptor programDescriptor = new ProgramDescriptor();
            programDescriptor.uuid = programUuid;
            programDescriptor.name = programName;
            programDescriptor.streamURL = streamUrl;
            programDescriptor.chunkFileNamePrefix = Optional.of(programUuid);

            // Create recorder specific parameters
            Map<String, String> recorderParams = new HashMap<>();
            recorderParams.put("durationSeconds", durationSeconds);

            // Start recording
            RecordingId recordingId = mediaRecorder.record(programDescriptor, recorderParams);

            logger.info("Media capture started successfully for program [{}] with recording ID",
                    programName);
        } catch (Exception e) {
            logger.error("Failed to start media capture for program [{}] with UUID [{}]: {}",
                    programName, programUuid, e.getMessage(), e);
            throw new JobExecutionException("Failed to start media capture", e);
        }
    }
}
