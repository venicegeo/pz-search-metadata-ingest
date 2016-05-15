package piazza.commons.elasticsearch;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
//old import org.elasticsearch.common.settings.ImmutableSettings;
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
	public Client client() throws UnknownHostException {
//		TransportClient client = TransportClient.builder().build()
//		        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hostname), port));
		
		Settings settings = Settings.settingsBuilder().put("cluster.name", clustername).build();
		
		TransportClient transportClient = TransportClient.builder().settings(settings).build();
		
		transportClient.addTransportAddress(new InetSocketTransportAddress(
											InetAddress.getByName(hostname), port));

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
