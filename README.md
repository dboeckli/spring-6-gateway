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

#### Images
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

### run image

Hint: remove the daemon flag -d to see what is happening, else it runs in background!
When cleaning stop all running container and rm those
Check running container with
```shell
docker ps
docker stop/start name
```
Check container with
```shell
docker container ls -a
docker rm name
```
You need to start the servers in following order:
1. auth-server
```shell
docker run --name auth-server -d -p 9000:9000 spring-6-auth-server:0.0.1-SNAPSHOT
```
2. mysql and rest-mvc
```shell
docker run --name mysql -d -e MYSQL_USER=restadmin -e MYSQL_PASSWORD=password -e MYSQL_DATABASE=restdb -e MYSQL_ROOT_PASSWORD=password mysql:9
```
```shell
docker run --name rest-mvc -d -p 8081:8080 -e SPRING_PROFILES_ACTIVE=localmysql -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://auth-server:9000 -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/restdb -e SERVER_PORT=8080 --link auth-server:auth-server --link mysql:mysql spring-6-rest-mvc:0.0.1-SNAPSHOT
```
3. reactive (use in memory h2 database)
```shell
docker run --name reactive -d -p 8082:8080 -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://auth-server:9000 -e SERVER_PORT=8080 --link auth-server:auth-server spring-6-reactive:0.0.1-SNAPSHOT
```
4. reactive-mongo and mongo
```shell
docker run --name mongo -d -e MONGO_INITDB_ROOT_USERNAME=root -e MONGO_INITDB_ROOT_PASSWORD=secret -p 27017:27017 mongo 
```
```shell
docker run --name reactive-mongo -d -p 8083:8080 -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://auth-server:9000 -e SERVER_PORT=8080 -e SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/sfg -e SPRING_DATA_MONGODB_USERNAME=root -e SPRING_DATA_MONGODB_PASSWORD=secret --link auth-server:auth-server --link mongo:mongo spring-6-reactive-mongo:0.0.1-SNAPSHOT
```
5. gateway
```shell
docker run --name gateway -d -p 8080:8080 -e SPRING_PROFILES_ACTIVE=docker -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://auth-server:9000 --link auth-server:auth-server --link rest-mvc:rest-mvc --link reactive:reactive --link reactive-mongo:reactive-mongo spring-6-gateway:0.0.1-SNAPSHOT
docker stop gateway
docker rm gateway
docker start gateway
```

## Docker Compose
The docker compose file is starting all above in the right order
```shell
# run with: 
docker compose up
docker compose up -d
# stop (down removes the container):
docker compose stop
docker compose down 
# check: 
docker container ls -a 
docker ps
```

## More Docker commands
Create image
```shell
.\mvnw clean package spring-boot:build-image
```
Run image
```shell
docker run -p 8080:8080 docker.io/library/spring-6-gateway:0.0.1-SNAPSHOT
```
Run the Docker image in background:
```shell
docker run -d -p 8080:8080 spring-6-gateway:0.0.1-SNAPSHOT
```
Run the Docker image in background with docker profile:
```shell
docker run -d -p 8080:8080 -e SPRING_PROFILES_ACTIVE=docker spring-6-gateway:0.0.1-SNAPSHOT
```
List running Docker containers:
```shell
docker ps
```

Name and Run the Docker image in background:
```shell
docker run --name gateway -d -p 8080:8080 spring-6-gateway:0.0.1-SNAPSHOT
```

Stop a running Docker container by name:
```shell
docker stop gateway
```

Restart a stopped Docker container by name. This will restore using the previous settings.
```shell
docker start gateway
```

Stop a running Docker container:
```shell
docker stop <container-id>
```

## All Spring Framework Guru Courses
### Spring Framework 6
* [Spring Framework 6 - Beginner to Guru](https://www.udemy.com/course/spring-framework-6-beginner-to-guru/?referralCode=2BD0B7B7B6B511D699A9)
* [Spring AI: Beginner to Guru](https://www.udemy.com/course/spring-ai-beginner-to-guru/?referralCode=EF8DB31C723FFC8E2751)
* [Hibernate and Spring Data JPA: Beginner to Guru](https://www.udemy.com/course/hibernate-and-spring-data-jpa-beginner-to-guru/?referralCode=251C4C865302C7B1BB8F)
* [API First Engineering with Spring Boot](https://www.udemy.com/course/api-first-engineering-with-spring-boot/?referralCode=C6DAEE7338215A2CF276)
* [Introduction to Kafka with Spring Boot](https://www.udemy.com/course/introduction-to-kafka-with-spring-boot/?referralCode=15118530CA63AD1AF16D)
* [Spring Security: Beginner to Guru](https://www.udemy.com/course/spring-security-core-beginner-to-guru/?referralCode=306F288EB78688C0F3BC)
