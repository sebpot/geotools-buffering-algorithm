package org.example;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;

public class RandomDataGenerator {
    public SimpleFeatureType featureType;
    public SimpleFeatureBuilder featureBuilder;
    private final GeometryFactory geometryFactory;

    public RandomDataGenerator(){
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("CustomFeatureType");
        builder.add("the_geom", Geometry.class);
        featureType = builder.buildFeatureType();

        featureBuilder = new SimpleFeatureBuilder(featureType);
        geometryFactory = new GeometryFactory();
    }

    public SimpleFeature createPointFeature() {
        double latitude = (Math.random() * 180.0) - 90.0;
        double longitude = (Math.random() * 360.0) - 180.0;
        Point point = this.geometryFactory.createPoint(new Coordinate(longitude, latitude));

        this.featureBuilder.set("the_geom", point);
        return this.featureBuilder.buildFeature(null);
    }

    public SimpleFeature createLineFeature() {
        int n = (int) ((Math.random() * 3) + 4);
        LineString line = createRandomLineString(n);
        this.featureBuilder.set("the_geom", line);

        return this.featureBuilder.buildFeature(null);
    }

    public LineString createRandomLineString(int n) {
        double latitude = (Math.random() * 180.0) - 90.0;
        double longitude = (Math.random() * 360.0) - 180.0;

        Coordinate[] coords = new Coordinate[n];
        coords[0] = new Coordinate(longitude, latitude);

        for (int i = 1; i < n; i++) {
            double deltaX = (Math.random() * 20.0);
            double deltaY = (Math.random() * 20.0);
            longitude += deltaX;
            if(Math.random() > 0.5) latitude -= deltaY;
            else latitude += deltaY;
            coords[i] = new Coordinate(longitude, latitude);
        }

        return this.geometryFactory.createLineString(coords);
    }

    public SimpleFeature createPolygonFeature() {
        int n = (int) ((Math.random() * 5) + 4);
        Polygon poly = createRandomPolygon(n);
        this.featureBuilder.add(poly);

        return this.featureBuilder.buildFeature(null);
    }


    public Polygon createRandomPolygon(int n) {
        double startingLatitude = (Math.random() * 180.0) - 90.0;
        double startingLongitude = (Math.random() * 360.0) - 180.0;

        Polygon poly = null;
        boolean isValid = false;
        while (!isValid) {
            Coordinate[] coords = new Coordinate[n];
            coords[0] = new Coordinate(startingLongitude, startingLatitude);

            double longitude = startingLongitude;
            double latitude = startingLatitude;

            for (int i = 1; i < n - 1; i++) {
                double deltaX = (Math.random() * 20.0);
                double deltaY = (Math.random() * 20.0);
                longitude += deltaX;
                if(i > n/2) latitude -= deltaY;
                else latitude += deltaY;
                coords[i] = new Coordinate(longitude, latitude);
            }
            coords[n-1] = new Coordinate(startingLongitude, startingLatitude);

            poly = this.geometryFactory.createPolygon(coords);
            isValid = poly.isValid();
        }
        return poly;
    }
}
