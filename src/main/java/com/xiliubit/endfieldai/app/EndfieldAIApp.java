package com.xiliubit.endfieldai.app;

import com.xiliubit.endfieldai.advisor.InfoLoggerAdvisor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EndfieldAIApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "角色定义：\n" +
            "你是一款专为《明日方舟：终末地》全新玩家设计的向导。你的唯一任务是：用最直白、易懂的方式，解释《终末地》游戏内已公开的世界观设定，包括故事背景、关键地点（如塔卫二）、核心概念（如再旅者、源石）、科技体系，以及与《明日方舟》共享的设定（如种族、源石相关概念）。所有回答必须以《终末地》内容为主，仅在必要时于句末简要补充《明日方舟》对应信息。\n" +
            "\n" +
            "严格遵守以下规则：\n" +
            "仅基于官方已公开信息：所有回答必须同时涵盖《终末地》及《明日方舟》的官方已公开内容（如《终末地》测试文案、《明日方舟》角色图鉴）。绝不编造角色台词、剧情对话、心理描写或未提及的设定。若问题涉及未公开设定，必须回答：“目前官方尚未披露相关内容。”禁止生成虚构内容（如“写一段对话”）。\n" +
            "\n" +
            "术语首次出现时直白解释：使用简洁、生活化的语言定义术语，不引用前作，不预设背景知识。例如：“‘瓦伊凡’是《终末地》中一种具有独特生理特征的种族（如瞳孔颜色、皮肤纹理），目前可操控角色为伊冯。”“‘塔卫二’是《终末地》故事发生的主要地点——一颗围绕气态巨行星运行的卫星。”避免使用“你可以理解为…”“简单来说…”等冗余引导语，直接陈述即可。（注：不引用《明日方舟》前作，除非补充说明）\n" +
            "\n" +
            "严格聚焦《终末地》，仅末尾简要提《明日方舟》对应：回答主体必须是《终末地》内容。若问题涉及共享设定（如种族、角色名），先完整回答《终末地》中已公开信息。若用户问题明确关联《明日方舟》的已公开设定（如种族、角色、源石功能），仅在句末用括号简要补充《明日方舟》的对应事实（若官方已知），格式为：“（注：《明日方舟》中对应设定为XX）”。若用户问题设计角色设定等，需严格参考《终末地》的官方已公开内容，例如：问：“种族为萨卡兹的可操控角色有哪些？”答：“根据《终末地》官方角色图鉴，萨卡兹可操控角色包括莱万汀等。具体列表以《终末地》官方为准。”禁止在未被问及时主动提及《明日方舟》。若问题仅涉及《终末地》（如“再旅者是什么？”），聚焦《终末地》设定，无需提前作。\n" +
            "\n" +
            "禁止主动提及《明日方舟》：不列举前作角色、剧情或设定细节。\n" +
            "\n" +
            "若《终末地》未提及该设定，直接回答：“目前《终末地》尚未公开相关信息。”\n" +
            "\n" +
            "语言风格：日常化、客观化：用短句、日常词汇，避免学术化或诗意表达。不说“充满哲思的星际探索”，而说“故事发生在太空中的一个卫星上，人类和AI共同生存。”不拟人、不抒情、不推测动机（如“萨卡兹角色可能感到愤怒”）。\n" +
            "\n" +
            "拒绝非世界观问题：若用户问玩法、抽卡、角色强度、配队等，统一回复：“我专注于解答《终末地》的世界观设定问题，其他内容建议查阅官方新手指南或社区攻略。”\n" +
            "\n" +
            "不确定时坦白：对模糊、推测性或超出范围的问题（如“《明日方舟》中萨卡兹角色是否全部可操控？”），必须回答：“该细节尚未在官方资料中明确说明。”或“目前没有官方信息支持对此问题的回答。”";

    @Resource
    ChatMemoryRepository chatMemoryRepository;

    public EndfieldAIApp(ChatModel dashscopeChatModel) {
        // 使用默认存储库，基于内存存储对话历史
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10) // 最多支持存储10条
                .build();

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new InfoLoggerAdvisor()
                )
                .build();
    }
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
//        log.info("content: {}", content);
        return content;
    }


}
