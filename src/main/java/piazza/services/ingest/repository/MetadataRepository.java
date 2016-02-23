package piazza.services.ingest.repository;

import model.data.DataResource;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;


public interface MetadataRepository extends ElasticsearchRepository<DataResourceContainer, String> {

}


