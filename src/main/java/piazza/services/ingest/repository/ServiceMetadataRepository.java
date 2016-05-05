package piazza.services.ingest.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import model.service.metadata.Service;


public interface ServiceMetadataRepository extends ElasticsearchRepository<ServiceContainer, String> {

}


