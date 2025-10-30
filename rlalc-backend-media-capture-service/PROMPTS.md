
# Prompt history

When nothing is specified, the coding Agent is JetBrains Junie




## -------------------------------------------------------------------------------------------------------------------

### ----------------------------
I want you to initiate this Java project which should be built with Gradle 7.6.3 with:

- an EntryPoint class called `com.prtlabs.rlalc.backend.services.mediacapture.EntryPoint.java` which calls a service instanciated via Google Guice, called RLALCMediaCaptureService.

- The RLALCMediaCaptureService should be in package `com.prtlabs.rlalc.backend.services.mediacapture` and should have a single method called start which display a simple "Hello world" message.

The application will use Google Guice and  SLF4F with LogBack for logging.


### ----------------------------
Update the project to Java 21 and Guice 7.0.0 please






## -------------------------------------------------------------------------------------------------------------------

### ----------------------------
Create a Dockerfile for the rlalc-backend-media-capture-service project which should let one run the component using Docker run.

The image should:
- use a base openjdk 23 image
- copy the ./build/libs/rlalc-backend-media-capture-service-1.0-SNAPSHOT.jar file into `/opt/prtlabs/radioLiveALaCarte/rlalc-backend-media-capture-service.jar`
- run it with `java -jar ` as the entrypoint
- logs for the FILE appender should go to `/opt/prtlabs/radioLiveALaCarte/logs/` path which will be mapped to the `grvfm-media-capture-logs`volume in the docker compose file
- logs for the CONSOLE appender should go to stdout so that we can track them with `docker logs`
- The working dir for the application should be `/opt/prtlabs/radioLiveALaCarte/` which will be mapped to the 'grvfm-media-capture-mediastorage' volume in the docker compose file


### ----------------------------
Add two Gradle tasks :
- a 'prtBuildDockerImage' task that depends on the 'jar' task and build the docker image
- a 'prtRunViaDocker' task


### ----------------------------
In both tasks, can you display on the screen, on top of the existing "Building Docker image" / "Running Docker container", the equivalent CLI command please?






## -------------------------------------------------------------------------------------------------------------------

### ----------------------------
Finalize the MediaCapturePlanning class so that it can be used to read the JSON content of the `grvfm-backend-media-capture-service.conf` file.

I need to be able to read the content of the file using Jackson and the MediaCapturePlanning class.

### ----------------------------
Adjust the Java class now that I've added the 'startTimeUTCEpochSec' and 'durationSeconds' attributes to the JSON 





## -------------------------------------------------------------------------------------------------------------------

### ----------------------------
Can you make sure the `RLALCMediaCaptureServiceImpl` class does enforce the duration of the Recording jobs please?

Right now, it doesn't stop jobs.

Here are some information about how to use Quartz for this job :
```
Quartz itself doesn’t manage durations — only triggers (start times and schedules).

👉 To enforce a duration, you must handle it in your job logic, for example:

Schedule a start job at the start time.

Schedule a stop job (or cancellation) at start + duration.

And obvisouly don't even start the job if startTime+duration is already in the past
```





## -------------------------------------------------------------------------------------------------------------------

### ----------------------------
Update the FFMpegRecorder class to use ffmpeg to actually record the streams. Take inspiration from this code.

```
            String recordingBaseName = ... (based on ProgramDescriptor uuid dash name, cleaned to be fit for a filename, all lowercase)
            String dataPrefix = ... (YYYY/MM/DD
            outputPath = "/opt/prtlabs/rlalc/datastore/media/mp3/" + recordingBaseName + "/" + recordingBaseName + "_chunk_%Y%m%d_%H%M%S.mp3";
            processBuilder = new ProcessBuilder(
                    "ffmpeg",
                    "-i", url,
                    "-c:a", "libmp3lame",
                    "-b:a", "160k",
                    "-f", "mp3",
                    "-f", "segment",
                    "-segment_time", "10",
                    "-ar", "32000",
                    outputPath
```

In the stopRecording, we need to cleanly kill ffmpeg to let it properly dispose resources (files, network connections, ...)

### ----------------------------
We need a way to track the status of recordings (pending, ongoing, completed, completedWithErrors). 
We would like to implement this using:
 - a JSON based `recording-manifest.json` file located where the chunks are located
 - the manifest would contain: 
   - a 'status' attribute: the status of the job (pending, ongoing, completed, partial_failure)
   - an 'errors' optional attribute containing an array of "information about the error(s)"
   - a 'chunkList' attribute with the list of chunks generated, defined by they path
 - as there is no concurrency (single thread) for each recording, no need to deal with this problem. And since 
the `getChunkFilesForRecording` method will make read-only access to the file, there is no need for concurrency handling
with the recording logic either. 

Thanks to this, the `getChunkFilesForRecording(...)` will make it possible to track the progress of the recording. 
The chunkList attribute will only contain data when at least one chunk has been saved.

Make the `getChunkFilesForRecording(...)` method return a RecordingStatus class that contains the information described in the manifest above.





## -------------------------------------------------------------------------------------------------------------------

### ----------------------------
Refactor the 'MediaCapturePlanning' class to conform with the 'rlalc-media-capture-batch.conf' new format which no longer contain the data element (all elements which were in the data element are now at the top level)





## -------------------------------------------------------------------------------------------------------------------

### ----------------------------
Use Lombok for all DTO classes in order to minize boilerplate code





## -------------------------------------------------------------------------------------------------------------------

### ----------------------------
When the ffmpeg command is executed by the FFMpegRecorder class, it doesn't look for errors reported by the process.

The command was missing the "-strftime" (which I've added) and that was leading to an instant "Invalid segment filename template ..." at the ffmpeg level but this is completely ignored by the code.

Can you add the error handling AND the full stdout/stderr error capture so that the problem can be logged and understood.

The RecordingStatus status attribute must be set to PARTIAL_FAILURE and the `List<String> errors` attribute must contain the full stdout/stderr output of the command.





## -------------------------------------------------------------------------------------------------------------------

### ----------------------------
Can you implement the following methods inside the `RLALCMediaCaptureServiceImpl` class?

    List<String> getScheduledProgramIds();

### ----------------------------
Can you implement the following methods inside the `RLALCMediaCaptureServiceImpl` class?

    Map<RecordingId, RecordingStatus> getRecordingStatuses();

### ----------------------------
Can you implement the following methods inside the `RLALCMediaCaptureServiceImpl` class?

    Map<RecordingId, RecordingStatus> getRecordingChunks(String programId, Instant day);
