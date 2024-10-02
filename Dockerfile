FROM openjdk:22
WORKDIR /docker-dir
CMD ["java", "-jar", "/docker-dir/server.jar"]