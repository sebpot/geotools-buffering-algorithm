package org.example;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.style.*;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.buffer.BufferOp;

public class Main {

    private static final double BUFFER_DISTANCE = 0.01;
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

    public static List<SimpleFeature> bufferFeatures(SimpleFeatureCollection features) {
        List<SimpleFeature> bufferedFeaturesList = new ArrayList<>();

        SimpleFeatureIterator iterator = features.features();

        try {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();

                SimpleFeatureType featureType = feature.getFeatureType();
                SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);

                Geometry geom = (Geometry) feature.getDefaultGeometry();
                /*... do something here */
                for (int i = 0; i < featureType.getAttributeCount(); i++) {
                    String attributeName = featureType.getDescriptor(i).getLocalName();
                    featureBuilder.set(attributeName, feature.getAttribute(attributeName));
                }
                BufferOp bufferOp = new BufferOp(geom);
                featureBuilder.set("the_geom", bufferOp.getResultGeometry(BUFFER_DISTANCE));
                SimpleFeature bufferedFeature = featureBuilder.buildFeature(feature.getID());

                bufferedFeaturesList.add(bufferedFeature);
            }
        } finally {
            iterator.close(); // IMPORTANT
        }

        return bufferedFeaturesList;
    }

    public static void main(String[] args) throws IOException {
        boolean fromFile = chooseDataInput();

        RandomDataGenerator randomDataGenerator = new RandomDataGenerator();

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

        Style style1;
        Style style2 = null;
        if(fromFile){
            style1 = SLD.createSimpleStyle(featureSource.getSchema());
        } else {
            style1 = SLD.createLineStyle(Color.BLACK, 1);
            style2 = SLD.createPointStyle("Circle", Color.BLACK, Color.BLACK, 1, 2);
        }

        //buffering
        boolean buffering = false;
        SimpleFeatureCollection bufferedFeatures = null;
        if(buffering){
            SimpleFeatureType schema;
            if(fromFile){
                schema = featureSource.getSchema();
            }
            else{
                schema = randomDataGenerator.featureType;
            }
            bufferedFeatures = new ListFeatureCollection(schema, bufferFeatures(features));
        }

        Layer layer = null, layer2 = null;
        if(buffering){
            layer = new FeatureLayer(bufferedFeatures, style1);
        } else {
            layer = new FeatureLayer(features, style1);
            if(!fromFile){
                layer2 = new FeatureLayer(pointFeatures, style2);
            }
        }
        map.addLayer(layer);
        if(!fromFile) map.addLayer(layer2);

        // Now display the map
        JMapFrame.showMap(map);
    }
}