package com.xiliubit.endfieldai.app;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.UUID;

@SpringBootTest
class EndfieldAiApplicationTest {

    @Resource
    private EndfieldAIApp endfieldAIApp;

    @Test
    void testChat() {
//        String chatId = UUID.randomUUID().toString();
        String chatId = "xiliu111";
        // 第一轮
        String message = "你好，我是溪流";
        String answer = endfieldAIApp.doChat(message, chatId);
//        Assertions.assertNotNull(answer);
        // 第二轮
        message = "我想知道有没有莱万汀这个角色，你只需要回答我有或者没有就行";
        answer = endfieldAIApp.doChat(message, chatId);
//        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我叫什么名字？";
        answer = endfieldAIApp.doChat(message, chatId);
//        Assertions.assertNotNull(answer);
    }
}
