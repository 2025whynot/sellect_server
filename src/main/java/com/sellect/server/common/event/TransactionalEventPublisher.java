package com.sellect.server.common.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class TransactionalEventPublisher {
    private static final String EVENT_LIST_KEY = "transactional_events";

    private final Map<String, EventPublisher> publishers;

    public TransactionalEventPublisher(Map<String, EventPublisher> publishers) {
        this.publishers = publishers;
    }

    public void registerEvents(List<Object> successEvents, List<Object> failureEvents, String publisherName) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            List<EventHolder> events = getEventsFromTransaction();
            events.add(new EventHolder(successEvents, failureEvents, publisherName));
            TransactionSynchronizationManager.registerSynchronization(new EventSynchronization(events));
        } else {
            EventPublisher publisher = publishers.getOrDefault(publisherName, publishers.get("kafka"));
            successEvents.forEach(publisher::publish);
        }
    }

    @SuppressWarnings("unchecked")
    private List<EventHolder> getEventsFromTransaction() {
        List<EventHolder> events = (List<EventHolder>) TransactionSynchronizationManager.getResource(EVENT_LIST_KEY);
        if (events == null) {
            events = new ArrayList<>();
            TransactionSynchronizationManager.bindResource(EVENT_LIST_KEY, events);
        }
        return events;
    }

    private class EventSynchronization implements TransactionSynchronization {
        private final List<EventHolder> events;

        public EventSynchronization(List<EventHolder> events) {
            this.events = events;
        }

        @Override
        public void afterCommit() {
            for (EventHolder holder : events) {
                EventPublisher publisher = publishers.getOrDefault(holder.publisherName, publishers.get("kafka"));
                holder.successEvents.forEach(publisher::publish);
            }
        }

        @Override
        public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) {
                for (EventHolder holder : events) {
                    EventPublisher publisher = publishers.getOrDefault(holder.publisherName, publishers.get("kafka"));
                    holder.failureEvents.forEach(publisher::publish);
                }
            }
            TransactionSynchronizationManager.unbindResourceIfPossible(EVENT_LIST_KEY);
        }
    }

    public record EventHolder(List<Object> successEvents, List<Object> failureEvents,
                               String publisherName) {

    }
}
