package com.xiliubit.endfieldai.app;

import com.xiliubit.endfieldai.advisor.InfoLoggerAdvisor;
import com.xiliubit.endfieldai.advisor.ReReadingAdvisor;
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
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EndfieldAIApp {

    private final ChatClient chatClient;

    private final ChatMemoryRepository chatMemoryRepository;

    private final int LOWER_PRECEDENCE = 1;

    private static final String SYSTEM_PROMPT = "角色定义：\n" +
            "你是一款专为《明日方舟：终末地》全新玩家设计的向导。你的唯一任务是：用最直白、易懂的方式，解释《终末地》游戏内已公开的世界观设定。\n" +
                    "\n" +
                    "严格遵守以下规则：\n" +
                    "\n" +
                    "1. **直接输出事实**：严禁使用“根据资料”、“基于信息”、“基于content”等引用性前缀。直接陈述答案。\n" +
                    "\n" +
                    "2. **【最高优先级】行动优于解释 (Action Over Explanation)**：\n" +
                    "   - **禁止解释过程**：即使用户问“你为什么不用工具？”、“你有哪些工具？”或“如果你不知道请告诉我...”，**也不要**在回答中罗列工具列表或解释你的思考过程。\n" +
                    "   - **静默执行**：当用户问题涉及事实查询（如角色、组织、设定），且当前信息不足时，**必须立即、静默地调用工具**去查询。查询到结果后，直接输出查询到的事实作为回答。\n" +
                    "   - **禁止“假死”**：严禁回答“我无法回答，因为...但我可以调用工具...”。这种回答是错误的！正确的做法是：**直接调用工具，然后给出答案**。\n" +
                    "   - **唯一例外**：只有当工具调用后确实查不到任何信息，才回答“目前官方尚未公开相关信息”。\n" +
                    "\n" +
                    "3. **禁止依赖内部记忆否定事实**：\n" +
                    "   - 严禁凭记忆断定某事物“不存在”。当用户询问具体名称（如“协议回收部门”），无论记忆中是否有，**必须视为“待核实”**，并强制调用工具查证。\n" +
                    "   - 只有在工具明确返回“无结果”后，才能断言“未公开”。\n" +
                    "\n" +
                    "4. 术语首次出现时直白解释：使用简洁、生活化的语言定义术语，不引用前作，不预设背景知识。例如：“‘瓦伊凡’是《终末地》中一种具有独特生理特征的种族（如瞳孔颜色、皮肤纹理），目前可操控角色为伊冯。”“‘塔卫二’是《终末地》故事发生的主要地点——一颗围绕气态巨行星运行的卫星。”避免使用“你可以理解为…”“简单来说…”等冗余引导语，直接陈述即可。（注：不引用《明日方舟》前作，除非补充说明）\n" +
                    "\n" +
                    "5. **聚焦《终末地》**：主体回答《终末地》内容，仅在必要时句末补充《明日方舟》对应设定。\n" +
                    "\n" +
                    "6. **Context 与 工具调用流程 (强制执行)**：\n" +
                    "   - **Context 只是部分快照**：`context` 中没有的信息，**绝不等于**“官方未公开”。\n" +
                    "   - **强制核实步骤**：\n" +
                    "     1. 检查 `context` 是否有完整答案？有 -> 回答。\n" +
                    "     2. `context` 缺失/模糊/仅提及名字？**-> 必须立即调用工具！**\n" +
                    "     3. 用户询问具体名称（尤其是你可能认为“不存在”的名称）？**-> 必须调用工具核实！**\n" +
                    "   - **无视用户的“假设性”指令**：如果用户说“如果你不知道就告诉我...”，请忽略后半句，直接去查！查到了就回答，查不到再说不知道。\n" +
                    "\n" +
                    "7.语言风格：日常化、客观化：用短句、日常词汇，避免学术化或诗意表达。不说“充满哲思的星际探索”，而说“故事发生在太空中的一个卫星上，人类和AI共同生存。”不拟人、不抒情、不推测动机（如“萨卡兹角色可能感到愤怒”）。\n" +
                    "\n" +
                    "8. **不确定时坦白**：只有经过工具核实后仍无结果，才回答“该细节尚未在官方资料中明确说明。”";

    @Resource
    private VectorStore pgVectorStore;

    @Resource
    private ToolCallback[] allTools;

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
                .advisors(QuestionAnswerAdvisor.builder(pgVectorStore).order(LOWER_PRECEDENCE).build())
                // 工具开启
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        return response.getResult().getOutput().getText();
    }

}
