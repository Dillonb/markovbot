package com.dillonbeliveau.spf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.ramswaroop.jbot.core.common.Controller;
import me.ramswaroop.jbot.core.common.EventType;
import me.ramswaroop.jbot.core.common.JBot;
import me.ramswaroop.jbot.core.slack.Bot;
import me.ramswaroop.jbot.core.slack.models.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.WebSocketSession;

@JBot
@Profile("slack")
public class SlackBot extends Bot {
    @Autowired
    private EventLoggingService eventLoggingService;
    @Value("${slackBotToken}")
    private String slackBotToken;


    public String getSlackToken() {
        return slackBotToken;
    }

    public Bot getSlackBot() {
        return this;
    }

    @Controller(events = {EventType.DIRECT_MENTION, EventType.DIRECT_MESSAGE})
    public void onReceiveDM(WebSocketSession session, Event event) {
        reply(session, event, "I AM THE ONE WHO SHITPOSTS");
    }

    @Controller(events = {EventType.MESSAGE})
    public void onPublicMessage(WebSocketSession session, Event event) throws JsonProcessingException {
        eventLoggingService.logEvent(event);
    }
}
