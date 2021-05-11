FROM openjdk:14
VOLUME /tmp
ADD build/libs/user-service-0.0.1-SNAPSHOT.jar user-service.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","user-service.jar"]
