package com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.scheduling.quartzjobs;

import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.recorders.IMediaRecorder;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import com.prtlabs.utils.json.PrtJsonUtils;
import jakarta.inject.Inject;
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

    @Inject
    private IMediaRecorder mediaRecorder;


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String debugProgramTitle = dataMap.getString(MediaCaptureJob.KEY_DEBUG_PROGRAM_TITLE);
        try {
            // Get the program descriptor from the job details
            ProgramDescriptorDTO programDescriptor = PrtJsonUtils.getFasterXmlObjectMapper().readValue(dataMap.getString(MediaCaptureJob.KEY_PROGRAM_DESC_ASJSON), ProgramDescriptorDTO.class);
            // Stop the recording for this program
            logger.info("Stopping media capture for program [{}] with UUID [{}]", programDescriptor.getTitle(), programDescriptor.getUuid());
            // Stop the recording
            mediaRecorder.stopRecording(programDescriptor.getUuid());
            logger.info("  -> Media capture stopped successfully for program [{}]", programDescriptor.getTitle());
        } catch (Exception e) {
            logger.error("Failed to stop media capture for program [{}] with message=[{}]", debugProgramTitle, e.getMessage(), e);
            throw new JobExecutionException("Failed to stop media capture", e);
        }
    }

}
