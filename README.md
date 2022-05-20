# General Instructions

This project uses an openapi generator to generate API resource interfaces. To generate the interfaces
execute `./mvnw compile`. These interfaces can be extended to
implement the interfaces.
In order to run the project execute `./mvnw spring-boot:run`.
Make sure that all your tests are executed when calling `./mvnw verify`.

# Sonar

In order to use sonar you have to install docker and docker-compose first. For Windows you can use Docker Desktop for
Windows.

Once docker is installed go to the directory `src/main/docker/sonar` and execute the command
`docker-compose up -d` to start sonarqube and the postgresql database. To stop sonar from running execute
`docker-compose stop` in the same directory.

After sonarqube is started access it on http://localhost:9000. You can login with admin admin. Set the new password to 'password'.

You can analyse your current build by executing `./mvnw sonar:sonar` after `./mvnw verify`.