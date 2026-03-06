package com.xiliubit.endfieldai.controller;

import com.xiliubit.endfieldai.app.EndfieldAIApp;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private EndfieldAIApp endfieldAIApp;

    /**
     * 同步调用 EndfieldApp
     *
     * @param message
     * @return
     */
    @GetMapping("/endfield_app/chat/sync")
    public String doChatWithEndfieldAppSync(String message, String chatId) {
        return endfieldAIApp.doChatWithRag(message, chatId);
    }

    /**
     * 流式调用 EndfieldApp
     *
     * @param message
     * @return
     */
    @GetMapping( value = "/endfield_app/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithEndfieldAppStream(String message, String chatId) {
        return endfieldAIApp.doChatByStream(message, chatId);
    }

    /**
     * 流式调用 Manus 超级智能体
     *
     * @param message
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        return endfieldAIApp.doChatWithManusByStream(message);
    }

}
