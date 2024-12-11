# spring-6-gateway
Examples of Reactive Programming with Spring Framework.

## Getting started
Server runs on port 8080. Requires that other projects are up and running
* spring-6-auth-server on port 9000
* spring-6-rest-mvc on port 8081
* spring-6-reactive on port 8082
* spring-6-reactive-mongo on port 8083
Example request you can find in the restRequest directory

This repository has examples from my course [Reactive Programming with Spring Framework 5](https://www.udemy.com/reactive-programming-with-spring-framework-5/?couponCode=GITHUB_REPO_SF5B2G)

## Docker

### create image
```shell
.\mvnw clean package spring-boot:build-image
```
or just run
```shell
.\mvnw clean install
```

### run image

Hint: remove the daemon flag -d to see what is happening, else it run in background

```shell
docker run --name gateway -d -p 8080:8080 -e SPRING_PROFILES_ACTIVE=docker --link auth-server:auth-server --link rest-mvc:rest-mvc spring-6-gateway:0.0.1-SNAPSHOT
docker stop gateway
docker rm gateway
docker start gateway
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

# Images
* spring-6-gateway:0.0.1-SNAPSHOT
* spring-6-auth-server:0.0.1-SNAPSHOT
* spring-6-rest-mvc:0.0.1-SNAPSHOT
* spring-6-reactive:0.0.1-SNAPSHOT
* spring-6-reactive-mongo:0.0.1-SNAPSHOT




## All Spring Framework Guru Courses
### Spring Framework 6
* [Spring Framework 6 - Beginner to Guru](https://www.udemy.com/course/spring-framework-6-beginner-to-guru/?referralCode=2BD0B7B7B6B511D699A9)
* [Spring AI: Beginner to Guru](https://www.udemy.com/course/spring-ai-beginner-to-guru/?referralCode=EF8DB31C723FFC8E2751)
* [Hibernate and Spring Data JPA: Beginner to Guru](https://www.udemy.com/course/hibernate-and-spring-data-jpa-beginner-to-guru/?referralCode=251C4C865302C7B1BB8F)
* [API First Engineering with Spring Boot](https://www.udemy.com/course/api-first-engineering-with-spring-boot/?referralCode=C6DAEE7338215A2CF276)
* [Introduction to Kafka with Spring Boot](https://www.udemy.com/course/introduction-to-kafka-with-spring-boot/?referralCode=15118530CA63AD1AF16D)
* [Spring Security: Beginner to Guru](https://www.udemy.com/course/spring-security-core-beginner-to-guru/?referralCode=306F288EB78688C0F3BC)
