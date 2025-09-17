package ru.beeline.architecting_graph.config;


import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class TraceIdResponseFilter implements Filter {

    private static final String TRACE_ID_HEADER = "traceparent";
    private final W3CTraceContextPropagator propagator = W3CTraceContextPropagator.getInstance();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        if (servletResponse instanceof HttpServletResponse httpServletResponse) {
            SpanContext spanContext = Span.current().getSpanContext();
            if (spanContext.isValid()) {
                propagator.inject(
                        io.opentelemetry.context.Context.current(),
                        httpServletResponse,
                        new HttpServletResponseSetter()
                );
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private static class HttpServletResponseSetter implements TextMapSetter<HttpServletResponse> {
        @Override
        public void set(HttpServletResponse response, String key, String value) {
            response.setHeader(TRACE_ID_HEADER, value);
        }
    }
}