package com.xiliubit.endfieldai.config;

import com.xiliubit.endfieldai.tools.EndfieldWikiTool;
import com.xiliubit.endfieldai.tools.TerminateTool;
import com.xiliubit.endfieldai.tools.WebScrapingTool;
import com.xiliubit.endfieldai.tools.WebSearchTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolRegistration {

    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Bean
    public ToolCallback[] allTools() {
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        EndfieldWikiTool endfieldWikiTool = new EndfieldWikiTool();
        TerminateTool terminateTool = new TerminateTool();
        return ToolCallbacks.from(
                webSearchTool,
                webScrapingTool,
                endfieldWikiTool,
                terminateTool
        );
    }
}
