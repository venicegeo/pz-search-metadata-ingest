/**
 * Copyright 2016, RadiantBlue Technologies, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package piazza.services.ingest.repository;

import model.data.DataResource;
import piazza.commons.elasticsearch.ESModel;
import piazza.services.ingest.util.GeoJsonDeserializer;
import piazza.services.ingest.util.GeoJsonSerializer;

import org.elasticsearch.common.geo.GeoPoint;
//import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

//import org.elasticsearch.common.geo.GeoPoint;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.elasticsearch.annotations.Document;
//import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import com.vividsolutions.jts.geom.Geometry;

/*
 * Shell containing object for DataResource annotated for ElasticSearch _mapping
 * @author C. Smith
 * @Document(indexName = "pzmetadata", type = "DataResourceContainer")
 */

//@Document(indexName = "pzmetadata", type = "DataResourceContainer")
public class DataResourceContainer implements piazza.commons.elasticsearch.ESModel {
//	@Id
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
		dataResourceContainerId = dataResource.getDataId();
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
