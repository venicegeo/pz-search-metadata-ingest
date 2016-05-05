package mti.commons.elasticsearch.dao.trackpoint;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

import mti.commons.elasticsearch.NativeElasticsearchTemplate;
import mti.commons.elasticsearch.dao.track.ElasticsearchTrackPointDAO;
import mti.commons.partitions.PartitionManager;
import mti.commons.partitions.PartitionType;

@Configuration
public class TrackPointDAOTestConfiguration {

	@Value("${elasticsearch.clustername}")
	private String clustername;

	@Value("${elasticsearch.hostname}")
	private String hostname;

	@Value("${elasticsearch.port}")
	private Integer port;

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertiesResolver() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public Client elasticsearchClient() {
		Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", clustername).build();
		TransportClient transportClient = new TransportClient(settings);
		transportClient.addTransportAddress(new InetSocketTransportAddress(hostname, port));

		return transportClient;
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public NativeElasticsearchTemplate trackTemplate() {
		return new NativeElasticsearchTemplate(elasticsearchClient(), objectMapper());
	}

	@Bean
	public PartitionManager partitionManager() {
		return new PartitionManager(PartitionType.TRACK);
	}
	
	@Bean
	public ElasticsearchTrackPointDAO trackPointDAO() {
		return new ElasticsearchTrackPointDAO(partitionManager());
	}

}
