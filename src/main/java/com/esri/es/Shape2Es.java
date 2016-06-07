package com.esri.es;

public class Shape2Es {
    static public void main(String[] args) {
        Parameters params = new Parameters();

        if (!params.parseInput(args)) {
            return;
        } else {
            System.out.format("Loading %s\n", params.shapeFilePath);
            System.out.format("...to Elasticsearch host %s on port \n", params.esHost, params.esPort);
            System.out.format("...to Elasticsearch index %s\n", params.esIndex);
        }

        try {
            ShapefileLoader shpLoader = new ShapefileLoader();
            EsFeatureLoader esLoader = new EsFeatureLoader();

            esLoader.connect(params.esHost, params.esPort, params.esIndex);
            shpLoader.load(params.shapeFilePath, esLoader);
            esLoader.disconnect();
        } catch (Exception ex) {
            System.out.println("Error loading shapefile");
            System.out.println("Exiting program");
            ex.printStackTrace();
        }
    }
}
