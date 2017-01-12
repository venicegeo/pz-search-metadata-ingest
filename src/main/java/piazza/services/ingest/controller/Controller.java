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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import exception.InvalidInputException;
import model.data.DataResource;
import model.job.metadata.SpatialMetadata;
import model.job.type.SearchMetadataIngestJob;
import model.logger.AuditElement;
import model.logger.Severity;
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
	// CSS 1/12/17 if also-indexed geohash is desired
	//static final String mappingJSON = "{ \"DataResourceContainer\": { \"properties\" : { \"locationCenterPoint\": { \"type\": \"geo_point\", \"geohash\": \"true\" }, \"boundingArea\": { \"type\": \"geo_shape\" } } } }";
	static final String mappingJSON = "{ \"DataResourceContainer\": { \"properties\" : { \"locationCenterPoint\": { \"type\": \"geo_point\" }, \"boundingArea\": { \"type\": \"geo_shape\" } } } }";
	private final static Logger LOGGER = LoggerFactory.getLogger(Controller.class);

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

	public void init() throws IOException {
		try {
			String mapping = mappingJSON;
			if (!template.indexExists(DATAINDEX))
				template.createIndexWithMapping(DATAINDEX, DATATYPE, mapping);
		} catch (Exception exception) {
			String message = "Error considering pre-exisitence of ES index";
			logger.log(message, Severity.ERROR);
			LOGGER.error(message, exception);
			throw new IOException(message);
		}
	}

	/**
	 * Endpoint ingesting SearchMetadataIngestJob containing data/metadata resource object
	 * 
	 * @return dataResource object ingested
	 */
	@RequestMapping(value = API_ROOT + "/data", method = RequestMethod.POST, consumes = "application/json")
	public DataResourceResponse ingestMetadataJob(@RequestBody(required = true) SearchMetadataIngestJob mdingestJob) throws InvalidInputException, IOException {

		/*
		 * Block for debug purposes if needed // get reconstituted JSON Doc out of job object parameter
		 */
		String reconJSONdoc;
		try {
			ObjectMapper mapper = new ObjectMapper();
			reconJSONdoc = mapper.writeValueAsString(mdingestJob.getData());
			//System.out.println(reconJSONdoc);
			logger.log("The Re-Constituted JSON Doc:\n", Severity.INFORMATIONAL);
			logger.log(reconJSONdoc, Severity.INFORMATIONAL);
		} catch (Exception exception) {
			String message = String.format("Error Reconstituting JSON Doc from SearchMetadataIngestJob: %s", exception.getMessage());
			//System.out.println(message);
			logger.log(message, Severity.ERROR);
			LOGGER.error(message, exception);
			throw new InvalidInputException(message);
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
				//GeoPoint gp = new GeoPoint((maxY + minY) / 2, (maxX + minX) / 2);
				//drc.setLocationCenterPoint(gp);
				Double[] lcp = new Double[]{ (maxX + minX) / 2, (maxY + minY) / 2 };  //lon then lat!
				drc.setLocationCenterPoint(lcp);

				Coordinate NW = new Coordinate(minX, maxY);
				Coordinate SE = new Coordinate(maxX, minY);
				Geometry bboxGeometry = GeometryUtils.createBoundingBox(NW, SE);
				drc.setBoundingArea(bboxGeometry);

			} catch (Exception exception) {
				LOGGER.error("Error Augmenting JSON", exception);
				try { // in case test or for some other reason null metadata values
					String message = String.format(
							"Error Augmenting JSON Doc with geolocation info, DataId: %s, possible null values input or unrecognized SRS: %s",
							dr.getDataId(), dr.getSpatialMetadata().getCoordinateReferenceSystem());
					logger.log(message, Severity.WARNING);
				} catch (Exception e2) {
					String message = "Error Augmenting JSON Doc with geolocation info";
					LOGGER.error(message, e2);
					logger.log(message, Severity.ERROR);
				}
			}

			// repository.save(drc);
			template.index(DATAINDEX, DATATYPE, drc);
			
			logger.log(
					String.format("Ingesting data into elastic search containing data/metadata resource object id %s",
							dr.getDataId()),
					Severity.INFORMATIONAL, new AuditElement("searchMetadataIngest", "searchIngest", dr.getDataId()));
			
			return new DataResourceResponse(dr);

		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc indexing in Elasticsearch from SearchMetadataIngestJob: %s", exception.getMessage());
			LOGGER.error(message, exception);
			logger.log(message, Severity.ERROR);
			logger.log(message, Severity.ERROR, new AuditElement("searchMetadataIngest", "searchIngest", "searchMetadataIngestJob"));
			throw new IOException(message);
		}

	}

	/**
	 * For debug if need to excercise WS with metadata payload rather than Piazza job.
	 * Comment out for code coverage
	 * May switch to this as payload endpoint, away from 'job' paradigm, then delete of comment out 'data' endpoint above
	 * 
	 * endpoint ingesting DataResource object 
	 * 
	 * @return dataResource object ingested
	 */
	@RequestMapping(value = API_ROOT + "/datanew", method = RequestMethod.POST, consumes = "application/json")
	public @ResponseBody DataResourceContainer createEntryNew(@RequestBody DataResource entry) throws IOException {
		DataResourceContainer drc = new DataResourceContainer(entry);
		try {
			SpatialMetadata sm = entry.getSpatialMetadata().getProjectedSpatialMetadata();
			Double minX = sm.getMinX();
			Double maxX = sm.getMaxX();
			Double minY = sm.getMinY();
			Double maxY = sm.getMaxY();
			//GeoPoint gp = new GeoPoint((maxY + minY) / 2, (maxX + minX) / 2);
			//drc.setLocationCenterPoint(gp);
			Double[] lcp = new Double[]{ (maxX + minX) / 2, (maxY + minY) / 2 };  //lon then lat!
			drc.setLocationCenterPoint(lcp);

			Coordinate NW = new Coordinate(minX, maxY);
			Coordinate SE = new Coordinate(maxX, minY);
			Geometry bboxGeometry = GeometryUtils.createBoundingBox(NW, SE);
			drc.setBoundingArea(bboxGeometry);
		} catch (Exception exception) {
			try{  // in case test or for some other reason null metadata values
				String message = String.format("Error Augmenting JSON Doc with geolocation info, DataId: %s, possible null values input or unrecognized SRS: %s",
						entry.getDataId(), entry.getSpatialMetadata().getCoordinateReferenceSystem());
				//System.out.println(message);
				logger.log(message, Severity.INFORMATIONAL);
				LOGGER.error(message, exception);
			} catch (Exception e2) {
				//System.out.println(e2.getMessage());
				LOGGER.error("Error Augmenting JSON Doc with geolocation info", e2);
				logger.log("Error Augmenting JSON Doc with geolocation info", Severity.ERROR, new AuditElement("searchMetadataIngest", "searchIngest", "DataResource"));
			}
		}

		/*
		 * Block for debug purposes if needed // get reconstituted JSON Doc out of augmented input parameter
		 */
		String reconJSONdoc;
		try {
			ObjectMapper mapper = new ObjectMapper();
			reconJSONdoc = mapper.writeValueAsString(drc);
			//System.out.println(reconJSONdoc);
			logger.log("The Re-Constituted JSON Doc:\n", Severity.INFORMATIONAL);
			logger.log(reconJSONdoc, Severity.INFORMATIONAL);
		} catch (Exception exception) {
			String message = String.format("Error Reconstituting JSON Doc from SearchMetadataIngestJob: %s", exception.getMessage());
			//System.out.println(message);
			LOGGER.error(message, exception);
			logger.log(message, Severity.ERROR, new AuditElement("searchMetadataIngest", "searchIngest", "DataResource"));
			throw new IOException(message);
		}

		try {
			template.index(DATAINDEX, DATATYPE, drc);
			return drc;
		} catch (org.elasticsearch.client.transport.NoNodeAvailableException exception) {
			String message = String.format("Error attempting index of data", exception.getMessage());
			//System.out.println(message);
			LOGGER.error(message, exception);
			logger.log(message, Severity.ERROR, new AuditElement("searchMetadataIngest", "searchIngest", "DataResource"));
			throw new IOException(message);
		}
	}
	
	/**
	 * Endpoint for deleting data metadata record from elastic search.
	 * 
	 * @param DataResource object
	 * @return PiazzaResponse ErrorResponse or ServiceResponse returned
	 */
	@RequestMapping(value = API_ROOT + "/datadeleteid", method = RequestMethod.POST, consumes = "application/json")
	public PiazzaResponse deleteDataDocById(@RequestBody(required = true) DataResource dr) throws IOException {
		try {
			DataResourceContainer drc = template.findOne(DATAINDEX, DATATYPE, dr.getDataId(),
					DataResourceContainer.class);
			if (drc == null) {
				return new ErrorResponse("Unable to find data record in elastic search.", "ElasticSearch");
			} else {
				template.delete(DATAINDEX, DATATYPE, drc);
				return new SuccessResponse(String.format( "Deleted data record %s from elastic search", dr.getDataId() ),
						"ElasticSearch");
			}
		} catch (Exception exception) {
			String message = String.format("Error deleting in Elasticsearch from DataResource object: %s", exception.getMessage());
			logger.log(message, Severity.ERROR);
			LOGGER.error(message, exception);
			logger.log(message, Severity.ERROR, new AuditElement("searchMetadataIngest", "deleteDataResourceObject", "DataResource"));
			throw new IOException(message);
		}
	}

	/**
	 * endpoint ingesting DataResource object using DataId as criterion for doc search/identification
	 * logic- delete identified doc; index input param as new
	 * 
	 * @param Service object
	 * 
	 * @return success/fail
	 */
	@RequestMapping(value = API_ROOT + "/dataupdateid", method = RequestMethod.POST, consumes = "application/json")
	public Boolean updateDataDocById(@RequestBody(required = true) DataResource dr) throws InvalidInputException, IOException {

		try {
			DataResourceContainer drc = template.findOne(DATAINDEX, DATATYPE, dr.getDataId(),
					DataResourceContainer.class);
			String reconJSONdoc;
			try {
				ObjectMapper mapper = new ObjectMapper();
				reconJSONdoc = mapper.writeValueAsString(drc);
				logger.log("The Re-Constituted JSON Doc found in ES:\n", Severity.INFORMATIONAL);
				logger.log(reconJSONdoc, Severity.INFORMATIONAL);
			} catch (Exception exception) {
				String message = String.format("Error Reconstituting JSON Doc from Data obj: %s", exception.getMessage());
				logger.log(message, Severity.ERROR);
				LOGGER.error(message, exception);
				logger.log(message, Severity.ERROR, new AuditElement("searchMetadataIngest", "ingestDataResourceObjectWithDataId", "DataResource"));
				throw new InvalidInputException(message);
			}

			if (drc == null) {

				logger.log(String.format("Unable to locate JSON Doc: %s", reconJSONdoc), Severity.ERROR);
				return false;
			} else {
				if (!template.delete(DATAINDEX, DATATYPE, drc)) {
					String message = String.format("Unable to delete JSON Doc: %s", reconJSONdoc);
					logger.log(message, Severity.ERROR);
					throw new IOException(message);
				}
				drc = new DataResourceContainer(dr);
				return template.index(DATAINDEX, DATATYPE, drc);
			}

		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc updating in Elasticsearch from Data object: %s",
					exception.getMessage());
			logger.log(message, Severity.ERROR);
			LOGGER.error(message, exception);
			logger.log(message, Severity.ERROR, new AuditElement("searchMetadataIngest", "ingestDataResourceObjectWithDataId", "DataResource"));
			throw new IOException(message);
		}
	}
	
	/**
	 * Endpoint ingesting Service object
	 * 
	 * @return Service object ingested
	 */
	@RequestMapping(value = API_ROOT + "/servicenew", method = RequestMethod.POST, consumes = "application/json")
	public ServiceResponse ingestServiceDoc(@RequestBody(required = true) Service objService) throws InvalidInputException, IOException {

		/*
		 * Block for debug purposes if needed // get reconstituted JSON Doc out of job object parameter
		 */
		String reconJSONdoc;
		logger.log(
				String.format("Trying to new service object into elastic search id %s", objService.getServiceId()),
				Severity.INFORMATIONAL,
				new AuditElement("searchMetadataIngest", "searchIngestService", objService.getServiceId()));
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			reconJSONdoc = mapper.writeValueAsString(objService);
			logger.log("The Re-Constituted JSON Doc:\n", Severity.INFORMATIONAL);
			logger.log(reconJSONdoc, Severity.INFORMATIONAL);
		} catch (Exception exception) {
			String message = String.format("Error Reconstituting JSON Doc from Service obj: %s", exception.getMessage());
			logger.log(message, Severity.ERROR);
			LOGGER.error(message, exception);
			logger.log(message, Severity.ERROR, new AuditElement("searchMetadataIngest", "ingestServiceObject", "Service"));
			throw new InvalidInputException(message);
		}

		try {
			ServiceContainer sc = new ServiceContainer(objService);
			// servicerepository.save(sc);
			template.index(SERVICESINDEX, SERVICESTYPE, sc);

			logger.log(
					String.format("Ingested new service object into elastic search id %s", objService.getServiceId()),
					Severity.INFORMATIONAL,
					new AuditElement("searchMetadataIngest", "searchIngestService", objService.getServiceId()));

			return new ServiceResponse(objService);

		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc indexing in Elasticsearch from Service object: %s", exception.getMessage());
			logger.log(message, Severity.ERROR);
			LOGGER.error(message, exception);
			logger.log(message, Severity.ERROR, new AuditElement("searchMetadataIngest", "ingestServiceObject", "Service"));
			throw new IOException(message);
		}

	}

	/**
	 * Endpoint for deleting service metadata from elastic search.
	 * 
	 * @param Service
	 * @return PiazzaResponse ErrorResponse or ServiceResponse returned
	 */
	@RequestMapping(value = API_ROOT + "/servicedeleteid", method = RequestMethod.POST, consumes = "application/json")
	public PiazzaResponse deleteServiceDocById(@RequestBody(required = true) Service objService) throws IOException {
		try {
			ServiceContainer serviceContainer = template.findOne(SERVICESINDEX, SERVICESTYPE, objService.getServiceId(),
					ServiceContainer.class);
			if (serviceContainer == null) {
				return new ErrorResponse("Unable to find service in elastic search.", "ElasticSearch");
			} else {
				template.delete(SERVICESINDEX, SERVICESTYPE, serviceContainer);
				logger.log(String.format("Deleted service metadata from elastic search %s", objService.getServiceId()),
						Severity.INFORMATIONAL,
						new AuditElement("searchMetadataIngest", "searchDeleteServiceMetadata", objService.getServiceId()));

				return new SuccessResponse(
						String.format("Deleted service %s from elastic search", objService.getServiceId()),
						"ElasticSearch");
			}
		} catch (Exception exception) {
			String message = String.format("Error deleting in Elasticsearch from Service object: %s", exception.getMessage());
			logger.log(message, Severity.ERROR);
			LOGGER.error(message, exception);
			logger.log(message, Severity.ERROR, new AuditElement("searchMetadataIngest", "deleteServiceObject", "Service"));
			throw new IOException(message);
		}
	}

	/**
	 * endpoint ingesting Service object 5/21 currently only using serviceId as criterion for doc search/identification
	 * logic- delete identified doc; index input param as new
	 * 
	 * @param Service object
	 * 
	 * @return success/fail
	 */
	@RequestMapping(value = API_ROOT + "/serviceupdateid", method = RequestMethod.POST, consumes = "application/json")
	public Boolean updateServiceDocById(@RequestBody(required = true) Service objService) throws InvalidInputException, IOException {

		try {
			ServiceContainer sc = template.findOne(SERVICESINDEX, SERVICESTYPE, objService.getServiceId(),
					new ServiceContainer().getClass());

			String reconJSONdoc;
			try {
				ObjectMapper mapper = new ObjectMapper();
				reconJSONdoc = mapper.writeValueAsString(sc);
				logger.log("The Re-Constituted JSON Doc found in ES:\n", Severity.INFORMATIONAL);
				logger.log(reconJSONdoc, Severity.INFORMATIONAL);
			} catch (Exception exception) {
				String message = String.format("Error Reconstituting JSON Doc from Service obj: %s", exception.getMessage());
				logger.log(message, Severity.ERROR);
				LOGGER.error(message, exception);
				logger.log(message, Severity.ERROR, new AuditElement("searchMetadataIngest", "ingestServiceObjectWithServiceId", "Service"));
				throw new InvalidInputException(message);
			}

			if (sc == null) {
				logger.log(String.format("Unable to locate JSON Doc: %s", reconJSONdoc), Severity.ERROR);
				return false;
			} else {
				if (!template.delete(SERVICESINDEX, SERVICESTYPE, sc)) {
					String message = String.format("Unable to delete JSON Doc: %s", reconJSONdoc);
					logger.log(message, Severity.ERROR);
					throw new IOException(message);
				}
				sc = new ServiceContainer(objService);
				
				logger.log(String.format("Ingesting service metadata into elastic search using only service id %s", objService.getServiceId()),
						Severity.INFORMATIONAL,
						new AuditElement("searchMetadataIngest", "searchIngest", objService.getServiceId()));

				return template.index(SERVICESINDEX, SERVICESTYPE, sc);
			}

		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc updating in Elasticsearch from Service object: %s", exception.getMessage());
			logger.log(message, Severity.ERROR);
			LOGGER.error(message, exception);
			logger.log(message, Severity.ERROR, new AuditElement("searchMetadataIngest", "ingestServiceObjectWithServiceId", "Service"));
			throw new IOException(message);
		}
	}
}