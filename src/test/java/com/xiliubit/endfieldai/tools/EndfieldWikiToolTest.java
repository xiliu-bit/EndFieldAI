package com.xiliubit.endfieldai.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class EndfieldWikiToolTest {

    @Test
    public void testGetOperatorInfo() {
        EndfieldWikiTool tool = new EndfieldWikiTool();
        String result = tool.getOperatorInfo("余烬");
        assertNotNull(result);
    }
}
