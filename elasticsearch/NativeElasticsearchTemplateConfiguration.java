package mti.commons.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableAutoConfiguration
public class NativeElasticsearchTemplateConfiguration {
	@Value("${elasticsearch.clustername}")
	private String clustername;

	@Value("${elasticsearch.hostname}")
	private String hostname;

	@Value("${elasticsearch.port}")
	private Integer port;

	@Bean
	public Client client() {
		Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", clustername).build();
		TransportClient transportClient = new TransportClient(settings);
		transportClient.addTransportAddress(new InetSocketTransportAddress(hostname, port));

		return transportClient;
	}

	@Bean
	public ObjectMapper mapper() {
		return new ObjectMapper();
	}

	@Bean
	public NativeElasticsearchTemplate template(Client client, ObjectMapper mapper) {
		return new NativeElasticsearchTemplate(client, mapper);
	}
}
