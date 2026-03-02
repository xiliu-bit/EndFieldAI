package com.xiliubit.endfieldai.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class WebScrapingToolTest {

    @Test
    public void testScrapeWebPage() {
        WebScrapingTool tool = new WebScrapingTool();
        String url = "https://baike.baidu.com/item/%E8%90%A4%E7%9F%B3/66955361";
        url = "https://wiki.biligame.com/zmd/?curid=334";
        url = "https://wiki.biligame.com/zmd/余烬";
        String result = tool.scrapeWebPage(url);
        assertNotNull(result);
    }
}
