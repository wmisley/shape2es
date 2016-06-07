package com.esri.es;


public class Parameters {
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
            System.out.format("ERROR: 3 parameters required");
            System.out.format("<Shapefile path> <ES host> <ES port> <ES index>");
            System.out.println("Exiting program");
        }

        return isValid;
    }
}
