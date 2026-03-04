package com.xiliubit.endfieldai.controller;

import com.xiliubit.endfieldai.app.EndfieldAIApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private EndfieldAIApp endfieldAIApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @GetMapping("/endfield_app/chat/sync")
    public String doChatWithEndfieldAppSync(String message, String chatId) {
        return endfieldAIApp.doChatWithRag(message, chatId);
    }

    @GetMapping( value = "/endfield_app/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithEndfieldAppStream(String message, String chatId) {
        return endfieldAIApp.doChatByStream(message, chatId);
    }
}
