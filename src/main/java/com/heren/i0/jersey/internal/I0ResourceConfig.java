package com.heren.i0.jersey.internal;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

/**
 * Created by zengchuan on 15-9-11.
 */
public class I0ResourceConfig extends ResourceConfig{

    public I0ResourceConfig(String packages) {
        register(JacksonJaxbJsonProvider.class);
        register(MultiPartFeature.class);
        property(ServerProperties.BV_DISABLE_VALIDATE_ON_EXECUTABLE_OVERRIDE_CHECK, true);
        property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, true);
        packages(packages);
    }
}
