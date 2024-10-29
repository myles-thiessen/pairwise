#!/bin/sh
L_IP="TODO"
P_IP="TODO"
Q_IP="TODO"
TOPOLOGY="$L_IP:1099:1100:1 $P_IP:1099:1100:2 $Q_IP:1099:1100:3"
ALGORITHMS="LEADER_READS CHT BHT-103-27 US-103 US2-63"
WRITE_PERCENTAGES=".025 .05 .075 .1 .125 .15 .175 .2 .225 .25"
cd ../code
for algorithm in $ALGORITHMS
do
	for wp in $WRITE_PERCENTAGES
	do
		for run in 1 2 3 4 5
		do
			echo Starting $algorithm with wp $wp for run $run
			mkdir -p ../results/figure11/$algorithm/$wp/$run
			mvn exec:java -Dexec.mainClass="mthiessen.experiments.Experiment2Master" -Dexec.args="$algorithm $wp $TOPOLOGY"
			mvn exec:java -Dexec.mainClass="mthiessen.experiments.extractors.ExperimentMetricsExtractor" -Dexec.args="figure11/$algorithm/$wp/$run $TOPOLOGY"
		done
	done
done
