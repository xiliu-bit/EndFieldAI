package com.xiliubit.endfieldai.config;

import com.xiliubit.endfieldai.chatmemoryrepository.FileBasedChatMemoryRepository;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemoryRepository fileBasedchatMemoryRepository() {
        // 基于内存
//        return new InMemoryChatMemoryRepository();

        // 基于文件
        String fileDir = System.getProperty("user.dir") + "/chat-memory-repository";
        return new FileBasedChatMemoryRepository(fileDir);
    }
}