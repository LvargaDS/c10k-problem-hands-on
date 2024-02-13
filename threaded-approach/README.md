## running in docker

Beware that you need this project and also cgi-approach project files.
So start docker with mounting "." the directory has to be done from the parent directory.

```shell
cd ..
mvn clean package -pl cgi-approach,threaded-approach
docker run -it -v ./:/c10k --memory-swap 1g --memory 1g openjdk:8-jre-slim /bin/bash
java -cp "threaded-approach/target/threaded-approach-1.0-SNAPSHOT.jar:cgi-approach/target/cgi-approach-1.0-SNAPSHOT.jar" eu.digitalsystems.ThreadedApp
```

Then you can check used resources by running `docker stats`.

Around 16500 servers started within 1GB memory limit.
