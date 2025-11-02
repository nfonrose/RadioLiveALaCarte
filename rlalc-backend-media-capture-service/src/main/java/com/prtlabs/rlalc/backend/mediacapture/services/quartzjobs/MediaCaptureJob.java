package com.prtlabs.rlalc.backend.mediacapture.services.quartzjobs;

import com.prtlabs.rlalc.backend.mediacapture.services.recordings.recorders.IMediaRecorder;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import com.prtlabs.utils.json.PrtJsonUtils;
import jakarta.inject.Inject;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;


/**
 * Quartz job that executes a media capture task.
 */
public class MediaCaptureJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(MediaCaptureJob.class);

    public  static final String KEY_PROGRAM_DESC_ASJSON = "programDescAsJson";
    public  static final String KEY_DURATION_SECONDS    = "durationSeconds";
    public  static final String KEY_DEBUG_PROGRAM_TITLE = "debug_programTitle";

    @Inject private IMediaRecorder mediaRecorder;


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String debugProgramTitle = dataMap.getString(KEY_DEBUG_PROGRAM_TITLE);
        try {
            // Get the program descriptor from the job details
            // REMARK: It's a bad practice to pass the ProgramDescriptorDTO directly in the JobDataMap because of potential
            //         serialization issues when using a persistent Job scheduler. This is why we go through a JSON representation
            //         and the optional adjusted duration
            ProgramDescriptorDTO programDescriptor = PrtJsonUtils.getFasterXmlObjectMapper().readValue(dataMap.getString(KEY_PROGRAM_DESC_ASJSON), ProgramDescriptorDTO.class);
            // Update its duration if an "adjusted duration" is present (which is the case when the program has already started and the recording needs to be shorter)
            if (dataMap.containsKey(KEY_DURATION_SECONDS)) {
                long adjustedDurationSeconds = dataMap.getLong(KEY_DURATION_SECONDS);
                programDescriptor = programDescriptor.toBuilder()    // Adjust the duration in the ProgramDescriptor
                    .durationSeconds(adjustedDurationSeconds)
                    .build();
            }

            // Start recording
            mediaRecorder.startRecording(programDescriptor, new HashMap<>());

            // Store the recording ID in the static map
            logger.info("Media capture started successfully for program [{}]", programDescriptor.getTitle());
        } catch (Exception e) {
            logger.error("Failed to start media capture for program [{}] with message=[{}]", debugProgramTitle, e.getMessage(), e);
            throw new JobExecutionException("Failed to start media capture for ["+debugProgramTitle+"]", e);
        }
    }

}
