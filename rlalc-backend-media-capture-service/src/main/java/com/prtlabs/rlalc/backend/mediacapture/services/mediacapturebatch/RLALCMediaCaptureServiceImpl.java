package com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.scheduling.quartzjobs.MediaCapturePendingStateInitializationJob;
import com.prtlabs.rlalc.domain.ProgramId;
import com.prtlabs.utils.dependencyinjection.hk2.quartz.PrtHK2QuartzJobFactory;
import com.prtlabs.utils.exceptions.PrtTechnicalException;
import com.prtlabs.utils.exceptions.PrtTechnicalRuntimeException;
import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.planning.IMediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.planning.dto.MediaCapturePlanningDTO;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.scheduling.quartzjobs.MediaCaptureJob;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.scheduling.quartzjobs.MediaCaptureStopJob;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.recorders.IMediaRecorder;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import com.prtlabs.rlalc.exceptions.RLALCExceptionCodesEnum;
import com.prtlabs.utils.json.PrtJsonUtils;
import jakarta.inject.Inject;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;

import java.io.File;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.quartz.impl.matchers.GroupMatcher;


/**
 * Implementation of the RLALCMediaCaptureService.
 */
@Singleton
public class RLALCMediaCaptureServiceImpl implements IRLALCMediaCaptureService {

    private static final Logger logger = LoggerFactory.getLogger(RLALCMediaCaptureServiceImpl.class);

    private static final ObjectMapper mapper = PrtJsonUtils.getFasterXmlObjectMapper();

    @Inject private IMediaRecorder mediaRecorder;
    @Inject private IMediaCapturePlanningLoader mediaCapturePlanningService;
    @Inject private PrtHK2QuartzJobFactory prtHK2QuartzJobFactory;


    @Override
    public void startMediaCapture() throws PrtTechnicalException {
        MediaCapturePlanningDTO planning = readMediaCapturePlanning();
        logger.info("  -> MediaCapturePlanning read successfully. Found nb=[{}] streams to capture. Scheduling media capture tasks ...", planning.getProgramsToCapture().size());
        scheduleMediaCapture(planning);
        logger.info(" -> Scheduling done.");
    }

