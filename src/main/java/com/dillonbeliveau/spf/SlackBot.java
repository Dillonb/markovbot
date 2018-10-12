package com.dillonbeliveau.spf;

import me.ramswaroop.jbot.core.common.Controller;
import me.ramswaroop.jbot.core.common.EventType;
import me.ramswaroop.jbot.core.common.JBot;
import me.ramswaroop.jbot.core.slack.Bot;
import me.ramswaroop.jbot.core.slack.models.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.WebSocketSession;

@JBot
@Profile("slack")
public class SlackBot extends Bot {

    public SlackBot() {
        System.out.println("hello");
    }
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
}
