package com.tracenixa.platform.replay.sandbox;

import com.tracenixa.platform.replay.model.RecordedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SandboxToolAdapter {

    private final Map<String, List<RecordedEvent>> recordedEventCache = new ConcurrentHashMap<>();

    public void registerRecordedEvents(String replaySessionId, List<RecordedEvent> events) {
        recordedEventCache.put(replaySessionId, new ArrayList<>(events));
    }

    public Optional<RecordedEvent> getNextRecordedToolResponse(String replaySessionId, String toolName) {
        List<RecordedEvent> list = recordedEventCache.get(replaySessionId);
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }

        for (Iterator<RecordedEvent> it = list.iterator(); it.hasNext(); ) {
            RecordedEvent ev = it.next();
            if ("TOOL_CALL".equalsIgnoreCase(ev.getEventType()) && ev.getOperationName().equalsIgnoreCase(toolName)) {
                it.remove();
                return Optional.of(ev);
            }
        }
        return Optional.empty();
    }

    public void cleanUpSession(String replaySessionId) {
        recordedEventCache.remove(replaySessionId);
    }
}
