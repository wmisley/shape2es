package com.esri.es;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.node.Node;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.Geometries;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;

import java.net.InetAddress;
import java.util.List;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;


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

        while (features.hasNext()) {
            sf = features.next();
            logger.debug("Reading: ID={}GEOMETRY={}", sf.getID(), sf.getDefaultGeometry());

            XContentBuilder xb = jsonBuilder().startObject();
            buildFeature(descs, sf, xb);
            xb.endObject();

            IndexResponse response = client.prepareIndex(esIndex, "feature", sf.getID())
                    .setSource(xb)
                    .execute()
                    .actionGet();

            System.out.format("Loaded job ID: %s\n", sf.getID());
        }

        features.close();

        //String alias = esIndex.substring(0, esIndex.lastIndexOf('-'));
        //IndicesAliasesResponse resp = client.admin().indices().prepareAliases().addAlias(esIndex, alias).execute().actionGet();
        //if (!resp.isAcknowledged())
        //    System.out.println("Failed to create index alias " + alias);
    }

    /*

    "location" : {
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

    private void buildPolygon(XContentBuilder xb, Geometry geo) throws Exception {
        if (geo != null) {
            xb.startObject("shape");

            xb.field("type", "polygon");
            xb.startArray("coordinates");
            xb.startArray();

            Coordinate[] coords = geo.getCoordinates();
            for (int i = 0; i < coords.length; i++) {
                xb.startArray();
                xb.value(coords[i].x);
                xb.value(coords[i].y);
                xb.endArray();
            }

            xb.endArray();
            xb.endArray();

            xb.endObject();
        }
    }

    private void buildFeature(List<AttributeDescriptor> descs, SimpleFeature sf, XContentBuilder xb) throws Exception {
       Geometry geo = (Geometry) sf.getDefaultGeometry();

       switch (Geometries.get(geo)) {
           case POINT:
               throw new Exception("Points not supported");
           case MULTIPOINT:
               throw new Exception("Multipoint not supported");
           case LINESTRING:
               throw new Exception("Linestring not supported");
           case MULTILINESTRING:
               throw new Exception("Multilinestring not supported");
           case POLYGON:
           case MULTIPOLYGON:
               buildPolygon(xb, geo);
               //throw new Exception("Multipolygon not supported");
           default:

       }

       for (AttributeDescriptor desc : descs) {
           xb.field(desc.getLocalName(), sf.getAttribute(desc.getLocalName()));
        }
    }
}