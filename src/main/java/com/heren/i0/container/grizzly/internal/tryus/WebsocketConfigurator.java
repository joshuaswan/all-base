package com.heren.i0.container.grizzly.internal.tryus;


import com.google.inject.Guice;
import com.google.inject.Injector;
import com.heren.i0.core.GuiceInjector;
import com.heren.i0.core.ServletContainer;

import javax.websocket.server.ServerEndpointConfig;

public class WebsocketConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass)
            throws InstantiationException {
        Injector injector = GuiceInjector.getInjector();
        T t = (T) injector.getInstance(endpointClass);
        return t;
    }


}
