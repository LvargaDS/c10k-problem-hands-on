#!/bin/bash
for i in {2000..4000}
do
   echo "start $i"
   java -cp target/cgi-approach-1.0-SNAPSHOT.jar eu.digitalsystems.CGILikeApp $i &
done