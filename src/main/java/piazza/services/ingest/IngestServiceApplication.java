package piazza.services.ingest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfiguration;

import piazza.commons.elasticsearch.NativeElasticsearchTemplateConfiguration;

//import piazza.commons.elasticsearch.NativeElasticsearchTemplateConfiguration;


//@Configuration
//@EnableAutoConfiguration 
//@ContextConfiguration(classes = NativeElasticsearchTemplateConfiguration.class)
@SpringBootApplication
@ComponentScan({"piazza, util"})
public class IngestServiceApplication {

	public static void main(String[] args) {

		SpringApplication.run(IngestServiceApplication.class, args);
	}
}
