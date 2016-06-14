package com.esri.es;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Shape2Es {
    private static final Logger logger = LogManager.getLogger(Shape2Es.class);

    static public void main(String[] args) {
        Parameters params = new Parameters();

        if (!params.parseInput(args)) {
            return;
        } else {
            logger.info("Loading {}", params.shapeFilePath);
            logger.info("...to Elasticsearch host {} on port", params.esHost, params.esPort);
            logger.info("...to Elasticsearch index {}", params.esIndex);
        }

        try {
            ShapefileLoader shpLoader = new ShapefileLoader();
            EsFeatureLoader esLoader = new EsFeatureLoader();

            esLoader.connect(params.esHost, params.esPort, params.esIndex);
            esLoader.addGeoShapMapping(params.esIndex);
            shpLoader.load(params.shapeFilePath, esLoader);
            esLoader.disconnect();
        } catch (Exception ex) {
            logger.error("Error loading shapefile", ex);
            logger.error("Exiting program");
        }
    }
}
