Build the project with maven. There should be created 2 jar files: 
- replicated-log-main-1.0-SNAPSHOT.jar,
- replicated-log-secondary-1.0-SNAPSHOT.jar

Used the commands:

  - mvn clean package -Pmain 
  - mvn package -Psecondary

Next build 2 docker images:

  - docker build -t server-image . 
  - docker build -t secondary-image .

And start 3 containers:

  - docker-compose up 