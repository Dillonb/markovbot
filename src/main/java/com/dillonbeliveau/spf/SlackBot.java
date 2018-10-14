package com.dillonbeliveau.spf;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    @Autowired
    private MarkovModelService markovModelService;

    public String getSlackToken() {
        return slackBotToken;
    }

    public Bot getSlackBot() {
        return this;
    }

    @Controller(events = {EventType.DIRECT_MENTION, EventType.DIRECT_MESSAGE})
    public void onReceiveDM(WebSocketSession session, Event event) {
        //reply(session, event, responses.get(random.nextInt(responses.size())));
        String response;
        do {
            response = markovModelService.generateMessage();
        } while(response.length() < 20);


        reply(session, event, response);
    }

    @Controller(events = {EventType.MESSAGE})
    public void onPublicMessage(WebSocketSession session, Event event) throws JsonProcessingException {
        markovModelService.trainOnEvent(event);

        if (!"message_deleted".equals(event.getSubtype())) {
            eventLoggingService.logEvent(event);
        }
    }
}
