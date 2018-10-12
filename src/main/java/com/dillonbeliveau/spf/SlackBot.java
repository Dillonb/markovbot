package com.dillonbeliveau.spf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import me.ramswaroop.jbot.core.common.Controller;
import me.ramswaroop.jbot.core.common.EventType;
import me.ramswaroop.jbot.core.common.JBot;
import me.ramswaroop.jbot.core.slack.Bot;
import me.ramswaroop.jbot.core.slack.models.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Random;

@JBot
@Profile("slack")
public class SlackBot extends Bot {
    Random random = new Random();
    @Autowired
    private EventLoggingService eventLoggingService;
    @Value("${slackBotToken}")
    private String slackBotToken;

    List<String> responses = new ImmutableList.Builder<String>()
            .add("I AM THE ONE WHO SHITPOSTS")
            .add("Bustin' makes me feel good!")
            .build();

    public String getSlackToken() {
        return slackBotToken;
    }

    public Bot getSlackBot() {
        return this;
    }

    @Controller(events = {EventType.DIRECT_MENTION, EventType.DIRECT_MESSAGE})
    public void onReceiveDM(WebSocketSession session, Event event) {
        reply(session, event, responses.get(random.nextInt(responses.size())));
    }

    @Controller(events = {EventType.MESSAGE})
    public void onPublicMessage(WebSocketSession session, Event event) throws JsonProcessingException {
        if (event.getText() != null && event.getText().toLowerCase().contains("bustin")) {
            reply(session, event, "Lemme tell ya somethin'.");
            reply(session, event, "BUSTIN' MAKES ME FEEL GOOD!");
        }

        if ("message_deleted".equals(event.getSubtype())) {
            eventLoggingService.logEvent(event);
        }
    }
}
