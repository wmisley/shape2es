
![Elastic](src/main/resources/es.png?raw=true "Elastic")

# shape2es
This is a Java commandline tool that supports loading shapefiles into Elasticsearch, including the geometry.

# Features
This command line tool supports loading point, line, and polygon features from a shape file into ElasticSearch. It will automatically map the geo_shape column type for the shape and load the feature geometry. It will also load all the columns of data in the shapefile with the same name as the column name in the source data.

# Prerequisites

* Java 8
* Maven 
* Elasticsearch 2.3.3


# Instructions
To build the tool, perform the following command:
```
mvn install
```
To run the tool, execute the following command:
```
shape2es <shapefile path> <elasticsearch hostname> <elasticsearch port> <elasticsearch indexname>
```
an example run of this command:
```
shape2es /Users/williami/Downloads/BUILDING_P.shp eshost 9300 buildings2
```
