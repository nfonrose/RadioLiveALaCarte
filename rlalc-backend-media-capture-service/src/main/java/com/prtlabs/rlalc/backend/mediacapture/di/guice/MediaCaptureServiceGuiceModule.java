package com.prtlabs.rlalc.backend.mediacapture.di.guice;

import com.google.inject.AbstractModule;
import com.prtlabs.rlalc.backend.mediacapture.services.IRLALCMediaCaptureService;
import com.prtlabs.rlalc.backend.mediacapture.services.RLALCMediaCaptureServiceImpl;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.IMediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.loaders.file.ConfigFileBased_MediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.recorders.IMediaRecorder;
import com.prtlabs.rlalc.backend.mediacapture.services.recorders.ffmpeg.FFMpegRecorder;

/**
 * Guice module for the RadioLiveALaCarte Media Capture service.
 */
public class MediaCaptureServiceGuiceModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(IRLALCMediaCaptureService.class).to(RLALCMediaCaptureServiceImpl.class);
        bind(IMediaCapturePlanningLoader.class).to(ConfigFileBased_MediaCapturePlanningLoader.class);
        bind(IMediaRecorder.class).to(FFMpegRecorder.class);
    }

}