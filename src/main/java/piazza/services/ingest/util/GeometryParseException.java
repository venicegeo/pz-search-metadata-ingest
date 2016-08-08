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
package piazza.services.ingest.util;

import org.springframework.beans.factory.annotation.Autowired;

import util.PiazzaLogger;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class GeometryParseException extends Exception {
	private static final long serialVersionUID = 1L;
	
	@Autowired
	private PiazzaLogger logger;
	//private final Logger log = LoggerFactory.getLogger(this.getClass());

	public GeometryParseException() {
		super();
	}

	public GeometryParseException(String message, Throwable cause) {
		super(message, cause);
		//log.error(message, cause);
		String message1 = String.format("%s --- %s", message, cause.getMessage());
		logger.log(message1, PiazzaLogger.ERROR);
	}

	public GeometryParseException(String message) {
		super(message);
		//log.error(message);
		logger.log(message, PiazzaLogger.ERROR);
	}

	public GeometryParseException(Throwable cause) {
		super(cause);
		//log.error(cause.getMessage(), cause);
		logger.log(cause.getMessage(), PiazzaLogger.ERROR);
	}
}