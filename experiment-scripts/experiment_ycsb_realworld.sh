#!/bin/sh
IPS="TODO"
TOPOLOGY="TODO"
ALGORITHMS="LR EAG-RocksDBCD DEL-108-58-RocksDBCD PL-134-RocksDBCD PA-92-RocksDBCD"
UPDATES="100"
cd code
for algorithm in $ALGORITHMS
do
	for update in $UPDATES
	do
		for run in 1 2 3 4 5 
		do
			workload="workload-mixed-$update"
			echo Starting $algorithm with workload $workload for run $run
			mkdir -p ../results/experiment-ycsb-realworld/$algorithm/$workload/$run
			mvn exec:java -Dexec.mainClass="mthiessen.experiments.masters.ExperimentYCSBRealWorldMaster" -Dexec.args="$algorithm $workload $TOPOLOGY"
			count=1
			for ip in $IPS
			do
				rsync -Pav -e "ssh -i ~/.ssh/myles.pem" ubuntu@$ip:ycsb-write.dat ../results/experiment-ycsb-realworld/$algorithm/$workload/$run/$count-write.txt
				rsync -Pav -e "ssh -i ~/.ssh/myles.pem" ubuntu@$ip:ycsb-read.dat ../results/experiment-ycsb-realworld/$algorithm/$workload/$run/$count-read.txt
				count=$((count + 1))
			done
		done
	done
done