    @Override
    public List<ProgramId> getScheduledProgramIds() {
        try {
            // Get the scheduler
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Scheduler scheduler = schedulerFactory.getScheduler();

            // Get all job keys in the default group
            List<ProgramId> programIds = new ArrayList<>();
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.anyGroup())) {
                try {
                    JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                    // Only consider MediaCaptureJob jobs (not stop jobs)
                    if (jobDetail.getJobClass().equals(MediaCaptureJob.class)) {
                        JobDataMap jobDataMap = jobDetail.getJobDataMap();
                        ProgramDescriptorDTO programDescriptor = PrtJsonUtils.getFasterXmlObjectMapper().readValue(jobDataMap.getString(MediaCaptureJob.KEY_PROGRAM_DESC_ASJSON), ProgramDescriptorDTO.class);
                        if (programDescriptor.getUuid() != null) {
                            programIds.add(programDescriptor.getUuid());
                        }
                    }
                } catch (Exception e) {
                    logger.warn("  -> Failed to extract ProgramDescriptor from MediaCaptureJob with jobKey=["+jobKey+"]. Skipping it.");
                }
            }
            return programIds;
        } catch (SchedulerException e) {
            logger.error("Failed to get scheduled program IDs with message=[{}]", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public Map<ProgramId, RecordingStatus> getRecordingStatusesForCurrentDay() {
        return mediaRecorder.getRecordingStatuses();
    }

    @Override
    public List<URI> getRecordingChunks(ProgramId programId, Instant day) {
        // Get the files list from the mediaRecorder
        List<File> chunkFiles = mediaRecorder.getChunkFiles(programId, day);
        // Turn them into URIs
        List<URI> result = chunkFiles.stream()
            .map(f -> URI.create(f.getPath()))
            .collect(Collectors.toList());
        return result;
    }

    @Override
    public ProgramId addOneTimeMediaCapture(ProgramDescriptorDTO programDescriptor) {
        try {
            logger.info("Adding one-time media capture for program=[{}] with UUID [{}] at [{}] for [{}] seconds", 
                programDescriptor.getTitle(), programDescriptor.getUuid(), programDescriptor.getStartTimeUTCEpochSec(), 
                programDescriptor.getDurationSeconds());

            // Initialize the scheduler
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Scheduler scheduler = schedulerFactory.getScheduler();
            scheduler.setJobFactory(prtHK2QuartzJobFactory);

            // Parse start time and duration
            long startTimeEpochSec = programDescriptor.getStartTimeUTCEpochSec();
            long durationSeconds = programDescriptor.getDurationSeconds();
            Date startDate = Date.from(Instant.ofEpochSecond(startTimeEpochSec));
            Date endDate = Date.from(Instant.ofEpochSecond(startTimeEpochSec + durationSeconds));

            // Create the Quartz Start Job and its associate trigger
            String startRecordingJobId = "capture-" + programDescriptor.getUuid();
            String startRecordingTriggerId = "trigger-" + programDescriptor.getUuid();
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(MediaCaptureJob.KEY_PROGRAM_DESC_ASJSON, mapper.writeValueAsString(programDescriptor));
            JobDetail jobDetail = JobBuilder.newJob(MediaCaptureJob.class)
                .withIdentity(startRecordingJobId)
                .usingJobData(jobDataMap)
                .build();

            // Create trigger for start job
            Trigger startTrigger = TriggerBuilder.newTrigger()
                .withIdentity(startRecordingTriggerId)
                .startAt(startDate)
                .build();

            // Schedule the start job
            scheduler.scheduleJob(jobDetail, startTrigger);

            // Create the Quartz Stop Job and its associate trigger
            String stopRecordingJobId = "stop-" + programDescriptor.getUuid();
            String stopRecordingTriggerId = "stop-trigger-" + programDescriptor.getUuid();
            JobDataMap stopJobDataMap = new JobDataMap();
            stopJobDataMap.put(MediaCaptureJob.KEY_PROGRAM_DESC_ASJSON, mapper.writeValueAsString(programDescriptor));
            JobDetail stopJobDetail = JobBuilder.newJob(MediaCaptureStopJob.class)
                .withIdentity(stopRecordingJobId)
                .usingJobData(stopJobDataMap)
                .build();

            // Create trigger for stop job
            Trigger stopTrigger = TriggerBuilder.newTrigger()
                .withIdentity(stopRecordingTriggerId)
                .startAt(endDate)
                .build();

            // Schedule the stop job
            scheduler.scheduleJob(stopJobDetail, stopTrigger);

            // Initialize the recording into a Pending state
            mediaRecorder.initBeforeRecording(programDescriptor);

            logger.info("    -> One-time media capture scheduled for stream [{}] at [{}] for a duration of [{}]secs", 
                programDescriptor.getTitle(), startDate, durationSeconds);

            return programDescriptor.getUuid();
        } catch (Exception e) {
            logger.error("Failed to schedule one-time media capture: {}", e.getMessage(), e);
            throw new PrtTechnicalRuntimeException(RLALCExceptionCodesEnum.RLAC_001_FailedToScheduleMediaCapture.name(), 
                    "Failed to schedule one-time media capture: " + e.getMessage(), e);
        }
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
            scheduler.setJobFactory(prtHK2QuartzJobFactory);      // When Quartz Jobs are created (based on JobDetails), they are instantiated via our HK2 aware factory
            scheduler.start();

            // Schedule a job for each Program to capture
            //  - Create a Quartz startJob and a Quartz startTrigger to start the recording
            //  - Create a Quartz stopJob and a Quartz stopTrigger to stop the recording
            for (ProgramDescriptorDTO program : planning.getProgramsToCapture()) {
                try {
                    logger.info(" - Scheduling media capture for program=[{}] with UUID [{}] at [{}] for [{}] seconds", program.getTitle(), program.getUuid(), program.getStartTimeUTCEpochSec(), program.getDurationSeconds());

                    // Parse start time and duration
                    long startTimeEpochSec = program.getStartTimeUTCEpochSec();
                    long durationSeconds = program.getDurationSeconds();
                    Date startDate = Date.from(Instant.ofEpochSecond(startTimeEpochSec));
                    Date endDate = Date.from(Instant.ofEpochSecond(startTimeEpochSec + durationSeconds));

                    // Check if the job has already ended
                    Date now = new Date();
                    if (endDate.before(now)) {
                        logger.info("    -> NOTE: Skipping media capture for program=[{}] as it has already ended with endTime=[{}]", program.getTitle(), endDate);
                        continue;
                    }

                    // Create the Quartz "pending initialization" Job and its associate trigger
                    // - Compute the time at which the pending state should be set
                    ZoneId timeZone = program.getTimeZone();
                    ZonedDateTime programMidnight = startDate.toInstant().atZone(timeZone)
                        .toLocalDate() // midnight on the same local day as the program start
                        .atStartOfDay(timeZone);
                    // - Ensure we don’t schedule in the past
                    Date pendingJobDate = Date.from(programMidnight.toInstant());
                    if (pendingJobDate.before(now)) {
                        // Move to next day’s midnight if already passed
                        programMidnight = programMidnight.plusDays(1);
                        pendingJobDate = Date.from(programMidnight.toInstant());
                    }
                    // - Create the Job
                    String pendingJobId = "pending-" + program.getUuid();
                    String pendingTriggerId = "pending-trigger-" + program.getUuid();
                    JobDataMap pendingJobData = new JobDataMap();
                    pendingJobData.put(MediaCaptureJob.KEY_PROGRAM_DESC_ASJSON, mapper.writeValueAsString(program));
                    JobDetail pendingJobDetail = JobBuilder.newJob(MediaCapturePendingStateInitializationJob.class)
                        .withIdentity(pendingJobId)
                        .usingJobData(pendingJobData)
                        .build();
                    // - Trigger at 00:00am local program time
                    Trigger pendingTrigger = TriggerBuilder.newTrigger()
                        .withIdentity(pendingTriggerId)
                        .startAt(pendingJobDate)
                        .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(0, 0)
                            .inTimeZone(TimeZone.getTimeZone(timeZone)))
                        .build();
                    // - Schedule the pending initialization job
                    scheduler.scheduleJob(pendingJobDetail, pendingTrigger);
                    logger.info("    -> Pending job scheduled at [{}] in timeZone [{}]", pendingJobDate, timeZone);


                    // Create the Quartz Start Job and its associate trigger
                    // - Create the Job
                    String startRecordingJobId = "capture-" + program.getUuid();
                    String startRecordingTriggerId = "trigger-" + program.getUuid();
                    JobDataMap jobDataMap = new JobDataMap();
                    jobDataMap.put(MediaCaptureJob.KEY_PROGRAM_DESC_ASJSON, mapper.writeValueAsString(program));
                    //    Deal with the case where the job should have already started but not ended (and compute the adjusted duration if so)
                    if (startDate.before(now) && endDate.after(now)) {
                        startDate = now;
                        durationSeconds = (endDate.getTime() - startDate.getTime()) / 1000;
                        jobDataMap.put(MediaCaptureJob.KEY_DURATION_SECONDS, durationSeconds);    // Store the information for the Quartz job
                        logger.info("Media capture for program=[{}] has already started. Adjusting start time to current time [{}] with durationSeconds=[{}]", program.getTitle(), startDate, durationSeconds);
                    }
                    JobDetail jobDetail = JobBuilder.newJob(MediaCaptureJob.class)
                        .withIdentity(startRecordingJobId)
                        .usingJobData(jobDataMap)
                        .build();
                    // - Create trigger for start job
                    Trigger startTrigger = TriggerBuilder.newTrigger()
                        .withIdentity(startRecordingTriggerId)
                        .startAt(startDate)
                        .build();
                    // - Schedule the start job
                    scheduler.scheduleJob(jobDetail, startTrigger);

                    // Create the Quartz Stop Job and its associate trigger
                    // - Create the Job
                    String stopRecordingJobId = "stop-" + program.getUuid();
                    String stopRecordingTriggerId = "stop-trigger-" + program.getUuid();
                    JobDataMap stopJobDataMap = new JobDataMap();
                    stopJobDataMap.put(MediaCaptureJob.KEY_PROGRAM_DESC_ASJSON, mapper.writeValueAsString(program));
                    JobDetail stopJobDetail = JobBuilder.newJob(MediaCaptureStopJob.class)
                        .withIdentity(stopRecordingJobId)
                        .usingJobData(stopJobDataMap)
                        .build();
                    // - Create trigger for stop job
                    Trigger stopTrigger = TriggerBuilder.newTrigger()
                        .withIdentity(stopRecordingTriggerId)
                        .startAt(endDate)
                        .build();
                    // - Schedule the stop job
                    scheduler.scheduleJob(stopJobDetail, stopTrigger);

                    // Initialize the recording into a Pending state for the first day (the "pending state initializations" for the days after today are performed by the dedicated Quartz Job)
                    mediaRecorder.initBeforeRecording(program);

                    logger.info("    -> Media capture scheduled for stream [{}] at [{}] for a duration of [{}]secs", program.getTitle(), startDate, durationSeconds);
                } catch (JsonProcessingException e) {
                    logger.warn("    -> Failed to schedule recording for stream [{}] with message=[{}]", program.getTitle(), e.getMessage());
                }
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
