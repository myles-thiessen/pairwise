#!/bin/sh
processes="1" # set to 1, 2, 4, 8, 16 and update topology to be 5, 10, 20, 40, 80 instances, respectively.
TOPOLOGY="TODO"
ALGORITHMS="LEADER_READS CHT BHT-108-58 US-134 US2-92"
SPLITS="100-100-100-100-100"
DELAYS="10 20 40 80 160 320 640"
cd ../code
for algorithm in $ALGORITHMS
do
	for split in $SPLITS
	do
		for delay in $DELAYS
		do
			for run in 1 2 3 4 5 
			do
				echo Starting $algorithm with split $split and delay $delay for run $run
				mkdir -p ../results/figure12-and-13/$processes/$algorithm/$split/$delay/$run
				mvn exec:java -Dexec.mainClass="mthiessen.experiments.Experiment4Master" -Dexec.args="$algorithm $split $delay $TOPOLOGY"
				mvn exec:java -Dexec.mainClass="mthiessen.experiments.extractors.ExperimentMetricsExtractor" -Dexec.args="figure12-and-13/$processes/$algorithm/$split/$delay/$run $TOPOLOGY"
			done
		done
	done
done
