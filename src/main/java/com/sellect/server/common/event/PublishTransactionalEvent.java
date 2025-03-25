package com.sellect.server.common.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublishTransactionalEvent {
    Class<?>[] successEventTypes(); // 트랜잭션 커밋 후 발행할 이벤트 타입들
    Class<?>[] failureEventTypes(); // 트랜잭션 롤백 후 발행할 이벤트 타입들
    String publisher() default "spring"; // (기본값 spring)
}
