package com.prtlabs.rlalc.backend.mediacapture.services;

import com.prtlabs.exceptions.PrtTechnicalException;
import com.prtlabs.exceptions.PrtTechnicalRuntimeException;
import com.prtlabs.rlalc.backend.mediacapture.services.configuration.MediaCapturePlanning;
import com.prtlabs.rlalc.backend.mediacapture.services.jobs.MediaCaptureJob;
import com.prtlabs.rlalc.backend.mediacapture.services.recorders.IMediaRecorder;
import com.prtlabs.rlalc.backend.mediacapture.services.recorders.ffmpeg.FFMpegRecorder;
import com.prtlabs.rlalc.exceptions.RLALCExceptionCodesEnum;
import jakarta.inject.Inject;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;

import java.io.IOException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

/**
 * Implementation of the RLALCMediaCaptureService.
 */
@Singleton
public class RLALCMediaCaptureServiceImpl implements RLALCMediaCaptureService {

    private static final Logger logger = LoggerFactory.getLogger(RLALCMediaCaptureServiceImpl.class);

    @Inject
    private IMediaRecorder mediaRecorder;


    @Override
    public void start() throws PrtTechnicalException {
        logger.info("Starting RLALC Media Capture Service ...");
        MediaCapturePlanning planning = readMediaCapturePlanning();
        logger.info("  -> MediaCapturePlanning read successfully. Found nb=[{}] streams to capture. Scheduling media capture tasks ...", planning.getStreamsToCapture().size());
        scheduleMediaCapture(planning);
        logger.info(" -> Scheduling done.");
    }


    //
    //
    // IMPLEMENTATION
    //
    //

    private MediaCapturePlanning readMediaCapturePlanning() throws PrtTechnicalException {
        try {
            String configFilePath = "/opt/prtlabs/rlalc/conf/rlalc-media-capture-batch.conf";
            logger.info("Reading media capture planning from [{}]", configFilePath);
            return MediaCapturePlanning.fromFile(configFilePath, "src/main/resources/rlalc-media-capture-batch.conf");
        } catch (PrtTechnicalRuntimeException ex) {
            throw new PrtTechnicalException(RLALCExceptionCodesEnum.RLAC_000_FailedToReadConfiguration.name(), "Failed to read media capture planning with message=["+ex.getMessage()+"]", ex);
        }
    }

    private void scheduleMediaCapture(MediaCapturePlanning planning) {
        try {
            // Initialize the scheduler
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Scheduler scheduler = schedulerFactory.getScheduler();
            scheduler.start();

            logger.info("Quartz scheduler initialized and started");

            // Schedule a job for each stream to capture
            for (MediaCapturePlanning.StreamToCapture stream : planning.getStreamsToCapture()) {
                String jobId = "capture-" + stream.getUuid();
                String triggerId = "trigger-" + stream.getUuid();

                logger.info(" - Scheduling media capture for stream [{}] with UUID [{}] at [{}] for [{}] seconds",
                        stream.getTitle(), stream.getUuid(), stream.getStartTimeUTCEpochSec(), stream.getDurationSeconds());

                // Create job data map
                JobDataMap jobDataMap = new JobDataMap();
                jobDataMap.put(MediaCaptureJob.KEY_PROGRAM_UUID, stream.getUuid());
                jobDataMap.put(MediaCaptureJob.KEY_PROGRAM_NAME, stream.getTitle());
                jobDataMap.put(MediaCaptureJob.KEY_STREAM_URL, stream.getStreamurl());
                jobDataMap.put(MediaCaptureJob.KEY_DURATION_SECONDS, stream.getDurationSeconds());
                jobDataMap.put(MediaCaptureJob.KEY_MEDIA_RECORDER, mediaRecorder);

                // Create job detail
                JobDetail jobDetail = JobBuilder.newJob(MediaCaptureJob.class)
                        .withIdentity(jobId)
                        .usingJobData(jobDataMap)
                        .build();

                // Parse start time
                long startTimeEpochSec = Long.parseLong(stream.getStartTimeUTCEpochSec());
                Date startDate = Date.from(Instant.ofEpochSecond(startTimeEpochSec));

                // Create trigger
                Trigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity(triggerId)
                        .startAt(startDate)
                        .build();

                // Schedule the job
                scheduler.scheduleJob(jobDetail, trigger);

                logger.info("    -> Media capture scheduled for stream [{}] at [{}] for a duration of [{}]secs", stream.getTitle(), startDate, stream.getDurationSeconds());
            }

            logger.info("All media capture tasks scheduled successfully");

        } catch (SchedulerException e) {
            logger.error("Failed to schedule media capture tasks: {}", e.getMessage(), e);
            throw new PrtTechnicalRuntimeException(RLALCExceptionCodesEnum.RLAC_001_FailedToScheduleMediaCapture.name(), 
                    "Failed to schedule media capture tasks: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            logger.error("Failed to parse start time: {}", e.getMessage(), e);
            throw new PrtTechnicalRuntimeException(RLALCExceptionCodesEnum.RLAC_001_FailedToScheduleMediaCapture.name(), 
                    "Failed to parse start time: " + e.getMessage(), e);
        }
    }

}
