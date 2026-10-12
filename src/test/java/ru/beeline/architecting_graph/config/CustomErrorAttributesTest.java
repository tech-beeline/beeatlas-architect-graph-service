package ru.beeline.architecting_graph.config;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CustomErrorAttributesTest {

    private final CustomErrorAttributes errorAttributes = new CustomErrorAttributes();

    @Test
    void unknownPath_returnsResourceNotFoundMessage() {
        Map<String, Object> attrs = attributesFor(404, null);

        assertEquals("ресурс по данному пути не найден", attrs.get("errorMessage"));
    }

    @Test
    void notFoundWithReason_keepsMethodMessage() {
        Map<String, Object> attrs = attributesFor(404,
                new ResponseStatusException(HttpStatus.NOT_FOUND, "ТС с id = 333333 не существует"));

        assertEquals("ТС с id = 333333 не существует", attrs.get("errorMessage"));
    }

    @Test
    void serverError_returnsExceptionMessage() {
        Map<String, Object> attrs = attributesFor(500, new IllegalStateException("Connection refused"));

        assertEquals("Connection refused", attrs.get("errorMessage"));
    }

    @Test
    void wrappedServerError_returnsCauseMessage() {
        Map<String, Object> attrs = attributesFor(500,
                new ServletException("Request processing failed", new IllegalStateException("Connection refused")));

        assertEquals("Connection refused", attrs.get("errorMessage"));
    }

    @Test
    void serverErrorWithoutMessage_returnsDefaultMessage() {
        Map<String, Object> attrs = attributesFor(500, new NullPointerException());

        assertEquals("Внутренняя ошибка сервера", attrs.get("errorMessage"));
    }

    @Test
    void serviceUnavailableWithReason_returnsReason() {
        Map<String, Object> attrs = attributesFor(503,
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Сервис недоступен"));

        assertEquals("Сервис недоступен", attrs.get("errorMessage"));
    }

    @Test
    void otherClientError_hasNoErrorMessage() {
        Map<String, Object> attrs = attributesFor(405, null);

        assertFalse(attrs.containsKey("errorMessage"));
    }

    private Map<String, Object> attributesFor(int status, Throwable error) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v4/tc");
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        if (error != null) {
            request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, error);
        }
        return errorAttributes.getErrorAttributes(
                new ServletWebRequest(request, new MockHttpServletResponse()), ErrorAttributeOptions.defaults());
    }
}
