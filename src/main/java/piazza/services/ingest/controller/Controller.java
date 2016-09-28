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
package piazza.services.ingest.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.common.geo.GeoPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import model.data.DataResource;
import model.job.metadata.SpatialMetadata;
import model.job.type.SearchMetadataIngestJob;
import model.response.DataResourceResponse;
import model.response.ErrorResponse;
import model.response.PiazzaResponse;
import model.response.ServiceResponse;
import model.response.SuccessResponse;
import model.service.metadata.Service;
import piazza.commons.elasticsearch.NativeElasticsearchTemplate;
import piazza.services.ingest.repository.DataResourceContainer;
import piazza.services.ingest.repository.ServiceContainer;
import piazza.services.ingest.util.GeometryUtils;
import util.PiazzaLogger;

@RestController
public class Controller {

	private final String API_ROOT = "${api.basepath}";

	static final String DATAINDEX = "pzmetadata";
	static final String DATATYPE = "DataResourceContainer";
	static final String SERVICESINDEX = "pzservices";
	static final String SERVICESTYPE = "ServiceContainer";

	@Autowired
	private PiazzaLogger logger;

	@Autowired
	NativeElasticsearchTemplate template;

	@RequestMapping("/")
	@ResponseBody
	public String checkme() {
		return ("Hello pz-search-metadata-ingest.  Let\'s have some metadata for search!");
	}

	/**
	 * Statistics from Spring Boot
	 * 
	 * @return json as statistics
	 */
	@RequestMapping(value = "/admin/stats", method = RequestMethod.GET)
	public void stats(HttpServletResponse response) throws IOException {
		response.sendRedirect("/metrics");
	}

