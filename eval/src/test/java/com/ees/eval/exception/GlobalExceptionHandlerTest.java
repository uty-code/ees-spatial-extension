package com.ees.eval.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.NoHandlerFoundException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GlobalExceptionHandler의 예외 처리 로직을 검증하는 테스트 클래스입니다.
 * 리다이렉트가 제거되고 직접 뷰를 반환하는 로직을 확인합니다.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("404 에러 발생 시 리다이렉트 없이 404 상태코드와 함께 에러 뷰를 직접 반환해야 한다")
    void should_return_error_view_when_nohandlerfound() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/invalid-url");
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/invalid-url", null);
        Model model = new ExtendedModelMap();

        // when
        String viewName = exceptionHandler.handleNoHandlerFoundException(ex, request, model);

        // then
        assertThat(viewName).isEqualTo("error/custom-error");
        assertThat(model.getAttribute("statusCode")).isEqualTo(404);
    }

    @Test
    @DisplayName("기타 서버 예외 발생 시 500 상태코드와 함께 에러 뷰를 직접 반환해야 한다")
    void should_return_error_view_when_general_exception() {
        // given
        Exception ex = new Exception("Test Error");
        Model model = new ExtendedModelMap();

        // when
        String viewName = exceptionHandler.handleGeneralException(ex, model);

        // then
        assertThat(viewName).isEqualTo("error/custom-error");
        assertThat(model.getAttribute("statusCode")).isEqualTo(500);
    }
}
