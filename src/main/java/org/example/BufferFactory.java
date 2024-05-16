package org.example;

import org.geotools.api.feature.GeometryAttribute;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.GeometryType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.geom.*;


import java.util.ArrayList;
import java.util.List;

public class BufferFactory {
    private static final double BUFFER_DISTANCE = 100.0;

    private static Coordinate[] createCircleCoordinates(Coordinate center, double radius, int numCoords) {
        Coordinate[] coords = new Coordinate[numCoords + 1];
        double angleIncrement = 2 * Math.PI / numCoords;

        for (int i = 0; i < numCoords; i++) {
            double angle = angleIncrement * i;
            double x = center.x + radius * Math.cos(angle);
            double y = center.y + radius * Math.sin(angle);
            coords[i] = new Coordinate(x, y);
        }
        coords[numCoords] = new Coordinate(coords[0].x, coords[0].y);

        return coords;
    }

    private static Geometry bufferPoint(Point point, double distance, GeometryFactory factory) {
        Coordinate[] coords = createCircleCoordinates(point.getCoordinate(), distance, 16);
        LinearRing ring = factory.createLinearRing(coords);
        return factory.createPolygon(ring);
        //return point.buffer(distance);
    }

    private static List<Coordinate> extractVertices(MultiLineString multiLineString) {
        List<Coordinate> vertices = new ArrayList<>();
        for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
            LineString lineString = (LineString) multiLineString.getGeometryN(i);
            for (Coordinate coordinate : lineString.getCoordinates()) {
                if (!vertices.contains(coordinate)) {
                    vertices.add(coordinate);
                }
            }
        }
        return vertices;
    }

    private static Geometry bufferLine(MultiLineString line, double distance, GeometryFactory factory) {
        factory = line.getFactory();
        List<Coordinate> vertices = extractVertices(line);

        // Initialize list to hold buffered polygons
        List<Polygon> bufferedPolygons = new ArrayList<>();

        // Buffer every pair of adjacent vertices
        for (int i = 0; i < vertices.size() - 1; i++) {
            Coordinate p1 = vertices.get(i);
            Coordinate p2 = vertices.get(i + 1);

            // Calculate angle and perpendicular vectors
            double angle = Angle.angle(p1, p2);
            double dx = distance * Math.cos(angle + Math.PI / 2);
            double dy = distance * Math.sin(angle + Math.PI / 2);

            // Calculate buffered coordinates for both sides
            Coordinate buf1 = new Coordinate(p1.x + dx, p1.y + dy);
            Coordinate buf2 = new Coordinate(p2.x + dx, p2.y + dy);
            Coordinate buf3 = new Coordinate(p2.x - dx, p2.y - dy);
            Coordinate buf4 = new Coordinate(p1.x - dx, p1.y - dy);

            // Create LinearRing for the buffered polygon
            Coordinate[] ringCoords = {buf1, buf2, buf3, buf4, buf1};
            LinearRing ring = factory.createLinearRing(ringCoords);

            // Create Polygon from the LinearRing
            Polygon bufferedPolygon = factory.createPolygon(ring, null);

            // Add the buffered polygon to the list
            bufferedPolygons.add(bufferedPolygon);
            bufferedPolygons.add((Polygon) bufferPoint(factory.createPoint(new Coordinate(p1.x, p1.y)), distance, factory));
            if(i + 1 == vertices.size() - 1){
                bufferedPolygons.add((Polygon) bufferPoint(factory.createPoint(new Coordinate(p2.x, p2.y)), distance, factory));
            }
        }

        // Combine buffered polygons into a single geometry
        GeometryCollection geometryCollection = factory.createGeometryCollection(bufferedPolygons.toArray(new Polygon[0]));
        return geometryCollection.union();
        //return line.buffer(distance);
    }

    private static Geometry bufferPolygon(MultiPolygon polygon, double distance, GeometryFactory factory) {
        // Implement buffer logic for polygons
        return polygon.buffer(distance);
        //return null;
    }

    public static Geometry buffer(Geometry geometry, double distance) {
        GeometryFactory factory = geometry.getFactory();

        if (geometry instanceof Point) {
            return bufferPoint((Point) geometry, distance, factory);
        } else if (geometry instanceof MultiLineString) {
            return bufferLine((MultiLineString) geometry, distance, factory);
        } else if (geometry instanceof LineString) {
            MultiLineString changedGeometry = factory.createMultiLineString(new LineString[]{(LineString) geometry});
            return bufferLine(changedGeometry, distance, factory);
        } else if (geometry instanceof MultiPolygon) {
            return bufferPolygon((MultiPolygon) geometry, distance, factory);
        } else if (geometry instanceof Polygon) {
            MultiPolygon changedPolygon = factory.createMultiPolygon(new Polygon[]{(Polygon) geometry});
            return bufferPolygon(changedPolygon, distance, factory);
        } else {
            return null;
        }
    }

    public static Geometry bufferGeom(CoordinateReferenceSystem origCRS, Geometry geom) throws FactoryException, TransformException {
        if(geom.isEmpty()) return null;
        String code = "AUTO:42001," + geom.getCentroid().getCoordinate().x + "," + geom.getCentroid().getCoordinate().y;
        CoordinateReferenceSystem auto = CRS.decode(code);

        MathTransform toTransform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, auto);
        MathTransform fromTransform = CRS.findMathTransform(auto, DefaultGeographicCRS.WGS84);

        Geometry pGeom = JTS.transform(geom, toTransform);
        Geometry pBufferedGeom = buffer(pGeom, BUFFER_DISTANCE);
        //return pBufferedGeom;
        return JTS.transform(pBufferedGeom, fromTransform);
    }
    public static SimpleFeature bufferFeature(SimpleFeature feature) throws FactoryException, TransformException {
        GeometryAttribute gProp = feature.getDefaultGeometryProperty();
        CoordinateReferenceSystem origCRS = gProp.getDescriptor().getCoordinateReferenceSystem();

        Geometry geom = (Geometry) feature.getDefaultGeometry();
        //System.out.println(geom);
        Geometry retGeom = bufferGeom(origCRS, geom);
        //System.out.println(retGeom);
        SimpleFeatureType schema = feature.getFeatureType();
        SimpleFeatureTypeBuilder ftBuilder = new SimpleFeatureTypeBuilder();
        ftBuilder.setCRS(origCRS);

        for (AttributeDescriptor attrib : schema.getAttributeDescriptors()) {
            AttributeType type = attrib.getType();

            if (type instanceof GeometryType) {
                String oldGeomAttrib = attrib.getLocalName();
                ftBuilder.add(oldGeomAttrib, Polygon.class);
            } else {
                ftBuilder.add(attrib);
            }
        }
        ftBuilder.setName(schema.getName());

        SimpleFeatureType nSchema = ftBuilder.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(nSchema);
        java.util.List<Object> atts = feature.getAttributes();
        for (int i = 0; i < atts.size(); i++) {
            if (atts.get(i) instanceof Geometry) {
                atts.set(i, retGeom);
            }
        }
        SimpleFeature nFeature = builder.buildFeature(null, atts.toArray());
        return nFeature;
    }

    public static java.util.List<SimpleFeature> bufferFeatures(SimpleFeatureCollection features) {
        List<SimpleFeature> bufferedFeaturesList = new ArrayList<>();

        SimpleFeatureIterator iterator = features.features();
        try {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                bufferedFeaturesList.add(bufferFeature(feature));
            }
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        } finally {
            iterator.close();
        }
        return bufferedFeaturesList;
    }
}
