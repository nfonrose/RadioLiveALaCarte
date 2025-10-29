package com.prtlabs.rlalc.backend.mediacapture.di.guice;

import com.google.inject.AbstractModule;
import com.prtlabs.rlalc.backend.mediacapture.services.IRLALCMediaCaptureService;
import com.prtlabs.rlalc.backend.mediacapture.services.RLALCMediaCaptureServiceImpl;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.IMediaCapturePlanningService;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.loaders.file.ConfigFileBased_MediaCapturePlanningService;
import com.prtlabs.rlalc.backend.mediacapture.services.recorders.IMediaRecorder;
import com.prtlabs.rlalc.backend.mediacapture.services.recorders.ffmpeg.FFMpegRecorder;

/**
 * Guice module for the RadioLiveALaCarte Media Capture service.
 */
public class MediaCaptureServiceGuiceModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(IRLALCMediaCaptureService.class).to(RLALCMediaCaptureServiceImpl.class);
        bind(IMediaCapturePlanningService.class).to(ConfigFileBased_MediaCapturePlanningService.class);
        bind(IMediaRecorder.class).to(FFMpegRecorder.class);
    }

}