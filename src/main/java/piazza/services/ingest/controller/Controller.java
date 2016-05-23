package piazza.services.ingest.controller;

import model.data.DataResource;
import model.job.metadata.SpatialMetadata;
import model.job.type.SearchMetadataIngestJob;
import model.job.type.ServiceMetadataIngestJob;
import util.PiazzaLogger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.fasterxml.jackson.databind.util.JSONWrappedObject;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.common.geo.GeoPoint;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
//import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;



import model.response.DataResourceResponse;
import model.response.ServiceResponse;
import model.service.metadata.Service;
import piazza.commons.elasticsearch.ESModel;
import piazza.commons.elasticsearch.NativeElasticsearchTemplate;
import piazza.commons.elasticsearch.NativeElasticsearchTemplateConfiguration;
import piazza.services.ingest.repository.DataResourceContainer;
//import piazza.services.ingest.repository.MetadataRepository;
import piazza.services.ingest.repository.ServiceContainer;
//import piazza.services.ingest.repository.ServiceMetadataRepository;
//import piazza.services.ingest.model.Metadata;
//import piazza.services.ingest.repository.MetadataRepository;
//import piazza.services.ingest.repository.ServiceContainer;
//import piazza.services.ingest.repository.ServiceMetadataRepository;
import piazza.services.ingest.util.GeometryUtils;

//@ContextConfiguration(classes = NativeElasticsearchTemplateConfiguration.class)
//@TestPropertySource(properties = { "elasticsearch.clustername = venice-es",
//		"elasticsearch.hostname = 127.0.0.1", "elasticsearch.port = 9300" })

//@Component
@RestController
public class Controller {

	private final String API_ROOT = "${api.basepath}";

	static final String DATAINDEX = "pzmetadata";
	static final String DATATYPE = "DataResourceContainer";
	static final String SERVICESINDEX = "pzservices";
	static final String SERVICESTYPE = "ServiceContainer";

//	@Autowired
	private PiazzaLogger logger;

	//@Autowired
	//NativeElasticsearchTemplateConfiguration templateconfig= new NativeElasticsearchTemplateConfiguration();
	//@Autowired
	//NativeElasticsearchTemplate template = templateconfig.template(templateconfig.client(), templateconfig.mapper());
	//@Autowired
	NativeElasticsearchTemplate template;

	//@RequestMapping(value="/", method=RequestMethod.GET)
	@RequestMapping("/")
	@ResponseBody
	public String checkme(){
		return ("Hello pz-search-metadata-ingest.  Let\'s have some metadata for search!");
	}

	/**
	 * Statistics from Spring Boot
	 * 
	 * @return json as statistics
	 */
	@RequestMapping(value = "/admin/stats",  method = RequestMethod.GET)
	public void stats(HttpServletResponse response) throws IOException {
		 response.sendRedirect("/metrics");
	}

	@RequestMapping(value = API_ROOT + "/dataold", method = RequestMethod.POST, consumes="application/json")
	public @ResponseBody DataResource createEntry(@RequestBody DataResource entry)  throws Exception {
		DataResourceContainer drc = new DataResourceContainer( entry );
		try {
			template.index(DATAINDEX, DATATYPE, drc);
			return entry;
		} catch (org.elasticsearch.client.transport.NoNodeAvailableException exception) {
			String message = String.format("Error attempting index of data", exception.getMessage());
			System.out.println(message);		
			throw new Exception(message);
		}
	}
	
	@RequestMapping(value = API_ROOT + "/datanew", method = RequestMethod.POST, consumes="application/json")
	public @ResponseBody DataResourceContainer createEntryNew(@RequestBody DataResource entry)  throws Exception {
		DataResourceContainer drc = new DataResourceContainer( entry );
		try {
			SpatialMetadata sm = entry.getSpatialMetadata();
			Double minX = sm.getMinX();
			Double maxX = sm.getMaxX();
			Double minY = sm.getMinY();
			Double maxY = sm.getMaxY();
			GeoPoint gp = new GeoPoint( (maxY+minY)/2, (maxX+minX)/2 );
			drc.setLocationCenterPoint(gp);
			
			Coordinate NW = new Coordinate(minX, maxY);
			Coordinate SE = new Coordinate(maxX, minY);
			Geometry bboxGeometry = GeometryUtils.createBoundingBox( NW, SE);
			drc.setBoundingArea(bboxGeometry);
		} catch (Exception exception) {
			String message = String.format("Error augmenting with geolocation center point and bbox", exception.getMessage());
			System.out.println(message);		
		}

		/*    Block for debug purposes if needed
		// get reconstituted JSON Doc out of augmented input parameter
		*/
		String reconJSONdoc;
		try {
			ObjectMapper mapper = new ObjectMapper();
			reconJSONdoc = mapper.writeValueAsString( drc );
			System.out.println("The Re-Constituted JSON Doc:\n");
			System.out.println( reconJSONdoc );
		} catch (Exception exception) {
			String message = String.format("Error Reconstituting JSON Doc from SearchMetadataIngestJob: %s", exception.getMessage());
			logger.log(message, PiazzaLogger.ERROR);
			throw new Exception(message);
		}
		
		try {
			template.index(DATAINDEX, DATATYPE, drc);
			//repository.save(drc);
			//repository.save(entry);
			return drc;
		} catch (org.elasticsearch.client.transport.NoNodeAvailableException exception) {
			String message = String.format("Error attempting index of data", exception.getMessage());
			System.out.println(message);		
			throw new Exception(message);
		}
	}
	
