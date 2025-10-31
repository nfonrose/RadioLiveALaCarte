package com.prtlabs.rlalc.backend.mediacapture.entrypoint;

import com.google.inject.AbstractModule;
import com.prtlabs.rlalc.backend.mediacapture.services.IRLALCMediaCaptureService;
import com.prtlabs.rlalc.backend.mediacapture.services.RLALCMediaCaptureServiceImpl;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.IMediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.loaders.file.ConfigFileBased_MediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.recorders.IMediaRecorder;
import com.prtlabs.rlalc.backend.mediacapture.services.recorders.ffmpeg.FFMpegRecorder;
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
        bind(IRLALCMediaCaptureService.class).to(RLALCMediaCaptureServiceImpl.class);
        bind(IMediaCapturePlanningLoader.class).to(ConfigFileBased_MediaCapturePlanningLoader.class);
        bind(IMediaRecorder.class).to(FFMpegRecorder.class);
    }

}