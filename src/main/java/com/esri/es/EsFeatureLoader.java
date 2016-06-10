package com.esri.es;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.Geometries;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.JTS;
import org.opengis.referencing.operation.MathTransform;

import java.math.RoundingMode;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.List;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


public class EsFeatureLoader implements FeatureLoadable {
    private static final Logger logger = LogManager.getLogger(EsFeatureLoader.class);
    protected Client client = null;
    protected String esIndex = "";

    public EsFeatureLoader() {
    }


    public void connect(String esHost, int esPort, String esIndex) throws Exception {
        this.esIndex = esIndex;

        System.out.println("Connecting to Elasticsearch...");
        client = TransportClient.builder().build()
                .addTransportAddress(new
                        InetSocketTransportAddress(InetAddress.getByName(esHost), esPort));
        System.out.println("Connected...");
    }

    public void disconnect() {
        client.close();
    }

    public void loadFeatures(List<AttributeDescriptor> descs, FeatureIterator<SimpleFeature> features) throws Exception {
        SimpleFeature sf;
        int badFeatures = 0;

        while (features.hasNext()) {
            sf = features.next();
            //logger.debug("Reading: ID={}GEOMETRY={}", sf.getID(), sf.getDefaultGeometry());

            XContentBuilder xb = jsonBuilder().startObject();
            buildFeature(descs, sf, xb);
            xb.endObject();

            //try {
                IndexResponse response = client.prepareIndex(esIndex, "feature", sf.getID())
                        .setSource(xb)
                        .execute()
                        .actionGet();

                System.out.format("Loaded job ID: %s\n", sf.getID());
            //} catch (Exception ex) {
            //    System.out.println("Error, couldn't load feature:" + ex.getMessage());
            //    badFeatures++;
            //}
        }

        System.out.format("Couldn't load %s features\n", badFeatures);

        features.close();
    }

    /*
    "shape" : {
        "type" : "polygon",
                "coordinates" : [[
        [ 4.89218, 52.37356 ],
        [ 4.89205, 52.37276 ],
        [ 4.89301, 52.37274 ],
        [ 4.89392, 52.37250 ],
        [ 4.89431, 52.37287 ],
        [ 4.89331, 52.37346 ],
        [ 4.89305, 52.37326 ],
        [ 4.89218, 52.37356 ]
        ]]
    }
    */

    private void buildPolygon(XContentBuilder xb, Polygon geo) throws Exception {
        //comment
        double prevX = -190.0;
        double prevY = -190.0;

        if (geo != null) {
            xb.startObject("shape");

            xb.field("type", "polygon");
            xb.startArray("coordinates");
            xb.startArray();

            Coordinate[] coords = geo.getExteriorRing().getCoordinates();
            for (int i = 0; i < coords.length; i++) {
                if (!((prevX == coords[i].x) && (prevY == coords[i].y))) {
                    xb.startArray();
                    xb.value(coords[i].x);
                    xb.value(coords[i].y);
                    xb.endArray();

                    prevX = coords[i].x;
                    prevY = coords[i].y;
                }
            }

            xb.endArray();
            xb.endArray();

            xb.endObject();

            //System.out.println(xb.string());
        }
    }

    private void buildFeature(List<AttributeDescriptor> descs, SimpleFeature sf, XContentBuilder xb) throws Exception {
        Geometry sourceGeo = (Geometry) sf.getDefaultGeometry();

        CoordinateReferenceSystem sourceCRS = sf.getFeatureType().getCoordinateReferenceSystem();
        CoordinateReferenceSystem targetCRS = org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;

        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
        Geometry targetGeometry = JTS.transform(sourceGeo, transform);

       switch (Geometries.get(targetGeometry)) {
           case POINT:
               throw new Exception("Points not supported");
           case MULTIPOINT:
               throw new Exception("Multipoint not supported");
           case LINESTRING:
               throw new Exception("Linestring not supported");
           case MULTILINESTRING:
               throw new Exception("Multilinestring not supported");
           case POLYGON:
               buildPolygon(xb, (Polygon) targetGeometry);
           case MULTIPOLYGON:
               MultiPolygon multiPolygon = (MultiPolygon) targetGeometry;

               if (multiPolygon.getNumGeometries() > 1) {
                   throw new Exception("Multipolygon that holds more than 1 polygon not supported");
               } else {
                   buildPolygon(xb, (Polygon) multiPolygon.getGeometryN(0));
               }
           default:

       }

       for (AttributeDescriptor desc : descs) {
           if (!desc.getLocalName().equalsIgnoreCase("the_geom")) {
               if (sf.getAttribute(desc.getLocalName()) != null) {
                   xb.field(desc.getLocalName(), sf.getAttribute(desc.getLocalName()));
               }
           }
        }
    }
}
