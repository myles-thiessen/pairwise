#!/bin/sh
L_IP="TODO"
P_IP="TODO"
Q_IP="TODO"
TOPOLOGY="$L_IP:1099:1100:1 $P_IP:1099:1100:2 $Q_IP:1099:1100:3"
ALGORITHMS="PQL CHT BHT-110-50 US-110 US2-60"
CS="1 .6666666 .3333333 0"
LATENCIES="0 2.5 5 7.5 10 12.5 15 17.5 20 22.5 25 27.5 30 32.5 35 37.5 40 42.5 45 47.5 50"
cd ../code
for algorithm in $ALGORITHMS
do
	for c in $CS
	do
		for latency in $LATENCIES
		do
			for run in 1 2 3 4 5
			do
				echo Starting $algorithm with c $c and latency $latency for run $run
				mkdir -p ../results/figure6/$algorithm/$c/$latency/$run
				mvn exec:java -Dexec.mainClass="mthiessen.experiments.Experiment3Master" -Dexec.args="$algorithm $c $latency $TOPOLOGY"
				mvn exec:java -Dexec.mainClass="mthiessen.experiments.extractors.ExperimentMetricsExtractor" -Dexec.args="figure6/$algorithm/$c/$latency/$run $TOPOLOGY"
			done
		done
	done
done
