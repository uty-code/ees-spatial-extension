package com.ees.eval.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 요청 로그(ApiLog)에 target_id로 기록할 파라미터를 지정하는 어노테이션
 * 이 어노테이션이 붙은 파라미터의 값은 ApiLog의 targetId 컬럼에 저장됩니다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogTarget {
}
