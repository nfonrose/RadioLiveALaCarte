package com.prtlabs.rlalc.backend.mediacapture.entrypoint;

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
import com.prtlabs.rlalc.backend.mediacapture.utils.RLALCLocalTimeZoneTimeHelper;
import com.prtlabs.utils.dependencyinjection.hk2.quartz.PrtHK2QuartzJobFactory;
import com.prtlabs.utils.time.provider.IPrtTimeProviderService;
import com.prtlabs.utils.time.provider.PrtComputerClockBasedTimeProviderService;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.quartz.spi.JobFactory;

/**
 * HK2 module for the RadioLiveALaCarte Media Capture service.
 */
public class MediaCaptureServiceHK2Module extends AbstractBinder {
    
    @Override
    protected void configure() {
        // Technical bindings
        bind(PrtHK2QuartzJobFactory.class).to(JobFactory.class);    // Needed for Quartz jobs which need to be injected

        // Business services bindings
        //  - PRTLabs framework
        bind(PrtComputerClockBasedTimeProviderService.class).to(IPrtTimeProviderService.class);
        //  - RLALC services
        bind(ManifestFileBasedRecordingStateManagementService.class).to(IRecordingStateManagementService.class);
        bind(RLALCMediaCaptureServiceImpl.class).to(IRLALCMediaCaptureService.class);
        bind(ConfigFileBased_MediaCapturePlanningLoader.class).to(IMediaCapturePlanningLoader.class);
        bind(FFMpegRecorder.class).to(IMediaRecorder.class);
        bind(RLALCLocalTimeZoneTimeHelper.class).to(RLALCLocalTimeZoneTimeHelper.class);
        bind(PrtHK2QuartzJobFactory.class).to(PrtHK2QuartzJobFactory.class);
        //  - Management of the MediaCaptureService
        bind(TomcatJerseyEmbeddedRESTServerModule.class).to(IEmbeddedRESTServerModule.class);
    }
}