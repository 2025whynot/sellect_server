package com.sellect.server.common.event;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TransactionalEventPublishingAspect {
    private final TransactionalEventPublisher eventPublisher;

    public TransactionalEventPublishingAspect(TransactionalEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Around("@annotation(publishTransactionalEvent)")
    public Object handleEventPublishing(ProceedingJoinPoint joinPoint,
        PublishTransactionalEvent publishTransactionalEvent) throws Throwable {
        Object result;
        List<Object> successEvents;
        List<Object> failureEvents;
        String publisherName = publishTransactionalEvent.publisher();

        try {
            result = joinPoint.proceed();
            successEvents = createEvents(publishTransactionalEvent.successEventTypes(), result);
            failureEvents = createEvents(publishTransactionalEvent.failureEventTypes(), result);
        } catch (Throwable e) {
            failureEvents = createEvents(publishTransactionalEvent.failureEventTypes(), null);
            eventPublisher.registerEvents(null, failureEvents, publisherName);
            throw e;
        }

        eventPublisher.registerEvents(successEvents, failureEvents, publisherName);
        return result;
    }

    private List<Object> createEvents(Class<?>[] eventTypes, Object result) {
        return Arrays.stream(eventTypes)
            .filter(eventType -> eventType != Void.class)
            .map(eventType -> {
                try {
                    return eventType.getConstructor(Object.class).newInstance(result);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create event: " + eventType.getName(), e);
                }
            })
            .collect(Collectors.toList());
    }
}
