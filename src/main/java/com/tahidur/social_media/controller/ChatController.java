package com.tahidur.social_media.controller;

import com.tahidur.social_media.model.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

@Controller
public class ChatController {

    @MessageMapping("hello")
    @SendTo("/topic/greetings")
    public ChatMessage greeting(ChatMessage message) {
        message.setContent("Hello " + HtmlUtils.htmlEscape(message.getUsername()) + "!");
        return message;
    }
}
