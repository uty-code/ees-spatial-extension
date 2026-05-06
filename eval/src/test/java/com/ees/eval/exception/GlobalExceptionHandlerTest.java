package com.ees.eval.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpInputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("404 에러 발생 시 커스텀 에러 페이지와 404 상태코드를 반환해야 한다")
    void should_return_404_error_page_when_nohandlerfound() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/invalid-url");
        Model model = new ExtendedModelMap();
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/invalid-url", null);

        // when
        String viewName = exceptionHandler.handleNoHandlerFoundException(ex, request, model);

        // then
        assertThat(viewName).isEqualTo("error/custom-error");
        assertThat(model.getAttribute("statusCode")).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(model.getAttribute("errorMessage")).isEqualTo("요청하신 페이지를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("JSON 파싱 에러 발생 시 400 상태코드를 반환해야 한다")
    void should_return_400_when_json_parsing_failed() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/data");
        Model model = new ExtendedModelMap();
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", mock(HttpInputMessage.class));

        // when
        String viewName = exceptionHandler.handleHttpMessageNotReadableException(ex, request, model);

        // then
        assertThat(viewName).isEqualTo("error/custom-error");
        assertThat(model.getAttribute("statusCode")).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(model.getAttribute("errorMessage")).isEqualTo("잘못된 요청 형식입니다.");
    }
}
