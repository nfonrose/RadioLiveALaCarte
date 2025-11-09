package com.prtlabs.rlalc.backend.mediacapture.entrypoint.embeddedrestserver;

public interface IEmbeddedRESTServerModule {

    public void start(int port, String logMessagePrefix);

}
