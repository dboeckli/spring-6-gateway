# spring-6-gateway
Examples of Reactive Programming with Spring Framework.

## Getting started
Server runs on port 8080. Requires that other projects are up and running
* spring-6-auth-server on port 9000
* spring-6-rest-mvc on port 8081
* spring-6-reactive on port 8082
* spring-6-reactive-mongo on port 8083
Example request you can find in the restRequest directory

![alt text](guru.png "Overview")

## Docker

### Images
* spring-6-gateway:0.0.1-SNAPSHOT
* spring-6-auth-server:0.0.1-SNAPSHOT
* spring-6-rest-mvc:0.0.1-SNAPSHOT
* spring-6-reactive:0.0.1-SNAPSHOT
* spring-6-reactive-mongo:0.0.1-SNAPSHOT

### create image
```shell
.\mvnw clean package spring-boot:build-image
```
or just run
```shell
.\mvnw clean install
```

Do this for all above projects to get the images into the local docker registry


## Docker

[Docker Documentation](docker-manual/DockerCommands.md)

## Kubernetes

[Kubernetes Documentation](k8s/KubeCommands.md)

The approach having all kubernetes files of the other projects here should be reworked. the kubernetes files should go into the 
appropriate projects, templating with helm and deployment into a kubernetes environment should be considered.
