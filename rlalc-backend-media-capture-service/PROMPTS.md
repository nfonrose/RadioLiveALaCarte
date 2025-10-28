
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
