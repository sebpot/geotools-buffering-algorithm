package org.example;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.*;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.style.*;
import org.geotools.api.style.Stroke;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQLException;
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
        File file = JFileDataStoreChooser.showOpenFile("csv", null);
        if (file == null) {
            return null;
        }

        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        return store.getFeatureSource();
    }

    public static Style createRandomDataStyle() {
        StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
        FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

        // Create a Style for points
        Mark mark = styleFactory.getCircleMark();
        mark.setStroke(styleFactory.createStroke(filterFactory.literal(Color.BLACK), filterFactory.literal(2)));
        mark.setFill(styleFactory.createFill(filterFactory.literal(Color.BLACK)));

        Graphic graphic = styleFactory.createDefaultGraphic();
        graphic.graphicalSymbols().clear();
        graphic.graphicalSymbols().add(mark);
        graphic.setSize(filterFactory.literal(2));

        PointSymbolizer pointSymbolizer = styleFactory.createPointSymbolizer(graphic, null);

        // Create a Style for lines
        Stroke stroke = styleFactory.createStroke(
                filterFactory.literal(Color.BLACK),
                filterFactory.literal(1)
        );

        LineSymbolizer lineSymbolizer = styleFactory.createLineSymbolizer(stroke, null);

        //Create Style
        FeatureTypeStyle featureTypeStyle = styleFactory.createFeatureTypeStyle();
        Rule pointRule = styleFactory.createRule();
        pointRule.symbolizers().add(pointSymbolizer);
        featureTypeStyle.rules().add(pointRule);

        Rule lineRule = styleFactory.createRule();
        lineRule.symbolizers().add(lineSymbolizer);
        featureTypeStyle.rules().add(lineRule);

        Style style = styleFactory.createStyle();
        style.featureTypeStyles().add(featureTypeStyle);
        return style;
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

    public static void main(String[] args) throws IOException, CQLException {
        boolean fromFile = chooseDataInput();

        RandomDataGenerator randomDataGenerator = new RandomDataGenerator();

        SimpleFeatureCollection features;
        SimpleFeatureSource featureSource = null;
        if(fromFile){
            featureSource = getFeaturesFromFile();
            if(featureSource == null) return;
            features = featureSource.getFeatures();
        } else {
            List<SimpleFeature> featuresList = new ArrayList<>();
            for(int i=0; i<POINT_NUMBER; i++){
                featuresList.add(randomDataGenerator.createPointFeature());
            }

            for(int i=0; i<LINE_NUMBER; i++){
                featuresList.add(randomDataGenerator.createLineFeature());
            }

            for(int i=0; i<POLYGON_NUMBER; i++){
                featuresList.add(randomDataGenerator.createPolygonFeature());
            }

            features = new ListFeatureCollection(randomDataGenerator.featureType, featuresList);
        }

        // Create a map content and add our shapefile to it
        MapContent map = new MapContent();
        map.setTitle("Buffering algorithm");

        Style style = null;
        if(fromFile){
            style = SLD.createSimpleStyle(featureSource.getSchema());
        } else {
            style = createRandomDataStyle();
        }

        //buffering
        boolean buffering = false;
        SimpleFeatureCollection bufferedFeatures = null;
        if(buffering){
            SimpleFeatureType schema = null;
            if(fromFile){
                schema = featureSource.getSchema();
            }
            else{
                schema = randomDataGenerator.featureType;
            }
            bufferedFeatures = new ListFeatureCollection(schema, bufferFeatures(features));
        }

        Layer layer;
        if(buffering){
            layer = new FeatureLayer(bufferedFeatures, style);
        } else {
            layer = new FeatureLayer(features, style);
        }
        map.addLayer(layer);

        // Now display the map
        JMapFrame.showMap(map);
    }
}