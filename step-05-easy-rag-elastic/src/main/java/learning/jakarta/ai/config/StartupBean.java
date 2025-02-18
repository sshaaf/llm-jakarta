package learning.jakarta.ai.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Startup
@Singleton
public class StartupBean {

    @Inject
    private ConfigProp config;
    @Inject
    private EmbeddingModel embeddingModel;
    @Inject
    private EmbeddingStore<TextSegment> elasticEmbeddingStore;

    @PostConstruct
    public void init() {

        List<Document> documents = FileSystemDocumentLoader.loadDocuments(config.getDocumentsDir(), new ApacheTikaDocumentParser());
        log.info("Total documents loaded by Apache Tika: {}", documents.size());

        documents
                .forEach(document -> {
                    TextSegment textSegment = document.toTextSegment();
                    Embedding content = embeddingModel.embed(textSegment).content();
                    elasticEmbeddingStore.add(content, textSegment);
                });
    }
}