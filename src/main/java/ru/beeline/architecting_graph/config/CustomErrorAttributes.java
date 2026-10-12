package ru.beeline.architecting_graph.config;

import javax.servlet.ServletException;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static ru.beeline.architecting_graph.utils.Constant.INTERNAL_SERVER_ERROR_MESSAGE;
import static ru.beeline.architecting_graph.utils.Constant.RESOURCE_NOT_FOUND_MESSAGE;

@Component
public class CustomErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> attrs = super.getErrorAttributes(webRequest, options);
        Integer status = (Integer) attrs.get("status");
        if (status == null) {
            return attrs;
        }
        Throwable error = unwrap(getError(webRequest));
        if (status == 404) {
            attrs.put("errorMessage", orDefault(reasonOf(error), RESOURCE_NOT_FOUND_MESSAGE));
        } else if (status >= 500) {
            attrs.put("errorMessage", orDefault(messageOf(error), INTERNAL_SERVER_ERROR_MESSAGE));
        }
        return attrs;
    }

    private Throwable unwrap(Throwable error) {
        while (error instanceof ServletException && error.getCause() != null) {
            error = error.getCause();
        }
        return error;
    }

    private String reasonOf(Throwable error) {
        return error instanceof ResponseStatusException ? ((ResponseStatusException) error).getReason() : null;
    }

    private String messageOf(Throwable error) {
        if (error instanceof ResponseStatusException) {
            return reasonOf(error);
        }
        return error == null ? null : error.getMessage();
    }

    private String orDefault(String message, String defaultMessage) {
        return message == null || message.isBlank() ? defaultMessage : message;
    }
}
