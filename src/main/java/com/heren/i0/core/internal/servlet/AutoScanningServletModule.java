package com.heren.i0.core.internal.servlet;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.servlet.ServletModule;
import com.heren.i0.core.ApplicationModule;
import com.heren.i0.core.internal.util.ClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Iterables.transform;
import static com.heren.i0.core.internal.util.ServletAnnotations.LOG_FORMATTER;
import static com.heren.i0.core.internal.util.ServletAnnotations.urlPatterns;
import static com.heren.i0.core.internal.util.TypePredicates.isFilter;
import static com.heren.i0.core.internal.util.TypePredicates.isHttpServlet;

public class AutoScanningServletModule extends ServletModule {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationModule.class);
    private final ClassScanner scanner;
    private final String[] packages;

    public AutoScanningServletModule(String... packages) {
        this.packages = packages;
        this.scanner = new ClassScanner(packages);
    }

    @Override
    protected void configureServlets() {
        if (logger.isInfoEnabled())
            logger.info("Scanning for servlet and filter classes in packages:\n  {}", on("\n  ").join(packages));

        scanHttpServletClasses();
        scanFilterClasses();
    }

    private void scanFilterClasses() {
        Set<Class<?>> filterClasses = scanner.findBy(isFilter);
        for (Class<?> filter : filterClasses)
            bind(filter.getAnnotation(WebFilter.class), (Class<? extends Filter>) filter);
        logFound("Filter", filterClasses, LOG_FORMATTER);
    }

    private void scanHttpServletClasses() {
        Set<Class<?>> servletClasses = scanner.findBy(isHttpServlet);
        for (Class<?> servlet : servletClasses)
            bind(servlet.getAnnotation(WebServlet.class), (Class<? extends HttpServlet>) servlet);
        logFound("Servlet", servletClasses, LOG_FORMATTER);
    }

    private void logFound(String type, Set<Class<?>> found, Function<Class<?>, String> formatter) {
        if (logger.isInfoEnabled())
            logger.info(found.isEmpty() ? ("No " + type.toLowerCase() + " classes found") : (type + " classes found:\n  {}"),
                    on("\n  ").join(formatter != null ? transform(found, formatter) : found));
    }


    private void bind(WebServlet servlet, Class<? extends HttpServlet> servletClass) {
        serve(urlPatterns(servletClass).asList()).with(servletClass, initParams(servlet.initParams()));
    }

    private void bind(WebFilter filter, Class<? extends Filter> filterClass) {
        filter(urlPatterns(filterClass).asList()).through(filterClass, initParams(filter.initParams()));
    }

    private ServletKeyBindingBuilder serve(ImmutableList<String> urlPatterns) {
        return serve(urlPatterns.get(0), tail(urlPatterns));
    }

    private FilterKeyBindingBuilder filter(ImmutableList<String> urlPatterns) {
        return filter(urlPatterns.get(0), tail(urlPatterns));
    }

    private Map<String, String> initParams(WebInitParam[] initParams) {
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
        for (WebInitParam initParam : initParams)
            builder.put(initParam.name(), initParam.value());
        return builder.build();
    }

    private String[] tail(ImmutableList<String> urlPatterns) {
        return toArray(urlPatterns.subList(1, urlPatterns.size()), String.class);
    }
}
