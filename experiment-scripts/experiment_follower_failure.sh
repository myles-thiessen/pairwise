#!/bin/sh
# all the processes
TOPOLOGY="TODO"
# only the processes that aren't killed (the dead one won't reply)
TOPOLOGY2="TODO"
ALGORITHMS="PLFFT-103 PAFFT-63"
cd code
for algorithm in $ALGORITHMS
do
	for run in 1 2 3 4 5 
	do
		echo Starting $algorithm with processes $PROCESSES for run $run
		mkdir -p ../results/experiment-follower-failure/$algorithm/$run
		mvn exec:java -Dexec.mainClass="mthiessen.experiments.masters.ExperimentFollowerFailure" -Dexec.args="$algorithm $TOPOLOGY"
		mvn exec:java -Dexec.mainClass="mthiessen.experiments.extractors.ExperimentMetricsExtractor" -Dexec.args="experiment-follower-failure/$algorithm/$run $TOPOLOGY2"
	done
done

