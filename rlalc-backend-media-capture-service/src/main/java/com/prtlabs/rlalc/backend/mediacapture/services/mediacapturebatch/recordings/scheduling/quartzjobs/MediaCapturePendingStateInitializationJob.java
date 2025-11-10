package com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.scheduling.quartzjobs;

import com.fasterxml.jackson.databind.ObjectMapper;
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


public class MediaCapturePendingStateInitializationJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(MediaCapturePendingStateInitializationJob.class);

    private static final ObjectMapper mapper = PrtJsonUtils.getFasterXmlObjectMapper();

    @Inject private IMediaRecorder mediaRecorder;


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JobDataMap dataMap = context.getMergedJobDataMap();
            String programJson = dataMap.getString(MediaCaptureJob.KEY_PROGRAM_DESC_ASJSON);
            ProgramDescriptorDTO program = new ObjectMapper().readValue(programJson, ProgramDescriptorDTO.class);

            logger.info("Marking program [{}] (UUID={}) as PENDING at 00:00 in timeZone [{}]", program.getTitle(), program.getUuid(), program.getTimeZone());

            // Initialize the recording into a Pending state
            mediaRecorder.initBeforeRecording(program);

        } catch (Exception e) {
            throw new JobExecutionException("Failed to initialize pending state", e);
        }
    }
}