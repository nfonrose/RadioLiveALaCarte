package com.prtlabs.rlalc.backend.mediacapture.entrypoint;

import com.google.inject.AbstractModule;
import com.prtlabs.rlalc.backend.mediacapture.entrypoint.embeddedrestserver.IEmbeddedRESTServerModule;
import com.prtlabs.rlalc.backend.mediacapture.entrypoint.embeddedrestserver.embeddedtomcatwithjersey.TomcatJerseyEmbeddedRESTServerModule;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.IRLALCMediaCaptureService;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.RLALCMediaCaptureServiceImpl;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.planning.IMediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.planning.loaders.file.ConfigFileBased_MediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.recorders.IMediaRecorder;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.recorders.ffmpeg.FFMpegRecorder;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.statemanagement.IRecordingStateManagementService;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.statemanagement.manifests.ManifestFileBasedRecordingStateManagementService;
import com.prtlabs.utils.dependencyinjection.guice.quartz.PrtGuiceQuartzJobFactory;
import com.prtlabs.utils.time.provider.IPrtTimeProviderService;
import com.prtlabs.utils.time.provider.PrtComputerClockBasedTimeProviderService;
import org.quartz.spi.JobFactory;

/**
 * Guice module for the RadioLiveALaCarte Media Capture service.
 */
public class MediaCaptureServiceGuiceModule extends AbstractModule {
    
    @Override
    protected void configure() {
        // Technical bindings
        bind(JobFactory.class).to(PrtGuiceQuartzJobFactory.class);    // Needed for Quartz jobs which need to be injected

        // Business services bindings
        //  - PRTLabs framework
        bind(IPrtTimeProviderService.class).to(PrtComputerClockBasedTimeProviderService.class);
        //  - RLALC services
        bind(IRecordingStateManagementService.class).to(ManifestFileBasedRecordingStateManagementService.class);
        bind(IRLALCMediaCaptureService.class).to(RLALCMediaCaptureServiceImpl.class);
        bind(IMediaCapturePlanningLoader.class).to(ConfigFileBased_MediaCapturePlanningLoader.class);
        bind(IMediaRecorder.class).to(FFMpegRecorder.class);
        //  - Management of the MediaCaptureService
        bind(IEmbeddedRESTServerModule.class).to(TomcatJerseyEmbeddedRESTServerModule.class);
    }

}