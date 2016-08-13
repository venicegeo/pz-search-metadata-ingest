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
package piazza.commons.elasticsearch.dao;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import piazza.commons.elasticsearch.ESModel;
import piazza.commons.elasticsearch.NativeElasticsearchTemplate;

public class ElasticsearchBasicDAO<T extends ESModel> {
	
	@Autowired
	protected NativeElasticsearchTemplate template;

	protected final String indexTemplate;
	protected final String documentType;
	protected final Class<T> modelClass;
	
	public ElasticsearchBasicDAO(String indexTemplate, String documentType, Class<T> modelClass) {
		super();
		this.indexTemplate = indexTemplate;
		this.documentType = documentType;
		this.modelClass = modelClass;
	}
	
	public T findOne(String uuid) {
		return template.findOne(indexTemplate, documentType, uuid, modelClass);
	}

	public T save(T instance) {
		return (template.index(indexTemplate, documentType, instance) ? instance : null);
	}

	public boolean delete(T instance) {
		return template.delete(indexTemplate, documentType, instance);
	}
	
	public int delete(Collection<T> instances) {
		return template.delete(indexTemplate, documentType, instances);
	}
	
	public void refresh() {
		template.refresh(indexTemplate);
	}
	
}
