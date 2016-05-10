package piazza.services.ingest.repository;

import model.service.metadata.Service;

//import org.elasticsearch.common.geo.GeoPoint;
import org.springframework.data.annotation.Id;
//import org.springframework.data.elasticsearch.annotations.Document;

/*
 * Shell containing object for DataResource annotated for ElasticSearch _mapping
 * @author C. Smith
 * @Document(indexName = "pzmetadata", type = "DataResource")
 */

//@Document(indexName = "pzservices", type = "ServiceContainer")
public class ServiceContainer implements piazza.commons.elasticsearch.ESModel {
//	@Id
	public String serviceContainerId;
	
//	@Field(type = FieldType.Nested)
	public Service service;

	public ServiceContainer( ) { }
	
	public ServiceContainer( Service s )
	{
		service = s;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setId(String id) {
		// TODO Auto-generated method stub
		
	}

}
