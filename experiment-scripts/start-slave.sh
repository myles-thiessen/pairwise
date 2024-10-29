#!/bin/sh
cd code
mvn exec:java -Dexec.mainClass="mthiessen.experiments.ExperimentSlave" -Dexec.args="1099" -DargLine="-Xmx20G"
