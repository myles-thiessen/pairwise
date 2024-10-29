#!/bin/sh
TOPOLOGY="TODO"
ALGORITHMS="EAG DEL-110-50 PL-110 PA-60"
CS="1 .6666666 .3333333 0"
LATENCIES="0 2.5 5 7.5 10 12.5 15 17.5 20 22.5 25 27.5 30 32.5 35 37.5 40 42.5 45 47.5 50"
cd code
for algorithm in $ALGORITHMS
do
	for c in $CS
	do
		for latency in $LATENCIES
		do
			for run in 1 2 3 4 5 
			do
				echo Starting $algorithm with c $c and latency $latency for run $run
				mkdir -p ../results/experiment3/$algorithm/$c/$latency/$run
				mvn exec:java -Dexec.mainClass="mthiessen.experiments.masters.Experiment3Master" -Dexec.args="$algorithm $c $latency $TOPOLOGY"
				mvn exec:java -Dexec.mainClass="mthiessen.experiments.extractors.ExperimentMetricsExtractor" -Dexec.args="experiment3/$algorithm/$c/$latency/$run $TOPOLOGY"
			done
		done
	done
done
