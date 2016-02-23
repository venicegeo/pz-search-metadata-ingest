package piazza.services.ingest;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableAutoConfiguration
@EnableElasticsearchRepositories(basePackages="piazza.services.ingest.repository", repositoryImplementationPostfix="CustomImpl")
public class ElasticsearchConfiguration {
	
	@Value("${elasticsearch.clustername}")
	public String clustername;
	
	@Value("${elasticsearch.hostname}")
	private String hostname;
	
	@Value("${elasticsearch.port}")
	private Integer port;
	
	@Bean
	public ElasticsearchTemplate elasticsearchTemplate() {
		Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", clustername).build();
		TransportClient transportClient = new TransportClient(settings);
		transportClient.addTransportAddress(new InetSocketTransportAddress(hostname, port));
		return new ElasticsearchTemplate(transportClient);
	}
}
