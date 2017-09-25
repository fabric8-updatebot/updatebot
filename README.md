## UpdateBot

A bot for updating dependencies

### Configuration

UpdateBot takes a simple YAML file to define which git repositories and github organisations so search for repositories to update.

See [an example UpdateBot YAML file](updatebot-core/src/test/resources/updatebot.yml)

### Using UpdateBot

The updatebot jar file is a fat executable jar so you can use: 

    java -jar updatebot-${version}.jar
  
But the jar is also a unix binary so you can just run the following:

    ./updatebot-${version}.jar

To install on a unix operating system just copy the updatebot-${version).jar to file called `updatebot` on your `PATH`

### Pushing

When you release an artifact its common to wish to eagerly update all of the projects using your artifact so that you can get fast feedback if your new version breaks any Continuous Integration tests.

To do this just run the `push` command passing in the kind of project, the property name and version.

e.g.

    updatebot push -k npm myapp 1.2.3
    
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


    
      
  