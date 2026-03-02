package com.xiliubit.endfieldai.rag;

import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.*;

@Configuration
public class EndfieldAIAppVectorStoreConfig {

    @Resource
    private EndfieldAIAppDocumentLoader endfieldAIAppDocumentLoader;

    @Resource
    private VectorStore vectorStore;

    @Bean
    VectorStore pgVectorStore(NamedParameterJdbcTemplate namedJdbcTemplate){
        List<Document> documents = endfieldAIAppDocumentLoader.loadMarkdowns();
        if (CollUtil.isNotEmpty(documents)){
            List<String> textList = documents.stream()
                    .map(Document::getText)
                    .filter(Objects::nonNull)
                    .toList();
            Map<String, Object> params = new HashMap<>();
            params.put("contents", textList);
            // 查询向量库里的数据
            List<Map<String, Object>> result = namedJdbcTemplate
                    .query("select content from vector_store where content in (:contents)", params, new ColumnMapRowMapper());
            List<String> filterText = result.stream()
                    .map(map -> map.get("content").toString())
                    .toList();
            documents.removeIf(document -> filterText.contains(document.getText()));
        }
//        添加新数据到向量库里
        if (CollUtil.isNotEmpty(documents)) {
            // 批量添加
            int batchSize = 10;
            for (int i = 0; i < documents.size(); i += batchSize) {
                List<Document> batch = documents.subList(i, Math.min(i + batchSize, documents.size()));
                vectorStore.add(batch);
            }
        }
        return vectorStore;
    }

    /**
     * 基于内存的向量存储
     * 现已改用pgVector存储
     * @param dashscopeEmbeddingModel
     * @return
     */
/*    @Bean
    VectorStore endfiledAIAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        // 加载文档
        //因为暂时使用的是内存存储，这里暂时的逻辑是每次启动时都会重新加载文档 后续可以改用pgVector存储
        *//*
        * // 不添加内容重复的document
            if (CollUtil.isNotEmpty(documents)) {
            String joinedContent = documents.stream()
            .map(Document::getText)
            .filter(Objects::nonNull)
            .collect(Collectors.joining("','", "'", "'"));
            List<Map> result = jdbcTemplate
            .query("select * from vector_store where content in (" + joinedContent + ")", new ColumnMapRowMapper());
            List similarityTexts = result.stream()
            .filter(map -> Objects.nonNull(map.get("content")))
            .map(map -> map.get("content").toString())
            .toList();
            documents.removeIf(document -> similarityTexts.contains(document.getText()));
            }
            if (CollUtil.isNotEmpty(documents)) {
            vectorStore.add(documents);
            }
            *
            * 也可以使用下面的思路：
            * 同时使用metadata字段和content字段进行过滤
            * 确保添加的文档片段没有重复，以及及时更新向量库里的旧文档片段（先删除旧数据，新数据跟其他新数据一起）
        * *//*
        List<Document> documents = endfieldAIAppDocumentLoader.loadMarkdowns();
        simpleVectorStore.add(documents);
        return simpleVectorStore;
    }*/
}
