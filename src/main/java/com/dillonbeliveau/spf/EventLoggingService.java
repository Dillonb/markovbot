package com.dillonbeliveau.spf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.ramswaroop.jbot.core.slack.models.Event;
import org.springframework.stereotype.Component;

@Component
public class EventLoggingService {
    private ObjectMapper objectMapper = new ObjectMapper();

    public EventLoggingService() {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public void logEvent(Event event) throws JsonProcessingException {
        String eventJson = objectMapper.writeValueAsString(event);
    }

}
