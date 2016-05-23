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