package com.dillonbeliveau.spf.service;

import com.dillonbeliveau.spf.MarkovModelService;
import me.ramswaroop.jbot.core.slack.models.Event;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkovModelServiceImplTest {

    private Event mockEvent(String message) {
        Event event = new Event();
        event.setType("message");
        event.setText(message);

        return event;
    }

    @Test
    public void doesTheThing() {
        String training = "This is a sentence that doesn't repeat any words.";

        MarkovModelService markovModelService = new MarkovModelServiceImpl();

        markovModelService.trainOnEvent(mockEvent(training));

        String generated = markovModelService.generateMessage();

        // Should generate the same text since it has no repeat words
        assertEquals(training, generated);
    }
}