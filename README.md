## UpdateBot

A bot for updating dependencies on your projects automatically!

[![Javadocs](http://www.javadoc.io/badge/io.fabric8.updatebot/updatebot-core.svg?color=blue)](http://www.javadoc.io/doc/io.fabric8.updatebot/updatebot-core)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.fabric8.updatebot/updatebot-core/badge.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/io.fabric8.updatebot/updatebot-core/)
![Apache 2](http://img.shields.io/badge/license-Apache%202-red.svg)

### Configuration

UpdateBot takes a simple YAML file to define which git repositories and github organisations so search for repositories to update.

See [an example UpdateBot YAML file](updatebot-core/src/test/resources/maven/updatebot.yml)

### Using UpdateBot

The updatebot jar file is a fat executable jar so you can use: 

    java -jar updatebot-${version}.jar
  
But the jar is also a unix binary so you can just run the following:

    ./updatebot-${version}.jar

To install on a unix operating system just copy the updatebot-${version).jar to file called `updatebot` on your `PATH`

### Pushing

When you release an artifact its common to wish to eagerly update all of the projects using your artifact so that you can get fast feedback if your new version breaks any Continuous Integration tests.

To do this just run the `push` command passing in the source directory of the source code. e.g. when doing a CD pipeline your local directory will be the tagged source after a release; so use that.

    updatebot push
    
Which will use the source in the current directory or
    
    updatebot push /foo/bar

if you wish to look at the source in `/foo/bar`    
    
#### Pushing other dependencies

You can configure other dependencies to push to other downstream projects. 

e.g. in []this example updatebot.yml](https://github.com/fabric8io/updatebot/blob/master/updatebot-core/src/test/resources/npm/source/updatebot.yml#L6-L13) we let the project `ngx-base` define the exact versions of lots of base dependencies like angular so that whenever they change in `ngx-base` it gets updated into the downstream dependent projects like `ngx-widgets`

We can use the `includes` and `excludes` patterns to filter out which dependencies you wish to push to other projects.


#### Pushing specific versions

Sometimes you just want to upgrade a specific version through your projects. To do this use the `push-version` command:

    updatebot push-version -k npm myapp 1.2.3
    
This will then iterate through all the projects defined by the configuration file you give it and generate the necessary code changes to adopt the new version and submit pull requests.    



### Pulling

We recommend `pushing` version changes eagerly in your CI / CD pipelines.

However projects often depend on lots of dependencies that are released upstream by different teams. So to pull version changes from upstream releases you can use the pull command:

    updatebot push -k npm 

This will then update any dependencies in your projects.

### Requirements

UpdateBot requires the following binaries to be available on your `PATH`

* java
* git

#### Node

To be able to pull version changes into your npm packages we use the [ncu](https://www.npmjs.com/package/npm-check-updates) CLI tool. You can install it via [these instructions](https://www.npmjs.com/package/npm-check-updates) or typing

    npm install -g npm-check-updates


### Docker

If you want to use UpdateBot inside a docker image you can reuse the [fabric8/maven-builder](https://hub.docker.com/r/fabric8/maven-builder/) image


    
      
  
