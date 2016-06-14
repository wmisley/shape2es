package com.esri.es;

import com.vividsolutions.jts.geom.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.Geometries;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.JTS;
import org.opengis.referencing.operation.MathTransform;

import java.net.InetAddress;
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

        logger.info("Connecting to Elasticsearch...");
        client = TransportClient.builder().build()
                .addTransportAddress(new
                        InetSocketTransportAddress(InetAddress.getByName(esHost), esPort));
        logger.info("Connected...");
    }

    public void disconnect() {
        client.close();
    }

    /*
        PUT <index>
        {
            "mappings": {
            "feature": {
                "properties": {
                    "shape":    { "type": "geo_shape"  }
                }
            }
        }
    */

    public void addGeoShapMapping(String esIndex) throws Exception {
        try {
            XContentBuilder xb = jsonBuilder().startObject();
            xb.startObject("properties");
            xb.startObject("shape");
            xb.field("type", "geo_shape");
            xb.endObject();
            xb.endObject();
            xb.endObject();

            client.admin().indices().prepareCreate(esIndex)
                    .addMapping("feature", xb)
                    .get();
        } catch (IndexAlreadyExistsException ex) {
            logger.warn("Index already exists, not mapping shape to geo_shape");
        }
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

            try {
                IndexResponse response = client.prepareIndex(esIndex, "feature", sf.getID())
                        .setSource(xb)
                        .execute()
                        .actionGet();

                logger.info("Loaded feature ID: {}", sf.getID());
            } catch (Exception ex) {
                logger.error("Error, couldn't load feature", ex);
                badFeatures++;
            }
        }

        logger.warn("Couldn't load {} features", badFeatures);

        features.close();
    }

    /*
    {
    "location" : {
            "type" : "multilinestring",
            "coordinates" : [
                [ [102.0, 2.0], [103.0, 2.0], [103.0, 3.0], [102.0, 3.0] ],
                [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0] ],
                [ [100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8] ]
            ]
        }
    }
     */

    private void buildMultilineString(XContentBuilder xb, MultiLineString ls) throws Exception {
        if (ls != null) {
            xb.startObject("shape");
            xb.field("type", "multilinestring");
            xb.startArray("coordinates");

            int count = ls.getNumGeometries();

            for (int i = 0; i < count; i ++) {
                LineString path = (LineString) ls.getGeometryN(i);
                buildRing(xb, path.getCoordinates());
            }

            xb.endArray();
            xb.endObject();
        }
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
        if (geo != null) {
            xb.startObject("shape");
            xb.field("type", "polygon");
            xb.startArray("coordinates");

            //Build outer ring (must be counter clock-wise)
            Coordinate[] outerRingCoords = geo.getExteriorRing().getCoordinates();
            buildRing(xb, outerRingCoords);

            //Build inner rings (must be clock-wise)
            if (geo.getNumInteriorRing() > 0) {
                for (int i = 0; i < geo.getNumInteriorRing(); i++) {
                    LinearRing lr = (LinearRing) geo.getInteriorRingN(i);
                    buildRing(xb, lr.getCoordinates());
                }
            }

            xb.endArray();
            xb.endObject();

            //System.out.println(xb.string());
        }
    }

    /*
        {
        "location" : {
            "type" : "point",
            "coordinates" : [-77.03653, 38.897676]
            }
        }
     */

    private void buildPoint(XContentBuilder xb, Point point) throws Exception {
        if (point != null) {
            xb.startObject("shape");
            xb.field("type", "point");
            xb.startArray("coordinates");

            xb.value(point.getX());
            xb.value(point.getY());

            xb.endArray();
            xb.endObject();

        }
    }

    private void buildRing(XContentBuilder xb, Coordinate[] coords) throws Exception {
        double prevX = -190.0;
        double prevY = -190.0;

        xb.startArray();

        for (int i = 0; i < coords.length; i++) {
            //Elasticsearch won't except consecutive coordinates with the same
            //value
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
    }

    private void buildFeature(List<AttributeDescriptor> descs, SimpleFeature sf, XContentBuilder xb) throws Exception {
        Geometry sourceGeo = (Geometry) sf.getDefaultGeometry();

        CoordinateReferenceSystem sourceCRS = sf.getFeatureType().getCoordinateReferenceSystem();
        CoordinateReferenceSystem targetCRS = org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;

        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
        Geometry targetGeometry = JTS.transform(sourceGeo, transform);

       switch (Geometries.get(targetGeometry)) {
           case POINT:
               buildPoint(xb, (Point) targetGeometry);
               break;

           case MULTIPOINT:
               if (targetGeometry.getNumGeometries() > 1) {
                   throw new Exception("Multipoint with more than one point not supported");
               } else {
                   buildPoint(xb, (Point) targetGeometry.getGeometryN(0));
               }

           case LINESTRING:
               throw new Exception("Linestring not supported");

           case MULTILINESTRING:
               buildMultilineString(xb, (MultiLineString) targetGeometry);
               break;

           case POLYGON:
               buildPolygon(xb, (Polygon) targetGeometry);
               break;

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
           //Shapes are loaded into the "shape" column, don't load the "the_geom" column
           if (!desc.getLocalName().equalsIgnoreCase("the_geom")) {
               if (sf.getAttribute(desc.getLocalName()) != null) {
                   xb.field(desc.getLocalName(), sf.getAttribute(desc.getLocalName()));
               }
           }
        }
    }
}
