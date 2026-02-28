package com.xiliubit.endfieldai.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class EndfieldAIAppVectorStoreConfig {

    @Resource
    private EndfieldAIAppDocumentLoader endfieldAIAppDocumentLoader;
    
    @Bean
    VectorStore endfiledAIAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        // 加载文档
        //todo  因为暂时使用的是内存存储，这里暂时的逻辑是每次启动时都会重新加载文档 后续可以改用pgVector存储
        /*
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
            * 上面的代码效率不高
            * 使用下面的思路：
            * 查询源码得知Document里的metadata有title字段，根据title字段过滤已经存储在向量库里的文档片段
            * pgVector里有一个json格式的metadata字段，里面存储了title字段
            * 所以可以先查询pgVector里已经存储的文档片段，转换成JSON对象的数组，再根据title字段过滤Document数组
            * 最后得到过滤后的Document数组，再添加到向量库里
            * 不过这样就不能修改已创建并加载的文档了，不然会无法更新到向量库里
            * 但是可以使用content字段进行过滤，只是这样效率跟上面查不多
            * 也可以同时使用title字段和content字段进行过滤，确保添加的文档片段没有重复，以及及时更新向量库里的旧文档片段
        * */
        List<Document> documents = endfieldAIAppDocumentLoader.loadMarkdowns();
        simpleVectorStore.add(documents);
        return simpleVectorStore;
    }
}
