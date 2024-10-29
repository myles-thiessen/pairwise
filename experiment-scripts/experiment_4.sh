#!/bin/sh
# configure based on how many processes per region there are (1, 2, 4, 8, or 16)
processes="1"
TOPOLOGY="TODO" 
ALGORITHMS="EAG LR DEL-108-58 PL-134 PA-92"
SPLITS="100-100-100-100-100"
DELAYS="10 20 40 80 160 320 640"
cd code
for algorithm in $ALGORITHMS
do
	for split in $SPLITS
	do
		for delay in $DELAYS
		do
			for run in 1 2 3 4 5 
			do
				echo Starting $algorithm with split $split and delay $delay for run $run
				mkdir -p ../results/experiment4/$processes/$algorithm/$split/$delay/$run
				mvn exec:java -Dexec.mainClass="mthiessen.experiments.Experiment4Master" -Dexec.args="$algorithm $split $delay $TOPOLOGY"
				mvn exec:java -Dexec.mainClass="mthiessen.experiments.extractors.ExperimentMetricsExtractor" -Dexec.args="experiment4/$processes/$algorithm/$split/$delay/$run $TOPOLOGY"
			done
		done
	done
done
