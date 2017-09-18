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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Geometry;

import model.data.DataResource;
import piazza.services.ingest.util.GeoJsonDeserializer;
import piazza.services.ingest.util.GeoJsonSerializer;

/*
 * Shell containing object for DataResource annotated for ElasticSearch _mapping
 * @author C. Smith
 * @Document(indexName = "pzmetadataalias", type = "DataResourceContainer")
 */

//@Document(indexName = "pzmetadataalias", type = "DataResourceContainer")
public class DataResourceContainer implements piazza.commons.elasticsearch.ESModel {
	// @Id
	private String dataResourceContainerId;

	// 8/9/16 need representation of <lat>,<lon> for correct entry,
	// without geohash, into Elasticsearch mapping of geo_point
	// 1/12/17 ObjectMapper serializes into lat,lon AND geohash (added!)
	// thus, GeoPoint in ES mapping, array representation in Java
	private Double[] locationCenterPoint; // lon, lat  - note order!
	
	// serialize into ES GeoShape
	@JsonSerialize(using = GeoJsonSerializer.class)
	@JsonDeserialize(using = GeoJsonDeserializer.class)
	private Geometry boundingArea = null;

	// @Field(type = FieldType.Nested)
	private DataResource dataResource;

	public DataResourceContainer() {
		// Empty constructor required by Jackson
	}

	public DataResourceContainer(DataResource dr) {
		dataResource = dr;
		dataResourceContainerId = dataResource.getDataId();
	}

	public Double[] getLocationCenterPoint() {
		return locationCenterPoint;
	}

	public void setLocationCenterPoint(Double[] gp) {
		this.locationCenterPoint = gp;
	}

	public Geometry getBoundingArea() {
		return boundingArea;
	}

	public void setBoundingArea(Geometry boundingArea) {
		this.boundingArea = boundingArea;
	}

	@Override
	public String getId() {
		return dataResourceContainerId;
	}

	@Override
	public void setId(String id) {
		dataResourceContainerId = id;
	}

}
