package com.esri.es;

import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;

import java.util.List;

public interface FeatureLoadable {
    void loadFeatures(List<AttributeDescriptor> descs, FeatureIterator<SimpleFeature> features) throws Exception;
}
