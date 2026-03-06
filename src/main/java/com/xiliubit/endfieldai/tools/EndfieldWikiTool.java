package com.xiliubit.endfieldai.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.util.List;

public class EndfieldWikiTool {

    private static final String BASE_URL = "https://wiki.biligame.com/zmd/";

    @Tool(name = "getOperatorInfo", description = "这是一个工具，作用：查询游戏《明日方舟：终末地》(Endfield) 中特定干员(Operator) 的背景故事、档案或基础信息。数据来源为明日方舟终末地WIKI_BWIKI_哔哩哔哩。仅当用户询问具体角色信息时使用。如判断某名词可能为干员名称也可使用该工具")
    public String getOperatorInfo(@ToolParam(description = "干员的名字，必须是正式称谓，例如 '佩丽卡', '萤石'等，如判断可能是正式名称也可使用") String operatorName) {
        if (operatorName == null || operatorName.trim().isEmpty()) {
            return "错误：干员名字不能为空";
        }
        String url = BASE_URL + operatorName;
        try {
            Document doc = Jsoup.connect(url).get();
            Elements wikiTable = doc.getElementsByClass("archives");
            Elements basisLeft = doc.getElementsByClass("basisLeft");
            List<String> stringList = basisLeft.eachText();
            stringList.addAll(wikiTable.eachText());
            return String.join("\n", stringList);
        } catch (IOException e) {
            return "查询的名词非任何干员正式称谓，可能为别名或不存在该角色";
        }
    }
}
