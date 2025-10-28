package com.prtlabs.rlalc.backend.mediacapture.services.jobs;

import com.prtlabs.rlalc.backend.mediacapture.services.recorders.IMediaRecorder;
import com.prtlabs.rlalc.domain.RecordingId;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quartz job that stops a media capture task.
 */
public class MediaCaptureStopJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(MediaCaptureStopJob.class);

    // Job data map keys
    public static final String KEY_PROGRAM_UUID = "programUuid";
    public static final String KEY_PROGRAM_NAME = "programName";
    public static final String KEY_MEDIA_RECORDER = "mediaRecorder";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        String programUuid = dataMap.getString(KEY_PROGRAM_UUID);
        String programName = dataMap.getString(KEY_PROGRAM_NAME);
        IMediaRecorder mediaRecorder = (IMediaRecorder) dataMap.get(KEY_MEDIA_RECORDER);

        // Get the recording ID from the static map in MediaCaptureJob
        RecordingId recordingId = MediaCaptureJob.getRecordingId(programUuid);

        if (recordingId == null) {
            logger.warn("No recording ID found for program [{}] with UUID [{}]. The recording may have already been stopped or never started.",
                    programName, programUuid);
            return;
        }

        logger.info("Stopping media capture for program [{}] with UUID [{}] and recording ID [{}]",
                programName, programUuid, recordingId);

        try {
            // Stop the recording
            mediaRecorder.stopRecording(recordingId);
            logger.info("Media capture stopped successfully for program [{}]", programName);
        } catch (Exception e) {
            logger.error("Failed to stop media capture for program [{}] with UUID [{}]: {}",
                    programName, programUuid, e.getMessage(), e);
            throw new JobExecutionException("Failed to stop media capture", e);
        }
    }
}
