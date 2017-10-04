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

import model.service.metadata.Service;

/*
 * Shell containing object for DataResource annotated for ElasticSearch _mapping
 * @author C. Smith
 * @Document(indexName = "pzmetadataalias", type = "DataResource")
 */

//@Document(indexName = "pzservices", type = "ServiceContainer")
public class ServiceContainer implements piazza.commons.elasticsearch.ESModel {
	// @Id
	private String serviceContainerId;

	// @Field(type = FieldType.Nested)
	private Service service;

	public ServiceContainer() {
		// Empty constructor required by Jackson
	}

	public ServiceContainer(Service s) {
		service = s;
		serviceContainerId = service.getServiceId();
	}

	@Override
	public String getId() {
		return serviceContainerId;
	}

	@Override
	public void setId(String id) {
		serviceContainerId = id;
	}

	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

	public String getServiceContainerId() {
		return serviceContainerId;
	}

	public void setServiceContainerId(String serviceContainerId) {
		this.serviceContainerId = serviceContainerId;
	}

}
