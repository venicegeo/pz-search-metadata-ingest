package piazza.services.ingest.util;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.vividsolutions.jts.geom.Geometry;

public class GeoJsonModule extends SimpleModule
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public GeoJsonModule() {
        super("GeoJsonModule");
        this.addSerializer(Geometry.class, new GeoJsonSerializer());
        this.addDeserializer(Geometry.class, new GeoJsonDeserializer());
    }
    
}