package com.prtlabs.rlalc.backend.mediacapture.services;

import com.prtlabs.exceptions.PrtTechnicalException;
import com.prtlabs.exceptions.PrtTechnicalRuntimeException;
import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.IMediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.dto.MediaCapturePlanningDTO;
import com.prtlabs.rlalc.backend.mediacapture.services.jobs.MediaCaptureJob;
import com.prtlabs.rlalc.backend.mediacapture.services.jobs.MediaCaptureStopJob;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.dto.StreamToCaptureDTO;
import com.prtlabs.rlalc.backend.mediacapture.services.recorders.IMediaRecorder;
import com.prtlabs.rlalc.domain.RecordingId;
import com.prtlabs.rlalc.exceptions.RLALCExceptionCodesEnum;
import jakarta.inject.Inject;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Implementation of the RLALCMediaCaptureService.
 */
@Singleton
public class RLALCMediaCaptureServiceImpl implements IRLALCMediaCaptureService {

    private static final Logger logger = LoggerFactory.getLogger(RLALCMediaCaptureServiceImpl.class);


    @Inject private IMediaRecorder mediaRecorder;
    @Inject private IMediaCapturePlanningLoader mediaCapturePlanningService;


    @Override
    public void startMediaCapture() throws PrtTechnicalException {
        MediaCapturePlanningDTO planning = readMediaCapturePlanning();
        logger.info("  -> MediaCapturePlanning read successfully. Found nb=[{}] streams to capture. Scheduling media capture tasks ...", planning.getStreamsToCapture().size());
        scheduleMediaCapture(planning);
        logger.info(" -> Scheduling done.");
    }

    @Override
    public Map<RecordingId, RecordingStatus> getMediaCaptureStatus() {
        return Map.of();
    }




    //
    //
    // IMPLEMENTATION
    //
    //

    private MediaCapturePlanningDTO readMediaCapturePlanning() throws PrtTechnicalException {
        try {
            return mediaCapturePlanningService.loadMediaCapturePlanning();
        } catch (PrtTechnicalRuntimeException ex) {
            throw new PrtTechnicalException(RLALCExceptionCodesEnum.RLAC_000_FailedToReadConfiguration.name(), "Failed to read media capture planning with message=["+ex.getMessage()+"]", ex);
        }
    }

    private void scheduleMediaCapture(MediaCapturePlanningDTO planning) {
        try {
            // Initialize the scheduler
            logger.info("Quartz scheduler initialization ...");
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Scheduler scheduler = schedulerFactory.getScheduler();
            scheduler.start();

            // Schedule a job for each stream to capture
            for (StreamToCaptureDTO stream : planning.getStreamsToCapture()) {
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

                // Parse start time and duration
                long startTimeEpochSec = stream.getStartTimeUTCEpochSec();
                long durationSeconds = stream.getDurationSeconds();
                Date startDate = Date.from(Instant.ofEpochSecond(startTimeEpochSec));
                Date endDate = Date.from(Instant.ofEpochSecond(startTimeEpochSec + durationSeconds));

                // Check if the job has already ended
                Date now = new Date();
                if (endDate.before(now)) {
                    logger.info("    -> NOTE: Skipping media capture for stream [{}] as it has already ended with endTime=[{}]", stream.getTitle(), endDate);
                    continue;
                }

                // Check if the job has already started but not ended
                if (startDate.before(now) && endDate.after(now)) {
                    // Adjust start date to now
                    startDate = now;
                    // Adjusting duration
                    durationSeconds = (endDate.getTime()-startDate.getTime())/1000;
                    logger.info("Media capture for stream [{}] has already started. Adjusting start time to current time [{}] with durationSeconds=[{}]",
                            stream.getTitle(), startDate, durationSeconds);
                }

                // Create trigger for start job
                Trigger startTrigger = TriggerBuilder.newTrigger()
                        .withIdentity(triggerId)
                        .startAt(startDate)
                        .build();

                // Schedule the start job
                scheduler.scheduleJob(jobDetail, startTrigger);

                // Create job detail for stop job
                String stopJobId = "stop-" + stream.getUuid();
                String stopTriggerId = "stop-trigger-" + stream.getUuid();

                // Create job data map for stop job
                JobDataMap stopJobDataMap = new JobDataMap();
                stopJobDataMap.put(MediaCaptureStopJob.KEY_PROGRAM_UUID, stream.getUuid());
                stopJobDataMap.put(MediaCaptureStopJob.KEY_PROGRAM_NAME, stream.getTitle());
                stopJobDataMap.put(MediaCaptureStopJob.KEY_MEDIA_RECORDER, mediaRecorder);

                // Create job detail for stop job
                JobDetail stopJobDetail = JobBuilder.newJob(MediaCaptureStopJob.class)
                        .withIdentity(stopJobId)
                        .usingJobData(stopJobDataMap)
                        .build();

                // Create trigger for stop job
                Trigger stopTrigger = TriggerBuilder.newTrigger()
                        .withIdentity(stopTriggerId)
                        .startAt(endDate)
                        .build();

                // Schedule the stop job
                scheduler.scheduleJob(stopJobDetail, stopTrigger);

                logger.info("    -> Media capture scheduled for stream [{}] at [{}] for a duration of [{}]secs", stream.getTitle(), startDate, durationSeconds);
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
