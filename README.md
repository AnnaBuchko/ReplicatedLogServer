In project the the simple replication solution was implemented with one main server and 2 and more replications.

Build the project with maven. There should be created 2 jar files: 
- replicated-log-main-1.0-SNAPSHOT.jar,
- replicated-log-secondary-1.0-SNAPSHOT.jar

Execute the next commands to create above files:

  - mvn clean package -Pmain 
  - mvn package -Psecondary

Next build 2 docker images:

  - docker build -t server-image . 
  - docker build -t secondary-image .

And start 3 containers:

  - docker-compose up 
