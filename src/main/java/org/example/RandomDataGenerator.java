package org.example;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;

public class RandomDataGenerator {
    public SimpleFeatureType featureType;

    public RandomDataGenerator(){
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("CustomFeatureType");

        builder.add("the_geom", Geometry.class);

        featureType = builder.buildFeatureType();
    }

    public SimpleFeature createPointFeature() {
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);

        double latitude = (Math.random() * 180.0) - 90.0;
        double longitude = (Math.random() * 360.0) - 180.0;
        //double latitude = (Math.random() * 20.0) - 10.0;
        //double longitude = (Math.random() * 40.0) - 20.0;

        GeometryFactory geometryFactory = new GeometryFactory();
        /* Longitude (= x coord) first ! */
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));

        featureBuilder.set("the_geom", point);
  /*  featureBuilder.add(name);
    featureBuilder.add(number);*/
        return featureBuilder.buildFeature(null);
    }

    public SimpleFeature createLineFeature() {
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        LineString line = createRandomLineString(5);
        featureBuilder.add(line);

        SimpleFeature feature = featureBuilder.buildFeature(null);
        return feature;
    }

    public static LineString createRandomLineString(int n) {
        double latitude = (Math.random() * 180.0) - 90.0;
        double longitude = (Math.random() * 360.0) - 180.0;
        GeometryFactory geometryFactory = new GeometryFactory();
        /* Longitude (= x coord) first ! */
        ArrayList<Coordinate> points = new ArrayList<Coordinate>();
        points.add(new Coordinate(longitude, latitude));
        for (int i = 1; i < n; i++) {
            double deltaX = (Math.random() * 30.0) - 5.0;
            double deltaY = (Math.random() * 30.0) - 5.0;
            longitude += deltaX;
            latitude += deltaY;
            points.add(new Coordinate(longitude, latitude));
        }
        LineString line = geometryFactory.createLineString((Coordinate[]) points.toArray(new Coordinate[] {}));
        return line;
    }

    public SimpleFeature createPolygonFeature() {
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        Polygon poly = createRandomPolygon(5);
        featureBuilder.add(poly);

        SimpleFeature feature = featureBuilder.buildFeature(null);
        return feature;
    }


    public static Polygon createRandomPolygon(int n) {
        double latitude = (Math.random() * 180.0) - 90.0;
        double longitude = (Math.random() * 360.0) - 180.0;
        GeometryFactory geometryFactory = new GeometryFactory();
        /* Longitude (= x coord) first ! */
        Polygon poly = null;
        boolean valid = false;
        while (!valid) {
            ArrayList<Coordinate> points = new ArrayList<Coordinate>();
            points.add(new Coordinate(longitude, latitude));
            double lon = longitude;
            double lat = latitude;
            for (int i = 1; i < n; i++) {
                double deltaX = (Math.random() * 30.0) - 5.0;
                double deltaY = (Math.random() * 30.0) - 5.0;
                lon += deltaX;
                lat += deltaY;
                points.add(new Coordinate(lon, lat));
            }
            points.add(new Coordinate(longitude, latitude));
            poly = geometryFactory.createPolygon((Coordinate[]) points.toArray(new Coordinate[] {}));
            valid = poly.isValid();
        }
        return poly;
    }
}
