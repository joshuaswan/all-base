package com.heren.i0.jersey.internal;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.heren.i0.config.Configuration;
import com.heren.i0.core.ApplicationModule;
import com.heren.i0.core.BindingProvider;
import com.heren.i0.core.internal.servlet.AutoScanningServletModule;
import com.heren.i0.jersey.RestApi;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.reflections.Reflections;

import javax.ws.rs.Path;

import java.util.Set;

import static com.google.common.base.Joiner.on;

public class JerseyEnabler implements BindingProvider<RestApi, Configuration> {

    @Override
    public void configure(Binder binder, final RestApi annotation, ApplicationModule<?> module, Configuration configuration) {
        final String[] autoScanPackages = new String[]{module.getClass().getPackage().getName()};
        binder.install(new AutoScanningServletModule() {

            @Override
            protected void configureServlets() {

                ImmutableSet<String> packageSet = ImmutableSet.<String>builder()
                        .add(annotation.packages().length == 0 ? autoScanPackages : annotation.packages()).build();

                if(packageSet != null && packageSet.size() > 0){
                    for(String packageName : packageSet){
                        Reflections reflections = new Reflections(packageName);
                        Set<Class<?>> classSet = reflections.getTypesAnnotatedWith(Path.class);
                        if(classSet != null && classSet.size() > 0){
                            for(Class<?> resouce : classSet){
                                bind(resouce);
                            }
                        }
                    }
                }

//                bind(ResourceConfig.class).toInstance(new MyApplication(on(",").skipNulls().join(packageSet), org.glassfish.jersey.jackson.JacksonFeature.class));
                I0ResourceConfig i0ResourceConfig = new I0ResourceConfig(on(",").skipNulls().join(packageSet));
                bind(ResourceConfig.class).toInstance(i0ResourceConfig);
                ServletContainer servletContainer = new ServletContainer(i0ResourceConfig);
                serve(annotation.prefix()).with(servletContainer, new ImmutableMap.Builder<String, String>()
                        .put(ServerProperties.PROVIDER_PACKAGES, on(",").skipNulls().join(packageSet)).build());
//                serve(annotation.prefix()).with(Application.class);

            }
        });
    }
}
