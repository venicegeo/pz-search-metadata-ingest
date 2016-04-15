package piazza.services.ingest.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeometryParseException extends Exception {
	private static final long serialVersionUID = 1L;
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public GeometryParseException() {
		super();
	}

	public GeometryParseException(String message, Throwable cause) {
		super(message, cause);
		log.error(message, cause);
	}

	public GeometryParseException(String message) {
		super(message);
		log.error(message);
	}

	public GeometryParseException(Throwable cause) {
		super(cause);
		log.error(cause.getMessage(), cause);
	}
}