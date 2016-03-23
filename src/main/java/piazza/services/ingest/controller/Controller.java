package piazza.services.ingest.controller;

import model.data.DataResource;
import model.job.type.SearchMetadataIngestJob;
import util.PiazzaLogger;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;



import model.response.DataResourceResponse;
import piazza.services.ingest.repository.DataResourceContainer;
//import piazza.services.ingest.model.Metadata;
import piazza.services.ingest.repository.MetadataRepository;

@RestController
public class Controller {
	
	private PiazzaLogger logger= new PiazzaLogger();
	private final String API_ROOT = "${api.basepath}";

	@Autowired
	MetadataRepository repository;
/*
	@RequestMapping(value="/metadata_v1/ingest", method=RequestMethod.POST, consumes="application/json")
	public @ResponseBody Metadata createEntry(@RequestBody Metadata entry){
		repository.save(entry);
		return entry;
	}
*/
	@RequestMapping(value="/", method=RequestMethod.GET)
	public String checkme(){
		return ("Hello pz-search-metadata-ingest.  Let\'s have some metadata for search!");
	}
	
	@RequestMapping(value = API_ROOT + "/dataold", method = RequestMethod.POST, consumes="application/json")
	public @ResponseBody DataResource createEntry(@RequestBody DataResource entry){
		DataResourceContainer dr = new DataResourceContainer( entry );
		repository.save(dr);
		//repository.save(entry);
		return entry;
	}
	
	/* 
	 * endpoint ingesting SearchMetadataIngestJob containing data/metadata resource object
	 * @return dataResource object ingested
	 */
	@SuppressWarnings("unchecked")
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
		
		DataResource dr;
		try {
			dr = mdingestJob.getData();
			DataResourceContainer drc = new DataResourceContainer( dr );
			repository.save(drc);
			
		} catch (Exception exception) {
			String message = String.format("Error completing JSON Doc indexing in Elasticsearch from SearchMetadataIngestJob: %s", exception.getMessage());
			logger.log(message, PiazzaLogger.ERROR);
			throw new Exception(message);
		}
		return new DataResourceResponse( dr );
	}
	
	

}
