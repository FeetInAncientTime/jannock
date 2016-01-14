# Jannock: PDF Comparison Tools

## Introduction

Comparing [PDF](http://www.adobe.com/products/acrobat/adobepdf.html) documents 
can be difficult: very different files can produce visually identical documents.  
Jannock is a Java library that aims to make this task easier.  It is mainly 
intended to be used in tests for methods that generate PDF documents.  
Jannock is able to ignore:

- document creation dates,
- the reorganization of the internal structure of the document.

## Dependencies

Jannock requires Java 8 or later.

## Downloading

The source code for the Java implementation of this library is available for 
[download](http://github.com/).

The artifacts used by the project are available in the 
[Central Maven repository](http://search.maven.org/).  The library is not
yet available in the central repository.  Nonetheless, the recommended 
coordinates for the current version are:

```
    <dependency>
      <groupId>com.sinefine.utils</groupId>
      <artifactId>jannock</artifactId>
      <version>0.1.0-SNAPSHOT</version>
    </dependency>
```

## Building Jannock

Assuming [Maven 3](https://maven.apache.org/) and [Git 2](http://git-scm.com/) 
are installed and are on the operating system's path then the following commands
can be used to download and build the application:

```
    host$ git clone https://github.com/jannock/jannock.git
    host$ cd jannock
    host$ mvn install
```

## Using Jannock

TODO...

## Configuration

TODO...

## Documentation

Further documentation is available at the project's 
[homepage](http://www.sinefine.com/software/jannock).
