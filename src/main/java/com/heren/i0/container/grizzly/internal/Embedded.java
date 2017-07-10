package com.heren.i0.container.grizzly.internal;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceFilter;
import com.heren.i0.config.HttpConfiguration;
import com.heren.i0.core.ServletContainer;
import com.squarespace.jersey2.guice.JerseyGuiceServletContextListener;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.servlet.DefaultServlet;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.grizzly.utils.ArraySet;

import javax.servlet.DispatcherType;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class Embedded implements ServletContainer {
    private final HttpServer server;
    private Injector injector;

    public Embedded(HttpConfiguration configuration) {
//        server = HttpServer.createSimpleServer(null, configuration.getPort());
        server = new HttpServer();
        NetworkListener networkListener = new NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST, configuration.getPort());
        ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.defaultConfig();
        threadPoolConfig.setCorePoolSize(configuration.getMinThread());
        threadPoolConfig.setMaxPoolSize(configuration.getMaxThread());
        networkListener.getTransport().setWorkerThreadPoolConfig(threadPoolConfig);
        server.addListener(networkListener);

        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        serverConfiguration.setJmxEnabled(true);
    }

    @Override
    public void addServletContext(String name, boolean shareNothing, Module... modules) {
        //fixme Ioc容器会初始化两次,数据库连接池也会初始化两次
//        injector = Guice.createInjector(modules);
        WebappContext context = new WebappContext(name, name);
        context.addFilter("guice", GuiceFilter.class).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), "/*");
        context.addServlet("default", new DefaultServlet(new ArraySet(Embedded.class)) {
        }).addMapping("/*");
        JerseyGuiceServletContextListener jerseyGuiceServletContextListener = new JerseyGuiceServletContextListener() {
            @Override
            protected List<? extends Module> modules() {
                return Arrays.asList(modules);
            }
        };
        injector = jerseyGuiceServletContextListener.getInjector();
        context.addListener(jerseyGuiceServletContextListener);
        context.deploy(server);
    }

    @Override
    public Injector injector() {
        return injector;
    }

    @Override
    public void start(boolean standalone) throws Exception {
        server.start();
        while (standalone) {
            Thread.sleep(Integer.MAX_VALUE);
        }
    }


    @Override
    public void stop() throws Exception {
        server.shutdown();
    }

    public HttpServer getServer() {
        return server;
    }
}
