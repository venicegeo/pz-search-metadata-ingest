package piazza.services.ingest.repository;

import model.data.DataResource;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

/*
 * Shell containing object for DataResource annotated for ElasticSearch _mapping
 * @author C. Smith
 * @Document(indexName = "pzmetadata", type = "DataResource")
 */

@Document(indexName = "pzmetadata", type = "DataResource")
public class DataResourceContainer {
	@Id
	public String dataResourceContainerId;
//	@Field(type = FieldType.Nested)
	public DataResource dataResource;

	public DataResourceContainer( ) { }
	
	public DataResourceContainer( DataResource dr )
	{
		dataResource = dr;
	}
}