	/* 
	 * endpoint ingesting SearchMetadataIngestJob containing data/metadata resource object
	 * @return dataResource object ingested
	 */
	@RequestMapping(value = API_ROOT + "/data", method = RequestMethod.POST, consumes = "application/json")
	public DataResourceResponse ingestMetadataJob(@RequestBody(required = true) SearchMetadataIngestJob mdingestJob)  throws Exception {
		
		/*    Block for debug purposes if needed
		// get reconstituted JSON Doc out of job object parameter
		*/
		String reconJSONdoc;
		try {
			ObjectMapper mapper = new ObjectMapper();
			reconJSONdoc = mapper.writeValueAsString( mdingestJob.getData() );
			System.out.println("The Re-Constituted JSON Doc:\n");
			System.out.println( reconJSONdoc );
		} catch (Exception exception) {
			String message = String.format("Error Reconstituting JSON Doc from SearchMetadataIngestJob: %s", exception.getMessage());
			logger.log(message, PiazzaLogger.ERROR);
			throw new Exception(message);
		}
		
		try {
			DataResource dr;
			dr = mdingestJob.getData();
			DataResourceContainer drc = new DataResourceContainer( dr );
			try {
				SpatialMetadata sm = dr.getSpatialMetadata();
				Double minX = sm.getMinX();
				Double maxX = sm.getMaxX();
				Double minY = sm.getMinY();
				Double maxY = sm.getMaxY();
				GeoPoint gp = new GeoPoint( (maxY+minY)/2, (maxX+minX)/2 );
				drc.setLocationCenterPoint(gp);
				
				Coordinate NW = new Coordinate(minX, maxY);
				Coordinate SE = new Coordinate(maxX, minY);
				Geometry bboxGeometry = GeometryUtils.createBoundingBox( NW, SE);
				drc.setBoundingArea(bboxGeometry);
				
			} catch (Exception exception) {
				String message = String.format("Error Augmenting JSON Doc with geolocation info, perhaps null values input: %s", exception.getMessage());
				logger.log(message, PiazzaLogger.ERROR);
			}
			
			//repository.save(drc);
			template.index(DATAINDEX, DATATYPE, drc);
			return new DataResourceResponse( dr );
			
		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc indexing in Elasticsearch from SearchMetadataIngestJob: %s", exception.getMessage());
			logger.log(message, PiazzaLogger.ERROR);
			throw new Exception(message);
		}
		
	}
	
	/* 
	 * endpoint ingesting ServiceMetadataIngestJob containing Service object
	 * @return Service object ingested
	 */
	@RequestMapping(value = API_ROOT + "/service", method = RequestMethod.POST, consumes = "application/json")
	public ServiceResponse ingestServiceMetadataJob(@RequestBody(required = true) ServiceMetadataIngestJob smdingestJob)  throws Exception {
		
		/*    Block for debug purposes if needed
		// get reconstituted JSON Doc out of job object parameter
		*/
		String reconJSONdoc;
		try {
			ObjectMapper mapper = new ObjectMapper();
			reconJSONdoc = mapper.writeValueAsString( smdingestJob.getData() );
			System.out.println("The Re-Constituted JSON Doc:\n");
			System.out.println( reconJSONdoc );
		} catch (Exception exception) {
			String message = String.format("Error Reconstituting JSON Doc from ServiceMetadataIngestJob: %s", exception.getMessage());
			logger.log(message, PiazzaLogger.ERROR);
			throw new Exception(message);
		}
		
		try {
			Service objService;
			objService = smdingestJob.getData();
			ServiceContainer sc = new ServiceContainer( objService );
			//servicerepository.save(sc);
			template.index(SERVICESINDEX, SERVICESTYPE, sc);
			return new ServiceResponse( objService );
			
		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc indexing in Elasticsearch from ServiceMetadataIngestJob: %s", exception.getMessage());
			logger.log(message, PiazzaLogger.ERROR);
			throw new Exception(message);
		}
		
	}
	/* 
	 * endpoint ingesting Service object
	 * @return Service object ingested
	 */
	@RequestMapping(value = API_ROOT + "/servicenew", method = RequestMethod.POST, consumes = "application/json")
	public ServiceResponse ingestServiceDoc(@RequestBody(required = true) Service objService)  throws Exception {
		
		/*    Block for debug purposes if needed
		// get reconstituted JSON Doc out of job object parameter
		*/
		String reconJSONdoc;
		try {
			ObjectMapper mapper = new ObjectMapper();
			reconJSONdoc = mapper.writeValueAsString( objService );
			System.out.println("The Re-Constituted JSON Doc:\n");
			System.out.println( reconJSONdoc );
		} catch (Exception exception) {
			String message = String.format("Error Reconstituting JSON Doc from Service obj: %s", exception.getMessage());
			logger.log(message, PiazzaLogger.ERROR);
			throw new Exception(message);
		}
		
		try {
			ServiceContainer sc = new ServiceContainer( objService );
			//servicerepository.save(sc);
			template.index(SERVICESINDEX, SERVICESTYPE, sc);
			return new ServiceResponse( objService );
			
		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc indexing in Elasticsearch from ServiceMetadataIngestJob: %s", exception.getMessage());
			logger.log(message, PiazzaLogger.ERROR);
			throw new Exception(message);
		}
		
	}

