#!/bin/sh
TOPOLOGY="TODO"
ALGORITHMS="LR EAG DEL-103-27 PL-103 PA-63"
WRITE_PERCENTAGES=".025 .05 .075 .1 .125 .15 .175 .2 .225 .25"
cd code
for algorithm in $ALGORITHMS
do
	for wp in $WRITE_PERCENTAGES
	do
		for run in 1 2 3 4 5
		do
			echo Starting $algorithm with wp $wp for run $run
			mkdir -p ../results/experiment2/$algorithm/$wp/$run
			mvn exec:java -Dexec.mainClass="mthiessen.experiments.masters.Experiment2Master" -Dexec.args="$algorithm $wp $TOPOLOGY"
			mvn exec:java -Dexec.mainClass="mthiessen.experiments.extractors.ExperimentMetricsExtractor" -Dexec.args="experiment2/$algorithm/$wp/$run $TOPOLOGY"
		done
	done
done

