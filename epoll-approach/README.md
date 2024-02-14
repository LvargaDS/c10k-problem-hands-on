## WIP project code is not finished!

## running in docker

This little example is self-contained and you can run it directly.

```shell
cd ..
mvn clean package
docker run -it -v ./:/c10k --memory-swap 1g --memory 1g openjdk:8-jre-slim /bin/bash
java -cp target/epoll-approach-1.0-SNAPSHOT.jar eu.digitalsystems.EpollApp
```

169000 listeners in 1GB or memory.
