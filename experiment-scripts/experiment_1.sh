#!/bin/sh
TOPOLOGY="TODO"
ALGORITHMS="EAG DEL PL PA"
PROMISE_TIMES="0 10 20 30 40 50 60 70 80 90 100 110 120"
CLOCK_SKEW="40"
cd code

run() {
	for run in 1 2 3 4 5
	do
       	echo Starting $algorithm for run $run
		mkdir -p ../results/experiment1/$algorithm/$run
		mvn exec:java -Dexec.mainClass="mthiessen.experiments.masters.Experiment1Master" -Dexec.args="$algorithm 0 $TOPOLOGY"
		mvn exec:java -Dexec.mainClass="mthiessen.experiments.extractors.ExperimentMetricsExtractor" -Dexec.args="experiment1/$algorithm/$run $TOPOLOGY"
	done
}

for algorithm in $ALGORITHMS
do
	if [ "$algorithm" = "DEL" ]; then
		for promise_time in $PROMISE_TIMES
		do
			algorithm="DEL-$promise_time-$CLOCK_SKEW"
			run	
		done
	elif [ "$algorithm" = "PL" ]; then
		for promise_time in $PROMISE_TIMES
		do
			algorithm="PL-$promise_time"
			run
		done
       	elif [ "$algorithm" = "PA" ]; then
		for promise_time in $PROMISE_TIMES
		do
			algorithm="PA-$promise_time"
			run
		done
	else
		run
	fi
done