	/*
	 * endpoint ingesting SearchMetadataIngestJob containing data/metadata resource object
	 * 
	 * @return dataResource object ingested
	 */
	@RequestMapping(value = API_ROOT + "/data", method = RequestMethod.POST, consumes = "application/json")
	public DataResourceResponse ingestMetadataJob(@RequestBody(required = true) SearchMetadataIngestJob mdingestJob) throws Exception {

		/*
		 * Block for debug purposes if needed // get reconstituted JSON Doc out of job object parameter
		 */
		String reconJSONdoc;
		try {
			ObjectMapper mapper = new ObjectMapper();
			reconJSONdoc = mapper.writeValueAsString(mdingestJob.getData());
			System.out.println("The Re-Constituted JSON Doc:\n");
			System.out.println(reconJSONdoc);
		} catch (Exception exception) {
			String message = String.format("Error Reconstituting JSON Doc from SearchMetadataIngestJob: %s", exception.getMessage());
			logger.log(message, PiazzaLogger.ERROR);
			throw new Exception(message);
		}

		try {
			DataResource dr;
			dr = mdingestJob.getData();
			DataResourceContainer drc = new DataResourceContainer(dr);
			try {
				SpatialMetadata sm = dr.getSpatialMetadata().getProjectedSpatialMetadata();
				Double minX = sm.getMinX();
				Double maxX = sm.getMaxX();
				Double minY = sm.getMinY();
				Double maxY = sm.getMaxY();
				GeoPoint gp = new GeoPoint((maxY + minY) / 2, (maxX + minX) / 2);
				drc.setLocationCenterPoint(gp);

				Coordinate NW = new Coordinate(minX, maxY);
				Coordinate SE = new Coordinate(maxX, minY);
				Geometry bboxGeometry = GeometryUtils.createBoundingBox(NW, SE);
				drc.setBoundingArea(bboxGeometry);

			} catch (Exception exception) {
				try { // in case test or for some other reason null metadata values
					String message = String.format(
							"Error Augmenting JSON Doc with geolocation info, DataId: %s, possible null values input or unrecognized SRS: %s",
							dr.getDataId(), dr.getSpatialMetadata().getCoordinateReferenceSystem());
					logger.log(message, PiazzaLogger.WARNING);
				} catch (Exception e2) {
					logger.log("Error Augmenting JSON Doc with geolocation info", PiazzaLogger.ERROR);
				}
			}

			// repository.save(drc);
			template.index(DATAINDEX, DATATYPE, drc);
			return new DataResourceResponse(dr);

		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc indexing in Elasticsearch from SearchMetadataIngestJob: %s",
					exception.getMessage());
			logger.log(message, PiazzaLogger.ERROR);
			throw new Exception(message);
		}

	}

	/*
	 * endpoint ingesting Service object
	 * 
	 * @return Service object ingested
	 */
	@RequestMapping(value = API_ROOT + "/servicenew", method = RequestMethod.POST, consumes = "application/json")
	public ServiceResponse ingestServiceDoc(@RequestBody(required = true) Service objService) throws Exception {

		/*
		 * Block for debug purposes if needed // get reconstituted JSON Doc out of job object parameter
		 */
		String reconJSONdoc;
		try {
			ObjectMapper mapper = new ObjectMapper();
			reconJSONdoc = mapper.writeValueAsString(objService);
			System.out.println("The Re-Constituted JSON Doc:\n");
			System.out.println(reconJSONdoc);
		} catch (Exception exception) {
			String message = String.format("Error Reconstituting JSON Doc from Service obj: %s", exception.getMessage());
			logger.log(message, PiazzaLogger.ERROR);
			throw new Exception(message);
		}

		try {
			ServiceContainer sc = new ServiceContainer(objService);
			// servicerepository.save(sc);
			template.index(SERVICESINDEX, SERVICESTYPE, sc);
			return new ServiceResponse(objService);

		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc indexing in Elasticsearch from Service object: %s",
					exception.getMessage());
			logger.log(message, PiazzaLogger.ERROR);
			throw new Exception(message);
		}

	}

	/**
	 * Endpoint for deleting service metadata from elastic search.
	 * 
	 * @param Service
	 * @return PiazzaResponse ErrorResponse or ServiceResponse returned
	 */
	@RequestMapping(value = API_ROOT + "/servicedeleteid", method = RequestMethod.POST, consumes = "application/json")
	public PiazzaResponse deleteServiceDocById(@RequestBody(required = true) Service objService) throws Exception {
		try {
			ServiceContainer serviceContainer = template.findOne(SERVICESINDEX, SERVICESTYPE, objService.getServiceId(),
					ServiceContainer.class);
			if (serviceContainer == null) {
				return new ErrorResponse("Unable to find service in elastic search.", "ElasticSearch");
			} else {
				template.delete(SERVICESINDEX, SERVICESTYPE, serviceContainer);
				return new SuccessResponse(String.format("Deleted service %s from elastic search", objService.getServiceId()),
						"ElasticSearch");
			}
		} catch (Exception exception) {
			String message = String.format("Error deleting in Elasticsearch from Service object: %s", exception.getMessage());
			logger.log(message, PiazzaLogger.ERROR);
			throw new Exception(message);
		}
	}

	/*
	 * endpoint ingesting Service object 5/21 currently only using serviceId as criterion for doc search/identification
	 * logic- delete identified doc; index input param as new
	 * 
	 * @param Service object
	 * 
	 * @return success/fail
	 */
	@RequestMapping(value = API_ROOT + "/serviceupdateid", method = RequestMethod.POST, consumes = "application/json")
	public Boolean updateServiceDocById(@RequestBody(required = true) Service objService) throws Exception {

		try {
			ServiceContainer sc = template.findOne(SERVICESINDEX, SERVICESTYPE, objService.getServiceId(),
					new ServiceContainer().getClass());

			String reconJSONdoc;
			try {
				ObjectMapper mapper = new ObjectMapper();
				reconJSONdoc = mapper.writeValueAsString(sc);
				System.out.println("The Re-Constituted JSON Doc found in ES:\n");
				System.out.println(reconJSONdoc);
			} catch (Exception exception) {
				String message = String.format("Error Reconstituting JSON Doc from Service obj: %s", exception.getMessage());
				logger.log(message, PiazzaLogger.ERROR);
				throw new Exception(message);
			}

			if (sc == null) {
				String message = String.format("Unable to locate JSON Doc: %s", reconJSONdoc);
				System.out.println(message);
				logger.log(message, PiazzaLogger.ERROR);
				return false;
			} else {
				if (!template.delete(SERVICESINDEX, SERVICESTYPE, sc)) {
					String message = String.format("Unable to delete JSON Doc: %s", reconJSONdoc);
					logger.log(message, PiazzaLogger.ERROR);
					throw new Exception(message);
				}
				sc = new ServiceContainer(objService);
				return template.index(SERVICESINDEX, SERVICESTYPE, sc);
			}

		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc updating in Elasticsearch from Service object: %s",
					exception.getMessage());
			logger.log(message, PiazzaLogger.ERROR);
			throw new Exception(message);
		}

	}

}
