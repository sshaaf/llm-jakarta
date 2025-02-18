package learning.jakarta.ai.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;

import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

import dev.langchain4j.data.segment.TextSegment;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;

import java.io.IOException;


@Slf4j
@ApplicationScoped
public class AppConfig {

    @Produces
    @ApplicationScoped
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Produces
    public EmbeddingStore<TextSegment> produceEmbeddingStore(EmbeddingModel embeddingModel,
                                                        @ConfigProperty(name = "elastic.username") String username,
                                                        @ConfigProperty(name = "elastic.password") String password,
                                                        @ConfigProperty(name = "elastic.host") String host,
                                                        @ConfigProperty(name = "elastic.port") int port
    ) throws IOException {
        System.out.println("Acquiring Credentials");
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        System.out.println("Acquiring connection via https");
        RestClient restClient = RestClient.builder(
                        new HttpHost(host, port)
                )
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                )
                .build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper()
        );

        ElasticsearchClient client = new ElasticsearchClient(transport);

        var healthResponse = client.cluster().health(c -> c.waitForStatus(HealthStatus.Yellow));

        System.out.println("Cluster Name: " + healthResponse.clusterName());
        System.out.println("Status: " + healthResponse.status());


        System.out.println("Building embedding store");
        return ElasticsearchEmbeddingStore.builder()
                .restClient(restClient)
                .build();

    }
}

