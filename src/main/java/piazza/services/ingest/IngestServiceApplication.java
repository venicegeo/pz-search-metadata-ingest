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
package piazza.services.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import piazza.services.ingest.controller.Controller;

@SpringBootApplication
@ComponentScan({"piazza, util"})
public class IngestServiceApplication {
	private static final Logger LOGGER = LoggerFactory.getLogger(IngestServiceApplication.class);

	@Autowired
	private Controller cont;

	public static void main(String[] args) {

		ConfigurableApplicationContext context = SpringApplication.run(IngestServiceApplication.class, args); //NOSONAR

		try {
			context.getBean(Controller.class).init();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}
}
