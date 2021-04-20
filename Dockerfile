FROM openjdk:14
VOLUME /tmp
ADD build/libs/template-service-0.0.1-SNAPSHOT.jar template-service.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","template-service.jar"]