	/* 
	 * endpoint ingesting Service object
	 * 5/21 currently only using serviceId as criterion for doc search/identification
	 * @param Service object
	 * @return success/fail
	 */
	@RequestMapping(value = API_ROOT + "/servicedeleteid", method = RequestMethod.POST, consumes = "application/json")
	public Boolean deleteServiceDocById(@RequestBody(required = true) Service objService)  throws Exception {
		
		
		try {
			//ObjectMapper mapper = new ObjectMapper();
			//Object obj = mapper.readValue(serviceIdJSON, Object.class);
			//ServiceContainer sc0 = new ServiceContainer();
			ServiceContainer sc = template.findOne(SERVICESINDEX, 
					SERVICESTYPE, objService.getServiceId(), new ServiceContainer().getClass());
			//ServiceContainer sc = new ServiceContainer( objService );
			//servicerepository.save(sc);
//			template.index(SERVICESINDEX, SERVICESTYPE, sc);

			String reconJSONdoc;
			try {
				ObjectMapper mapper = new ObjectMapper();
				reconJSONdoc = mapper.writeValueAsString( sc );
				System.out.println("The Re-Constituted JSON Doc:\n");
				System.out.println( reconJSONdoc );
			} catch (Exception exception) {
				String message = String.format("Error Reconstituting JSON Doc from Service obj: %s", exception.getMessage());
				logger.log(message, PiazzaLogger.ERROR);
				throw new Exception(message);
			}
			
			if (sc == null) return false; 
			else {
				return template.delete(SERVICESINDEX, SERVICESTYPE, sc);
			}
			
		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc deleting in Elasticsearch from Service object: %s", exception.getMessage());
			logger.log(message, PiazzaLogger.ERROR);
			throw new Exception(message);
		}
		
	}

	/* 
	 * endpoint ingesting Service object
	 * 5/21 currently only using serviceId as criterion for doc search/identification
	 * logic- delete identified doc; index input param as new
	 * @param Service object
	 * @return success/fail
	 */
	@RequestMapping(value = API_ROOT + "/serviceupdateid", method = RequestMethod.POST, consumes = "application/json")
	public Boolean updateServiceDocById(@RequestBody(required = true) Service objService)  throws Exception {
		
		
		try {
			//ObjectMapper mapper = new ObjectMapper();
			//Object obj = mapper.readValue(serviceIdJSON, Object.class);
			//ServiceContainer sc0 = new ServiceContainer();
			ServiceContainer sc = template.findOne(SERVICESINDEX, 
					SERVICESTYPE, objService.getServiceId(), new ServiceContainer().getClass());
			//ServiceContainer sc = new ServiceContainer( objService );
			//servicerepository.save(sc);
//			template.index(SERVICESINDEX, SERVICESTYPE, sc);

			String reconJSONdoc;
			try {
				ObjectMapper mapper = new ObjectMapper();
				reconJSONdoc = mapper.writeValueAsString( sc );
				System.out.println("The Re-Constituted JSON Doc found in ES:\n");
				System.out.println( reconJSONdoc );
			} catch (Exception exception) {
				String message = String.format("Error Reconstituting JSON Doc from Service obj: %s", exception.getMessage());
				logger.log(message, PiazzaLogger.ERROR);
				throw new Exception(message);
			}
			
			if (sc == null) {
				String message = String.format("Unable to locate JSON Doc: %s", reconJSONdoc);
				System.out.println( message );
				logger.log(message, PiazzaLogger.ERROR);
				return false; 
			}
			else {
				if( !template.delete(SERVICESINDEX, SERVICESTYPE, sc) ) {
					String message = String.format("Unable to delete JSON Doc: %s", reconJSONdoc);
					logger.log(message, PiazzaLogger.ERROR);
					throw new Exception(message);
				}
				sc = new ServiceContainer( objService );
				return template.index(SERVICESINDEX, SERVICESTYPE, sc);
			}
			
		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc updating in Elasticsearch from Service object: %s", exception.getMessage());
			logger.log(message, PiazzaLogger.ERROR);
			throw new Exception(message);
		}
		
	}

}
