#!/bin/sh
TOPOLOGY="TODO"
ALGORITHMS="EAG DEL PL PA"
LBS="0 4 8 12 16 20 24 28 32 36 40"
PL_BASE_PROMISE_TIME="90"
PA_BASE_PROMISE_TIME="50"
DEL_BASE_PROMISE_TIME="90"
BASE_CLOCK_SKEW="40"
cd code

run() {
	for run in 1 2 3 4 5 
	do
        echo Starting $algorithm_new for run $run
		mkdir -p ../results/experiment6/$algorithm_new/$run
		mvn exec:java -Dexec.mainClass="mthiessen.experiments.masters.Experiment1Master" -Dexec.args="$algorithm_new $lbs $TOPOLOGY"
		mvn exec:java -Dexec.mainClass="mthiessen.experiments.extractors.ExperimentMetricsExtractor" -Dexec.args="experiment6/$algorithm_new/$run $TOPOLOGY"
	done
}

for algorithm in $ALGORITHMS
do
	for lbs in $LBS
	do
		if [ "$algorithm" = "DEL" ]; then
			promise_time="$(($DEL_BASE_PROMISE_TIME + $lbs))"
			clock_skew="$(($BASE_CLOCK_SKEW - $lbs))"
			algorithm_new="DEL-$promise_time-$clock_skew"
			run	
		elif [ "$algorithm" = "PL" ]; then
			promise_time="$(($PL_BASE_PROMISE_TIME + $lbs))"
			algorithm_new="PL-$promise_time"
			run
       		elif [ "$algorithm" = "PA" ]; then
			promise_time="$(($PA_BASE_PROMISE_TIME + $lbs))"
			algorithm_new="PA-$promise_time"
			run
		else
			algorithm_new=$algorithm
			run
		fi
	done
done
