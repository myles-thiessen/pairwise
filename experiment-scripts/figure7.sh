#!/bin/sh
L_IP="TODO"
P_IP="TODO"
Q_IP="TODO"
TOPOLOGY="$L_IP:1099:1100:1 $P_IP:1099:1100:2 $Q_IP:1099:1100:3"
ALGORITHMS="BHT US US2"
PROMISE_TIMES="0 10 20 30 40 50 60 70 80 90 100 110 120"
CLOCK_SKEW="40"
cd ../code

run() {
	for run in 1 2 3 4 5 
	do
        	echo Starting $algorithm for run $run
		mkdir -p ../results/figure7/$algorithm/$run
		mvn exec:java -Dexec.mainClass="mthiessen.experiments.Experiment1Master" -Dexec.args="$algorithm 0 $TOPOLOGY"
		mvn exec:java -Dexec.mainClass="mthiessen.experiments.extractors.ExperimentMetricsExtractor" -Dexec.args="figure7/$algorithm/$run $TOPOLOGY"
	done
}

for algorithm in $ALGORITHMS
do
	if [ "$algorithm" = "BHT" ]; then
		for promise_time in $PROMISE_TIMES
		do
			algorithm="BHT-$promise_time-$CLOCK_SKEW"
			run	
		done
	elif [ "$algorithm" = "US" ]; then
		for promise_time in $PROMISE_TIMES
		do
			algorithm="US-$promise_time"
			run
		done
       	elif [ "$algorithm" = "US2" ]; then
		for promise_time in $PROMISE_TIMES
		do
			algorithm="US2-$promise_time"
			run
		done
	else
		run
	fi
done
