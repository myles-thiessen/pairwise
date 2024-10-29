#!/bin/sh
L_IP="TODO"
P_IP="TODO"
Q_IP="TODO"
TOPOLOGY="$L_IP:1099:1100:1 $P_IP:1099:1100:2 $Q_IP:1099:1100:3"
ALGORITHMS="PQL CHT BHT US US2"
LBS="0 4 8 12 16 20 24 28 32 36 40"
US_BASE_PROMISE_TIME="90"
US2_BASE_PROMISE_TIME="50"
BHT_BASE_PROMISE_TIME="90"
BASE_CLOCK_SKEW="40"
cd ../code

run() {
	for run in 1 2 3 4 5 
	do
       		echo Starting $algorithm_new for run $run
		mkdir -p ../results/figure8/$algorithm_new/$run
		mvn exec:java -Dexec.mainClass="mthiessen.experiments.Experiment1Master" -Dexec.args="$algorithm_new $lbs $TOPOLOGY"
		mvn exec:java -Dexec.mainClass="mthiessen.experiments.extractors.ExperimentMetricsExtractor" -Dexec.args="figure8/$algorithm_new/$run $TOPOLOGY"
	done
}

for algorithm in $ALGORITHMS
do
	for lbs in $LBS
	do
		if [ "$algorithm" = "BHT" ]; then
			promise_time="$(($BHT_BASE_PROMISE_TIME + $lbs))"
			clock_skew="$(($BASE_CLOCK_SKEW - $lbs))"
			algorithm_new="BHT-$promise_time-$clock_skew"
			run	
		elif [ "$algorithm" = "US" ]; then
			promise_time="$(($US_BASE_PROMISE_TIME + $lbs))"
			algorithm_new="US-$promise_time"
			run
       		elif [ "$algorithm" = "US2" ]; then
			promise_time="$(($US2_BASE_PROMISE_TIME + $lbs))"
			algorithm_new="US2-$promise_time"
			run
		else
			algorithm_new=$algorithm
			run
		fi
	done
done

