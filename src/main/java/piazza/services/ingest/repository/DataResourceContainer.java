package piazza.services.ingest.repository;

import model.data.DataResource;
import piazza.services.ingest.util.GeoJsonDeserializer;
import piazza.services.ingest.util.GeoJsonSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

//import org.elasticsearch.common.geo.GeoPoint;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import com.vividsolutions.jts.geom.Geometry;

/*
 * Shell containing object for DataResource annotated for ElasticSearch _mapping
 * @author C. Smith
 * @Document(indexName = "pzmetadata", type = "DataResource")
 */

@Document(indexName = "pzmetadata", type = "DataResourceContainer")
public class DataResourceContainer {
	@Id
	public String dataResourceContainerId;
	public GeoPoint locationCenterPoint;
	// serialize into ES GeoShape
	@JsonSerialize(using = GeoJsonSerializer.class)
	@JsonDeserialize(using = GeoJsonDeserializer.class)
	public Geometry boundingArea = null;
	
//	@Field(type = FieldType.Nested)
	public DataResource dataResource;

	public DataResourceContainer( ) { }
	
	public DataResourceContainer( DataResource dr )
	{
		dataResource = dr;
	}
	
	public GeoPoint getLocationCenterPoint() {
		return locationCenterPoint;
	}
	public void setLocationCenterPoint(
			GeoPoint gp ) {
		this.locationCenterPoint = gp;
	}
	
	public Geometry getBoundingArea() {
		return boundingArea;
	}
	public void setBoundingArea(
		Geometry boundingArea ) {
		this.boundingArea = boundingArea;
	}

}
