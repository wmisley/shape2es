# shape2es
This is a Java commandline tool that supports loading shapefiles into Elasticsearch, including the geometry.

# Features
This command line tool supports loading point, line, and polygon features from a shape file into ElasticSearch. It will automatically map the geo_shape column type for the shape and load the feature geometry. It will also load all the columns of data in the shapefile with the same name as the column name in the source data.

# Prerequisites

* Java 8
* Maven 
* Elasticsearch 2.3.3


# Instructions
```
mvn install
```
