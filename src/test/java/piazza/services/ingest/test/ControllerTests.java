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
package piazza.services.ingest.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletResponse;

import model.data.DataResource;
import model.data.type.GeoJsonDataType;
import model.job.metadata.ResourceMetadata;
import model.job.type.SearchMetadataIngestJob;
import model.response.DataResourceResponse;
import piazza.commons.elasticsearch.NativeElasticsearchTemplate;
import piazza.services.ingest.controller.Controller;
import piazza.services.ingest.repository.DataResourceContainer;
import util.PiazzaLogger;

/**
 * Tests simple functionality of the Search Ingest controller.
 * 
 * @author Patrick.Doody
 *
 */
public class ControllerTests {
	@Mock
	private PiazzaLogger logger;
	@Mock
	NativeElasticsearchTemplate template;
	@InjectMocks
	Controller controller;

	/**
	 * Initial test setup
	 */
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	/**
	 * Tests the health endpoint
	 */
	@Test
	public void testHealth() {
		String health = controller.checkme();
		Assert.assertTrue(health.length() > 0);
	}

	/**
	 * Tests admin/stats endpoint
	 */
	@Test
	public void testAdminStats() throws IOException {
		// Ensuring no Exceptions are thrown
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		controller.stats(mockResponse);
	}

	/**
	 * Test the ingest of a Data Resource
	 */
	@Test
	public void testDataIngest() throws Exception {
		// Mock
		DataResource mockData = new DataResource();
		mockData.setDataId("123456");
		GeoJsonDataType mockDataType = new GeoJsonDataType();
		mockDataType.geoJsonContent = "{\"type\": \"FeatureCollection\",\"features\": [{\"type\": \"Feature\",\"geometry\": {\"type\": \"Point\",\"coordinates\": [102.0,0.5]},\"properties\": {\"prop0\": \"value0\"}},{\"type\": \"Feature\",\"geometry\": {\"type\": \"Point\",\"coordinates\": [106.0,4]},\"properties\": {\"prop0\": \"value0\"}}]}";
		mockData.dataType = mockDataType;
		mockData.metadata = new ResourceMetadata();
		mockData.metadata.setName("Test GeoJSON");

		SearchMetadataIngestJob mockJob = new SearchMetadataIngestJob();
		mockJob.setData(mockData);

		// Test
		DataResourceResponse response = controller.ingestMetadataJob(mockJob);

		// Verify
		Assert.assertTrue(response != null);
		Assert.assertTrue(response.data.getDataId().equals(mockData.getDataId()));
		Assert.assertTrue(response.data.getDataType() instanceof GeoJsonDataType);
		GeoJsonDataType dataType = (GeoJsonDataType) response.data.getDataType();
		Assert.assertTrue(dataType.geoJsonContent.length() == mockDataType.geoJsonContent.length());
	}

	/**
	 * Test the error condition for a Data Resource ingest
	 */
	@Test(expected = Exception.class)
	public void testDataIngestError() throws Exception {
		// Mock
		SearchMetadataIngestJob mockJob = new SearchMetadataIngestJob();

		// Test - ensure exception is thrown
		controller.ingestMetadataJob(mockJob);
	}

	/**
	 * Test the ingest of a Service
	 */
	@Test
	public void testServiceIngest() {
		// Mock

		// Test

		// Verify
	}

	/**
	 * Test the error condition for a Service ingest
	 */
	@Test(expected = Exception.class)
	public void testServiceIngestError() throws Exception {
		// Mock

		// Test - ensure exception is thrown

	}
}
