#!/bin/sh

mkdir -p jar/com/mergebase/util/
mkdir -p jar/META-INF
echo 'Main-Class: com.mergebase.util.Java2Json' > jar/META-INF/MANIFEST.MF
cp *.class jar/com/mergebase/util/
cd jar
zip -r json-mergebase-2019.09.09.jar .
mv json-mergebase-2019.09.09.jar ..
