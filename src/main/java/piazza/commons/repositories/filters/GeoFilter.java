package piazza.commons.repositories.filters;

import java.util.List;

import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.common.geo.builders.LineStringBuilder;
import org.elasticsearch.common.geo.builders.PointBuilder;
import org.elasticsearch.common.geo.builders.PolygonBuilder;
import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.OrFilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;

public class GeoFilter {
	private final static Logger log = LoggerFactory.getLogger(GeoFilter.class);
	
	public static FilterBuilder getGeoShapeFilterBuilder(String fieldName,
			Geometry geometry ) {
		if(geometry.isRectangle() || 
				geometry.getGeometryType().equalsIgnoreCase("POLYGON") ||
				geometry.getGeometryType().equalsIgnoreCase("CIRCLE") ||
				geometry.getGeometryType().equalsIgnoreCase("BOX") ||
				geometry.getGeometryType().equalsIgnoreCase("RECTANGLE")) {
			
			PolygonBuilder sb = ShapeBuilder.newPolygon().points(geometry.getCoordinates());
			return FilterBuilders.geoShapeFilter(fieldName, sb, ShapeRelation.INTERSECTS);
		}
		if(geometry.getGeometryType().equalsIgnoreCase("POINT")){
			PointBuilder sb = ShapeBuilder.newPoint(geometry.getCoordinate());
			return FilterBuilders.geoShapeFilter(fieldName, sb, ShapeRelation.INTERSECTS);
		}
		if(geometry.getGeometryType().equalsIgnoreCase("LINESTRING")){
			LineStringBuilder sb = ShapeBuilder.newLineString().points(geometry.getCoordinates());
			return FilterBuilders.geoShapeFilter(fieldName, sb, ShapeRelation.INTERSECTS);
		}	
		else {
			log.error("Unsupported geometry type in getGeoShapeFilterBuilder " + geometry.getGeometryType());
			return null;
		}
	}
	
	public static FilterBuilder getGeometryFilter(String fieldName, List<Geometry> geometries)
	{
		OrFilterBuilder geoFilter = FilterBuilders.orFilter();
		
		for(Geometry geo : geometries)
		{
			geoFilter.add(getGeoShapeFilterBuilder(fieldName, geo));
		}
		
		return geoFilter;
	}
}
