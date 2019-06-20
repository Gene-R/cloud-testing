call mvn clean
cd config-service
call mvn install
start java -jar target/config-service-0.0.1-SNAPSHOT.jar
cd ..
call mvn install