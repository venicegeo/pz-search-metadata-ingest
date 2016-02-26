package piazza.services.ingest.controller;

import model.data.DataResource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;




import piazza.services.ingest.repository.DataResourceContainer;
//import piazza.services.ingest.model.Metadata;
import piazza.services.ingest.repository.MetadataRepository;

@RestController
public class Controller {
	
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
		return ("Hello pz-search-metadata-ingest.  Let\'s have some metadata ingest for search!");
	}
	
	@RequestMapping(value="/api/v1/data", method=RequestMethod.POST, consumes="application/json")
	public @ResponseBody DataResource createEntry(@RequestBody DataResource entry){
		DataResourceContainer dr = new DataResourceContainer( entry );
		repository.save(dr);
		//repository.save(entry);
		return entry;
	}

}
