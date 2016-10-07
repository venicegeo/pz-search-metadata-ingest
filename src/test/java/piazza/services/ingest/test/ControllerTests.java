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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletResponse;

import model.data.DataResource;
import model.data.type.GeoJsonDataType;
import model.job.metadata.ResourceMetadata;
import model.job.type.SearchMetadataIngestJob;
import model.response.DataResourceResponse;
import model.response.ErrorResponse;
import model.response.PiazzaResponse;
import model.response.ServiceResponse;
import model.response.SuccessResponse;
import model.service.metadata.Service;
import piazza.commons.elasticsearch.NativeElasticsearchTemplate;
import piazza.services.ingest.controller.Controller;
import piazza.services.ingest.repository.ServiceContainer;
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
	 * Tests ES initialization
	 */
	@Test
	public void testInit() throws Exception {
		// Ensuring no Exceptions are thrown
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		controller.init();
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
	public void testServiceIngest() throws Exception {
		// Mock
		Service mockService = new Service();
		mockService.setUrl("http://test.com/service");
		mockService.setContractUrl("http://test.com/contract");
		mockService.setMethod("GET");
		mockService.setResourceMetadata(new ResourceMetadata());
		mockService.setServiceId("123456");
		mockService.getResourceMetadata().setName("Test Service");

		// Test
		ServiceResponse response = controller.ingestServiceDoc(mockService);

		// Verify
		Assert.assertTrue(response != null);
		Assert.assertTrue(response.data.getServiceId().equals(mockService.getServiceId()));
		Assert.assertTrue(response.data.getUrl().equals(mockService.getUrl()));
		Assert.assertTrue(response.data.getMethod().equals(mockService.getMethod()));
		Assert.assertTrue(response.data.getResourceMetadata().getName().equals(mockService.getResourceMetadata().getName()));
	}

	/**
	 * Test the deletion of a Service
	 */
	@Test
	public void testServiceDelete() throws Exception {
		// Mock
		Service mockService = new Service();
		mockService.setUrl("http://test.com/service");
		mockService.setContractUrl("http://test.com/contract");
		mockService.setMethod("GET");
		mockService.setResourceMetadata(new ResourceMetadata());
		mockService.setServiceId("123456");
		mockService.getResourceMetadata().setName("Test Service");

		// Test. Template will return null.
		PiazzaResponse response = controller.deleteServiceDocById(mockService);

		// Verify that error is sent.
		Assert.assertTrue(response instanceof ErrorResponse);

		// Mock Template so that a ServiceContainer is returned
		ServiceContainer mockContainer = new ServiceContainer(mockService);
		Mockito.doReturn(mockContainer).when(template).findOne(Mockito.eq("pzservices"), Mockito.eq("ServiceContainer"),
				Mockito.eq("123456"), Mockito.any());

		// Re-test
		response = controller.deleteServiceDocById(mockService);

		// Verify correct
		Assert.assertTrue(response instanceof SuccessResponse);
	}

	/**
	 * Test updating a Service
	 */
	@Test
	public void testServiceUpdate() throws Exception {
		// Mock
		Service mockService = new Service();
		mockService.setUrl("http://test.com/service");
		mockService.setContractUrl("http://test.com/contract");
		mockService.setMethod("GET");
		mockService.setResourceMetadata(new ResourceMetadata());
		mockService.setServiceId("123456");
		mockService.getResourceMetadata().setName("Test Service");
		ServiceContainer mockContainer = new ServiceContainer(mockService);

		Mockito.doReturn(mockContainer).when(template).findOne(Mockito.eq("pzservices"), Mockito.eq("ServiceContainer"),
				Mockito.eq("123456"), Mockito.any());
		Mockito.doReturn(new Boolean(true)).when(template).index(Mockito.eq("pzservices"), Mockito.eq("ServiceContainer"), Mockito.any());
		Mockito.doReturn(true).when(template).delete(Mockito.eq("pzservices"), Mockito.eq("ServiceContainer"),
				Mockito.any(ServiceContainer.class));

		// Test
		Boolean isSuccess = controller.updateServiceDocById(mockService);

		// Verify
		Assert.assertTrue(isSuccess.booleanValue());
	}

	/**
	 * Test the error condition for a Service ingest
	 */
	@Test(expected = Exception.class)
	public void testServiceIngestError() throws Exception {
		// Mock null data
		Service mockService = null;

		// Test - ensure exception is thrown
		controller.ingestServiceDoc(mockService);
	}
}
