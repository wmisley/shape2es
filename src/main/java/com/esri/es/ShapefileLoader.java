package com.esri.es;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShapefileLoader {
    private static final Logger logger = LogManager.getLogger(ShapefileLoader.class);

    public ShapefileLoader() {
    }

    public void load(String shapefilePath, FeatureLoadable loader) throws Exception {
        File file = new File(shapefilePath);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("url", file.toURI().toURL());

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];

        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore
                .getFeatureSource(typeName);

        List<AttributeDescriptor> descs = source.getSchema().getAttributeDescriptors();

        Filter filter = Filter.INCLUDE;

        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
        logger.info("Shapefile has {} features", collection.size());

        loader.loadFeatures(descs, collection.features());
    }
}
