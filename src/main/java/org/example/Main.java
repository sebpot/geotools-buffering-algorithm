package org.example;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import org.geotools.api.data.*;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.*;
import org.geotools.api.style.Stroke;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.locationtech.jts.geom.Polygon;

import static org.example.BufferFactory.bufferFeatures;

public class Main {
    private static final double POINT_NUMBER = 10;
    private static final double LINE_NUMBER = 10;
    private static final double POLYGON_NUMBER = 5;

    public static boolean chooseDataInput() {
        String[] choices = {"Load from file", "Generate random data"};

        JFrame frame = new JFrame("Map data");
        String selectedChoice = (String) JOptionPane.showInputDialog(frame,
                "Choose one",
                "Choose data input",
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                choices[0]);

        return selectedChoice.equals(choices[0]);
    }

    public static SimpleFeatureSource getFeaturesFromFile() throws IOException {
        File file = JFileDataStoreChooser.showOpenFile("shp", null);
        if (file == null) {
            return null;
        }

        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        return store.getFeatureSource();
    }

    public static Style createStyleForBufferedFeatures() {
        Style style;
        StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
        FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

        Stroke stroke = styleFactory.createStroke(
                filterFactory.literal(java.awt.Color.BLACK),
                filterFactory.literal(1) // Stroke width
        );
        Fill fill = styleFactory.createFill(filterFactory.literal(java.awt.Color.BLUE));
        PolygonSymbolizer polygonSymbolizer = styleFactory.createPolygonSymbolizer(stroke, fill, null);
        Rule rule = styleFactory.createRule();
        rule.symbolizers().add(polygonSymbolizer);

        FeatureTypeStyle featureTypeStyle = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
        style = styleFactory.createStyle();
        style.featureTypeStyles().add(featureTypeStyle);

        return style;
    }

    public static void exportToShapefile(SimpleFeatureCollection collection, SimpleFeatureType type) throws IOException {
        File newFile = new File("export2.shp");

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params = new HashMap<>();
        params.put("url", newFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore =
                (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

        /*
         * TYPE is used as a template to describe the file contents
         */
        newDataStore.createSchema(type);

        Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
        SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();
        /*
         * The Shapefile format has a couple limitations:
         * - "the_geom" is always first, and used for the geometry attribute name
         * - "the_geom" must be of type Point, MultiPoint, MuiltiLineString, MultiPolygon
         * - Attribute names are limited in length
         * - Not all data types are supported (example Timestamp represented as Date)
         *
         * Each data store has different limitations so check the resulting SimpleFeatureType.
         */
        System.out.println("SHAPE:" + SHAPE_TYPE);

        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            /*
             * SimpleFeatureStore has a method to add features from a
             * SimpleFeatureCollection object, so we use the ListFeatureCollection
             * class to wrap our list of features.
             */
            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(collection);
                transaction.commit();
            } catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();
            } finally {
                transaction.close();
            }
            System.exit(0); // success!
        } else {
            System.out.println(typeName + " does not support read/write access");
            System.exit(1);
        }
    }

    public static void main(String[] args) throws IOException, FactoryException, SchemaException {
        System.setProperty("org.geotools.referencing.forceXY", "true");

        //choose data input (file or generated data)
        boolean fromFile = chooseDataInput();

        String input = JOptionPane.showInputDialog("Enter buffering distance");
        double bufferDistance = Double.parseDouble(input);

        RandomDataGenerator randomDataGenerator = new RandomDataGenerator();

        //extract or create features
        SimpleFeatureCollection features, pointFeatures = null;
        SimpleFeatureSource featureSource = null;
        if(fromFile){
            featureSource = getFeaturesFromFile();
            if(featureSource == null) return;
            features = featureSource.getFeatures();
        } else {
            List<SimpleFeature> pointsFeaturesList = new ArrayList<>();
            List<SimpleFeature> linesAndPolysFeaturesList = new ArrayList<>();
            for(int i=0; i<POINT_NUMBER; i++){
                pointsFeaturesList.add(randomDataGenerator.createPointFeature());
            }

            for(int i=0; i<LINE_NUMBER; i++){
                linesAndPolysFeaturesList.add(randomDataGenerator.createLineFeature());
            }

            for(int i=0; i<POLYGON_NUMBER; i++){
                linesAndPolysFeaturesList.add(randomDataGenerator.createPolygonFeature());
            }

            features = new ListFeatureCollection(randomDataGenerator.featureType, linesAndPolysFeaturesList);
            pointFeatures = new ListFeatureCollection(randomDataGenerator.featureType, pointsFeaturesList);
        }

        // Create a map content and add our shapefile to it
        MapContent map = new MapContent();
        map.setTitle("Buffering algorithm");

        //create style for buffered features
        Style style = createStyleForBufferedFeatures();

        //buffer features
        SimpleFeatureCollection bufferedFeatures = null, bufferedFeatures2 = null;
        SimpleFeatureType schema;
        if(fromFile){
            schema = featureSource.getSchema();
        }
        else{
            schema = randomDataGenerator.featureType;
        }

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.init(schema);
        builder.remove("the_geom");
        builder.add("the_geom", Polygon.class);
        schema = builder.buildFeatureType();

        bufferedFeatures = new ListFeatureCollection(schema, bufferFeatures(features, bufferDistance));
        if(!fromFile)
            bufferedFeatures2 = new ListFeatureCollection(schema, bufferFeatures(pointFeatures, bufferDistance));

        //exportToShapefile(bufferedFeatures, schema);

        //create layers
        Layer layer = null, layer2 = null;
        layer = new FeatureLayer(bufferedFeatures, style);
        map.addLayer(layer);
        if(!fromFile) {
            layer2 = new FeatureLayer(bufferedFeatures2, style);
            map.addLayer(layer2);
        }

        // Now display the map
        JMapFrame.showMap(map);
    }
}