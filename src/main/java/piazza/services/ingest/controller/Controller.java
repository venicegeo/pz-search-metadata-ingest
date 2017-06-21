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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

	@Value("${elasticsearch.dataindex}")
	private String dataIndex;
	@Value("${elasticsearch.dataindexalias}")
	private String dataIndexAlias;
	
	@Value("${elasticsearch.serviceindex}")
	private String serviceIndex;
	
	
	private final String API_ROOT = "${api.basepath}";

	static final String DATATYPE = "DataResourceContainer";
	static final String SERVICESTYPE = "ServiceContainer";
	
	// CSS 1/12/17 if also-indexed geohash is desired
	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);
	private static final String JSON_DOC_ERR = "The Re-Constituted JSON Doc:\n";
	private static final String JSON_AUG_ERR = "Error Augmenting JSON Doc with geolocation info";
	private static final String SEARCH_METADATA_INGEST = "searchMetadataIngest";
	private static final String SEARCH_INGEST = "searchIngest";
	private static final String DATA_RESOURCE = "DataResource";
	private static final String ELASTIC_SEARCH = "ElasticSearch";
	private static final String SERVICE = "Service";
	
	
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
			boolean indexExists = template.indexExists(dataIndexAlias);
			LOG.debug("Metadata alias exists: {}", indexExists);
			if (!indexExists){
				template.createIndexWithMappingFromShellScript(dataIndex, dataIndexAlias, DATATYPE);
			}
		} catch (Exception exception) {
			String message = "Error considering pre-exisitence of ES index";
			logger.log(message, Severity.ERROR);
			LOG.error(message, exception);
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

			logger.log(JSON_DOC_ERR, Severity.INFORMATIONAL);
			logger.log(reconJSONdoc, Severity.INFORMATIONAL);
		} catch (Exception exception) {
			String message = String.format("Error Reconstituting JSON Doc from SearchMetadataIngestJob: %s", exception.getMessage());
			logger.log(message, Severity.ERROR);
			LOG.error(message, exception);
			throw new InvalidInputException(message);
		}

		try {
			DataResource dr;
			dr = mdingestJob.getData();
			DataResourceContainer drc = new DataResourceContainer(dr);
			if (dr.getSpatialMetadata() != null) {
				try {
					SpatialMetadata sm = dr.getSpatialMetadata().getProjectedSpatialMetadata();
					Double minX = sm.getMinX();
					Double maxX = sm.getMaxX();
					Double minY = sm.getMinY();
					Double maxY = sm.getMaxY();
					Double[] lcp = new Double[] { (maxX + minX) / 2, (maxY + minY) / 2 }; // lon, then lat!
					drc.setLocationCenterPoint(lcp);

					Coordinate NW = new Coordinate(minX, maxY);
					Coordinate SE = new Coordinate(maxX, minY);
					Geometry bboxGeometry = GeometryUtils.createBoundingBox(NW, SE);
					drc.setBoundingArea(bboxGeometry);

				} catch (Exception exception) {
					LOG.error("Error Augmenting JSON", exception);
					try { // in case test or for some other reason null metadata
							// values
						String message = String.format(
								"Error Augmenting JSON Doc with geolocation info, DataId: %s, possible null values input or unrecognized SRS: %s",
								dr.getDataId(), dr.getSpatialMetadata().getCoordinateReferenceSystem());
						logger.log(message, Severity.WARNING);
					} catch (Exception e2) {
						String message = JSON_AUG_ERR;
						LOG.error(message, e2);
						logger.log(message, Severity.ERROR);
					}
				}
			}

			template.index(dataIndexAlias, DATATYPE, drc);
			
			logger.log(
					String.format("Ingesting data into elastic search containing data/metadata resource object id %s",
							dr.getDataId()),
					Severity.INFORMATIONAL, new AuditElement(SEARCH_METADATA_INGEST, SEARCH_INGEST, dr.getDataId()));
			
			return new DataResourceResponse(dr);

		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc indexing in Elasticsearch from SearchMetadataIngestJob: %s", exception.getMessage());
			LOG.error(message, exception);
			logger.log(message, Severity.ERROR);
			logger.log(message, Severity.ERROR, new AuditElement(SEARCH_METADATA_INGEST, SEARCH_INGEST, "searchMetadataIngestJob"));
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
				logger.log(message, Severity.INFORMATIONAL);
				LOG.error(message, exception);
			} catch (Exception e2) {
				LOG.error(JSON_AUG_ERR, e2);
				logger.log(JSON_AUG_ERR, Severity.ERROR, new AuditElement(SEARCH_METADATA_INGEST, SEARCH_INGEST, DATA_RESOURCE));
			}
		}

		/*
		 * Block for debug purposes if needed // get reconstituted JSON Doc out of augmented input parameter
		 */
		String reconJSONdoc;
		try {
			ObjectMapper mapper = new ObjectMapper();
			reconJSONdoc = mapper.writeValueAsString(drc);
			logger.log(JSON_DOC_ERR, Severity.INFORMATIONAL);
			logger.log(reconJSONdoc, Severity.INFORMATIONAL);
		} catch (Exception exception) {
			String message = String.format("Error Reconstituting JSON Doc from SearchMetadataIngestJob: %s", exception.getMessage());
			LOG.error(message, exception);
			logger.log(message, Severity.ERROR, new AuditElement(SEARCH_METADATA_INGEST, SEARCH_INGEST, DATA_RESOURCE));
			throw new IOException(message);
		}

		try {
			template.index(dataIndexAlias, DATATYPE, drc);
			return drc;
		} catch (org.elasticsearch.client.transport.NoNodeAvailableException exception) {
			final String message = exception.getMessage();
			LOG.error("Error attempting index of data: {}", message, exception);
			logger.log(message, Severity.ERROR, new AuditElement(SEARCH_METADATA_INGEST, SEARCH_INGEST, DATA_RESOURCE));
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
			DataResourceContainer drc = template.findOne(dataIndexAlias, DATATYPE, dr.getDataId(),
					DataResourceContainer.class);
			if (drc == null) {
				return new ErrorResponse("Unable to find data record in elastic search.", ELASTIC_SEARCH);
			} else {
				template.delete(dataIndexAlias, DATATYPE, drc);
				return new SuccessResponse(String.format( "Deleted data record %s from elastic search", dr.getDataId() ),
						ELASTIC_SEARCH);
			}
		} catch (Exception exception) {
			String message = String.format("Error deleting in Elasticsearch from DataResource object: %s", exception.getMessage());
			logger.log(message, Severity.ERROR);
			LOG.error(message, exception);
			logger.log(message, Severity.ERROR, new AuditElement(SEARCH_METADATA_INGEST, "deleteDataResourceObject", DATA_RESOURCE));
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
			DataResourceContainer drc = template.findOne(dataIndexAlias, DATATYPE, dr.getDataId(),
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
				LOG.error(message, exception);
				logger.log(message, Severity.ERROR, new AuditElement(SEARCH_METADATA_INGEST, "ingestDataResourceObjectWithDataId", DATA_RESOURCE));
				throw new InvalidInputException(message);
			}

			if (drc == null) {

				logger.log(String.format("Unable to locate JSON Doc: %s", reconJSONdoc), Severity.ERROR);
				return false;
			} else {
				if (!template.delete(dataIndexAlias, DATATYPE, drc)) {
					String message = String.format("Unable to delete JSON Doc: %s", reconJSONdoc);
					logger.log(message, Severity.ERROR);
					throw new IOException(message);
				}
				drc = new DataResourceContainer(dr);
				return template.index(dataIndexAlias, DATATYPE, drc);
			}

		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc updating in Elasticsearch from Data object: %s",
					exception.getMessage());
			logger.log(message, Severity.ERROR);
			LOG.error(message, exception);
			logger.log(message, Severity.ERROR, new AuditElement(SEARCH_METADATA_INGEST, "ingestDataResourceObjectWithDataId", DATA_RESOURCE));
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
				new AuditElement(SEARCH_METADATA_INGEST, "searchIngestService", objService.getServiceId()));
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			reconJSONdoc = mapper.writeValueAsString(objService);
			logger.log(JSON_DOC_ERR, Severity.INFORMATIONAL);
			logger.log(reconJSONdoc, Severity.INFORMATIONAL);
		} catch (Exception exception) {
			String message = String.format("Error Reconstituting JSON Doc from Service obj: %s", exception.getMessage());
			logger.log(message, Severity.ERROR);
			LOG.error(message, exception);
			logger.log(message, Severity.ERROR, new AuditElement(SEARCH_METADATA_INGEST, "ingestServiceObject", SERVICE));
			throw new InvalidInputException(message);
		}

		try {
			ServiceContainer sc = new ServiceContainer(objService);
			template.index(serviceIndex, SERVICESTYPE, sc);

			logger.log(
					String.format("Ingested new service object into elastic search id %s", objService.getServiceId()),
					Severity.INFORMATIONAL,
					new AuditElement(SEARCH_METADATA_INGEST, "searchIngestService", objService.getServiceId()));

			return new ServiceResponse(objService);

		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc indexing in Elasticsearch from Service object: %s", exception.getMessage());
			logger.log(message, Severity.ERROR);
			LOG.error(message, exception);
			logger.log(message, Severity.ERROR, new AuditElement(SEARCH_METADATA_INGEST, "ingestServiceObject", SERVICE));
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
			ServiceContainer serviceContainer = template.findOne(serviceIndex, SERVICESTYPE, objService.getServiceId(),
					ServiceContainer.class);
			if (serviceContainer == null) {
				return new ErrorResponse("Unable to find service in elastic search.", ELASTIC_SEARCH);
			} else {
				template.delete(serviceIndex, SERVICESTYPE, serviceContainer);
				logger.log(String.format("Deleted service metadata from elastic search %s", objService.getServiceId()),
						Severity.INFORMATIONAL,
						new AuditElement(SEARCH_METADATA_INGEST, "searchDeleteServiceMetadata", objService.getServiceId()));

				return new SuccessResponse(
						String.format("Deleted service %s from elastic search", objService.getServiceId()),
						ELASTIC_SEARCH);
			}
		} catch (Exception exception) {
			String message = String.format("Error deleting in Elasticsearch from Service object: %s", exception.getMessage());
			logger.log(message, Severity.ERROR);
			LOG.error(message, exception);
			logger.log(message, Severity.ERROR, new AuditElement(SEARCH_METADATA_INGEST, "deleteServiceObject", SERVICE));
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
			ServiceContainer sc = template.findOne(serviceIndex, SERVICESTYPE, objService.getServiceId(), ServiceContainer.class);

			String reconJSONdoc;
			try {
				ObjectMapper mapper = new ObjectMapper();
				reconJSONdoc = mapper.writeValueAsString(sc);
				logger.log("The Re-Constituted JSON Doc found in ES:\n", Severity.INFORMATIONAL);
				logger.log(reconJSONdoc, Severity.INFORMATIONAL);
			} catch (Exception exception) {
				String message = String.format("Error Reconstituting JSON Doc from Service obj: %s", exception.getMessage());
				logger.log(message, Severity.ERROR);
				LOG.error(message, exception);
				logger.log(message, Severity.ERROR, new AuditElement(SEARCH_METADATA_INGEST, "ingestServiceObjectWithServiceId", SERVICE));
				throw new InvalidInputException(message);
			}

			if (sc == null) {
				logger.log(String.format("Unable to locate JSON Doc: %s", reconJSONdoc), Severity.ERROR);
				return false;
			} else {
				if (!template.delete(serviceIndex, SERVICESTYPE, sc)) {
					String message = String.format("Unable to delete JSON Doc: %s", reconJSONdoc);
					logger.log(message, Severity.ERROR);
					throw new IOException(message);
				}
				sc = new ServiceContainer(objService);
				
				logger.log(String.format("Ingesting service metadata into elastic search using only service id %s", objService.getServiceId()),
						Severity.INFORMATIONAL,
						new AuditElement(SEARCH_METADATA_INGEST, SEARCH_INGEST, objService.getServiceId()));

				return template.index(serviceIndex, SERVICESTYPE, sc);
			}

		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc updating in Elasticsearch from Service object: %s", exception.getMessage());
			logger.log(message, Severity.ERROR);
			LOG.error(message, exception);
			logger.log(message, Severity.ERROR, new AuditElement(SEARCH_METADATA_INGEST, "ingestServiceObjectWithServiceId", SERVICE));
			throw new IOException(message);
		}
	}
}