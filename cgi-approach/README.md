## Running it in docker

```shell
mvn package
docker run -it -v .:/c10k --memory-swap 1g --memory 1g openjdk:8-jre-slim /bin/bash
cd /c10k/target
java -cp cgi-approach-1.0-SNAPSHOT.jar eu.digitalsystems.CGILikeApp 5555
```

Around 260 servers started within 1GB memory limit. You can check using this:

```shell
mvn package
docker run -it -v .:/c10k --memory-swap 1g --memory 1g openjdk:8-jre-slim /bin/bash
cd /c10k/
./startMany.sh
```

## Interact with server

You can use netcat. For example:

```shell
netcat localhost 5555
```
