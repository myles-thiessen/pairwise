#!/bin/sh
TOPOLOGY="TODO"
ALGORITHMS="PL-103 PL-183 PA-63 PA-103"
cd code
for algorithm in $ALGORITHMS
do
	for run in 1 2 3 4 5 
	do
		echo Starting $algorithm with processes $PROCESSES for run $run
		mkdir -p ../results/experiment-network-variability/$algorithm/$run
		mvn exec:java -Dexec.mainClass="mthiessen.experiments.masters.ExperimentNetworkVariabilityMaster" -Dexec.args="$algorithm $TOPOLOGY"
		mvn exec:java -Dexec.mainClass="mthiessen.experiments.extractors.ExperimentMetricsExtractor" -Dexec.args="experiment-network-variability/$algorithm/$run $TOPOLOGY"
	done
done
