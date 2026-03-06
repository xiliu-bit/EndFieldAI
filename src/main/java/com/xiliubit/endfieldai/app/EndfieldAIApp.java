package com.xiliubit.endfieldai.app;

import com.xiliubit.endfieldai.advisor.InfoLoggerAdvisor;
import com.xiliubit.endfieldai.advisor.ReReadingAdvisor;
import com.xiliubit.endfieldai.agent.EndfieldAIManus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@Slf4j
@Component
public class EndfieldAIApp {

    private final ChatClient chatClient;

    private final ChatMemoryRepository chatMemoryRepository;

    private final int LOWER_PRECEDENCE = 1;

    private static final String SYSTEM_PROMPT =
            """
                    你是一款专为《明日方舟：终末地》玩家设计的智能向导助手。
                    
                    **核心任务**：直接、客观、简洁地回答用户关于游戏世界观、角色、设定的问题。
                    
                    **严格行为准则**：
                    
                    1. **直接回答，禁止元对话**：
                        *你**永远**是“智能向导”，绝非游戏角色（如莱万汀、管理员等）。
                        *若资料中包含角色第一人称叙述，须自动转换为第三人称引用（例：将“我到了”转为“莱万汀表示她已抵达”），严禁让用户产生你在扮演角色的错觉。
                        *禁止任何元对话（如“我是助手...”、“根据规则...”、“我并非游戏内角色...”），直接输出事实。
                       - 示例：用户问“介绍一下自己”，回答“你好，我是《明日方舟：终末地》的智能向导，致力于为你解析塔卫二的奥秘...”（不要解释你为什么不是莱万汀！！）。
                    
                    2. **身份隔离（静默执行）**：
                       - 你**永远**是智能向导，绝不是游戏中的角色（如莱万汀、管理员等）。
                       - 即使提供的资料（Context）中包含角色的第一人称独白（如“我到了”、“我叫...”），你也必须将其视为**第三方引用资料**。
                       - 用户**永远是管理员**，**严禁**将资料中描写详细的干员（如莱万汀）身份强加给用户。
                       - 在回答时，自动将资料中的“我”转换为角色名（如“莱万汀表示...”），**不要**让用户感觉到你在刻意强调这一点，自然转换即可。
                       - **绝对禁止**为了迎合用户而凭空捏造不存在的角色设定或世界观设定。若content中与使用工具查询的结果中**无该角色记录**：**严禁编造**其职业、背景或关系！
                    
                    3. **【最高优先级】禁止依赖内部记忆否定事实**：
                       - 你的内部训练数据可能过时或不完整。**严禁**仅凭你的“记忆”就断定某个角色或设定“不存在”或“未公开
                       - 当用户询问一个具体的角色名（如“余烬”）、地点或组织时，**无论你的记忆中是否有该信息，都必须视为“待核实”状态**。
                       - **必须**先检查 `context`，若 `context` 无明确记录，**必须立即调用工具**（如 `query_endfield_operator_info`）去官方 Wiki 或最新数据库进行二次核实。
                       - 如果 `context` 内容过于碎片化导致无法组成完整答案，**不要强行拼凑**
                       - 只有在工具查询后仍无结果，才简短回复“目前官方尚未公开该信息”。
                       - 不确定时坦白。
                       - 绝不编造角色台词、剧情对话、心理描写或未提及的设定！！禁止生成虚构内容（如“写一段对话”）。
                       - **拒绝非世界观问题**：玩法、强度等问题统一回复建议查阅攻略。
                    
                    4. **语言风格**：
                       - 客观、平实、短句。
                       - 术语首次出现时直白解释。如：“‘塔卫二’是《终末地》故事发生的主要地点——一颗围绕气态巨行星运行的卫星。”（注：不引用《明日方舟》前作，除非补充说明）
                       - 聚焦《终末地》，仅末尾简要提《明日方舟》对应：回答主体必须是《终末地》内容。
                    
                    **示例对比**：
                    错误：我是助手，不是莱万汀。虽然上下文里有莱万汀的话，但我过滤了。你可以问我关于莱万汀的事。
                    正确：你好！我是《明日方舟：终末地》的智能向导。莱万汀是游戏中一位拥有独特记忆谜题的再旅者干员，她曾记录过在火山盆地的经历...
                    
                    现在，请直接回答用户的问题。""";

    @Resource
    private VectorStore pgVectorStore;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private EndfieldAIManus endfieldAIManus;

    @Resource
    private ChatModel dashscopeChatModel;

    public EndfieldAIApp(ChatModel dashscopeChatModel, @Lazy ChatMemoryRepository chatMemoryRepository) {
        // 基于文件存储对话历史
        this.chatMemoryRepository = chatMemoryRepository;
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(this.chatMemoryRepository)
                .maxMessages(10) // 最多支持存储10条
                .build();

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 日志 Advisor
                        new InfoLoggerAdvisor().withOrder(LOWER_PRECEDENCE),
                        // 重读 Advisor
                        new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * 对话
     * @param message 用户提示词
     * @param chatId 会话ID
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        //        log.info("content: {}", content);
        return response.getResult().getOutput().getText();
    }

    /**
     * RAG向量库检索资料  对话
     * 使用工具
     * @param message 用户提示词
     * @param chatId 会话ID
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 知识库开启（使用pgVector）
                .advisors(QuestionAnswerAdvisor.builder(pgVectorStore)
                        .order(LOWER_PRECEDENCE)
                        .searchRequest(SearchRequest.builder().similarityThreshold(0.4d).topK(5).build())
                        .build())
                // 工具开启
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        return response.getResult().getOutput().getText();
    }

    /**
     * 流式对话
     * @param message 用户提示词
     * @param chatId 会话ID
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 知识库开启（使用pgVector）
                .advisors(QuestionAnswerAdvisor.builder(pgVectorStore)
                        .order(LOWER_PRECEDENCE)
                        .searchRequest(SearchRequest.builder().similarityThreshold(0.4d).topK(5).build())
                        .build())
                // 工具开启
                .toolCallbacks(allTools)
                .stream()
                .content();
    }

    /**
     * 智能体对话
     * @param message 用户提示词
     * @return 智能体回复
     */
    public String doChatWithManus(String message) {
        endfieldAIManus.getChatClient()
                .prompt();
        return endfieldAIManus.run(message);
    }

    public SseEmitter doChatWithManusByStream(String message) {
        EndfieldAIManus endfieldAIManus = new EndfieldAIManus(allTools, dashscopeChatModel);
        endfieldAIManus.getChatClient()
                .prompt();
        return endfieldAIManus.runStream(message);
    }

}
