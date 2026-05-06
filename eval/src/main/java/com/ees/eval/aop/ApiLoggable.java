package com.ees.eval.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 중요 API 메소드에 부착하여 호출 이력을 자동으로 기록하는 커스텀 어노테이션입니다.
 *
 * <p>이 어노테이션이 붙은 컨트롤러 메소드가 실행될 때,
 * {@link ApiLoggingAspect}가 자동으로 API URL, 파라미터, 결과 등을
 * {@code api_logs_51} 테이블에 저장합니다.</p>
 *
 * <p>사용 예시:</p>
 * <pre>
 * {@code @ApiLoggable}
 * {@code @PostMapping("/submit")}
 * public String submitForm(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiLoggable {
}
