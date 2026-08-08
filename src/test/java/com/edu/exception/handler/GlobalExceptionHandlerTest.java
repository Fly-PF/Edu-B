package com.edu.exception.handler;

import com.edu.common.Result;
import com.edu.exception.UserErrorException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void baseExceptionMessageIsReturnedToFrontendSafely() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/teacher/ai/gradings/generate");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Result<?> result = handler.exceptionHandler(
                new UserErrorException(HttpStatus.SERVICE_UNAVAILABLE, "AI 服务暂时不可用，请稍后重试。"),
                request,
                response
        );

        assertEquals(200, response.getStatus());
        assertEquals(503, result.getCode());
        assertEquals("AI 服务暂时不可用，请稍后重试。", result.getMessage());
    }
}
