package com.esri.es;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Parameters {
    private static final Logger logger = LogManager.getLogger(Shape2Es.class);

    public Parameters() {
        shapeFilePath = "";
    }

    public String shapeFilePath;
    public String esHost;
    public int esPort;
    public String esIndex;

    public boolean parseInput(String[] args) {
        boolean isValid = false;

        if (args.length == 4) {
            shapeFilePath = args[0];
            esHost = args[1];
            esPort = Integer.parseInt(args[2]);
            esIndex = args[3];
            isValid = true;
        } else {
            logger.error("ERROR: 3 parameters required");
            logger.error("<Shapefile path> <ES host> <ES port> <ES index>");
            logger.error("Exiting program");
        }

        return isValid;
    }
}
