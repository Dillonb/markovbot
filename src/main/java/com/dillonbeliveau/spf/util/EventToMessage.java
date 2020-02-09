package com.dillonbeliveau.spf.util;

import me.ramswaroop.jbot.core.slack.models.Event;

import java.util.Optional;
import java.util.function.Function;

public class EventToMessage implements Function<Event, Optional<String>> {
    private EventToMessage() { }
    public static EventToMessage singleton = new EventToMessage();
    @Override
    public Optional<String> apply(Event event) {
        if ("message".equals(event.getType()) && event.getSubtype() == null) {
            return Optional.ofNullable(event.getText());
        }

        return Optional.empty();
    }
}
