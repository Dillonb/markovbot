package com.dillonbeliveau.spf;

import me.ramswaroop.jbot.core.slack.models.Event;

public interface MarkovModelService {
    String generateMessage();
    void trainOnEvent(Event event);
}
