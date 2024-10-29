## Repository Structure

There are three folders:
- pairwise-YCSB (a fork of YCSB with a connector for this repo)
- code
- experiment-scripts
- plotting-scripts

The code folder contains our implementations of Leader Reads, Invalidation, Eager Stamping, Delayed Stamping, Pairwise-Leader, and Pairwise-All.

The experiment-script folder contains the bash scripts used to generate the results and the plotting-script folder contains the python scripts used to parse these results and generate the graphs.

## Figure to Experiment Translation

- Figure 8 = experiment_3
- Figure 9 = experiment_1
- Figure 10 = experiment_6
- Figure 11 Top = experiment_network_variability
- Figure 11 Bottom = experiment_follower_failure
- Figure 12 = experiment_2
- Figure 13 & Figure 14 = experiment_4
- Figure 15 = experiment_ycsb_realworld
- Second YCSB experiment in 8.4 text = experiment_ycsb_varyrmw

## Running the Experiments

To run experiments 1, 3, 6, network_variability, and follower_failure you need to do the following:

1. Start three machines inside the same local area network. We ran ours in the North Virginia AWS region and used c5.4xlarge instances. Each machine is assumed to have a user account named ubuntu with sudo permissions and be accessible with the same ssh-key. Furthermore, the master machine (pick any one of them) must store this ssh-key at /home/ubuntu/.ssh/myles.pem.
2. Clone this repository on all machines in the home directory /home/ubuntu.
3. Install Java 17 or later, and run mvn package install inside the code folder on all machines.
4. On the master machine, you need to fill in the "TODO"s inside experiment-scripts in the form IP:1099:1100:X where IP is the process' ip address and X is the unique id of this process (we used integers from 1 to 3).
5. Run experiment-scripts on the master machine to run that experiment. You should do this in a tmux session (or something equivalent like screen) as the experiments take a few hours to run.

To run experiment 2 the procedure is the same except the machines need to be in Montreal, North Virginia, and North California AWS regions to get matching results. Specifically, l (id 1) is in Montreal, p (id 2) is in North Virginia, and q (id 3) is in North California.

To run experiment 4 the procedure is the same except you need between 1 and 16 machines in the following AWS regions: Montreal, North Virginia, North California, Frankfurt, and Stockholm. For each x in 1, 2, 4, 8, 16 the processes variable in experiment_4.sh needs to be set to x and TOPOLOGY needs to be of the form "IP:1099:1100:1 ... IP:1099:1100:(x * 5)" where the first x entries in TOPOLOGY are the machines in Montreal, the second x entries are the machines in North Virginia, and so forth. While we do not provide a script for doing so, it is easy to generate the TOPOLOGY variable automatically by fetching the ip addresses of all machines through your cloud provider's API.

To run the YCSB experiments you need to:
1. run mvn package -Psource-run inside the pairwise-YCSB folder
2. On the master machine, fill in the "TODO"s in the same way as experiment 4 for 4 machines in the same AWS regions. The IPS TODO is just a list of the 20 machine's IPs.
3. Run experiment_ycsb_realworld.sh / experiment_ycsb_varyrmw.sh

## Plotting the Results

When you run the above experiments, they will create a fourth top level folder called results. Once you are done, run the associated python scripts in plotting-scripts on the machine you ran the experiment-scripts.

## Disclaimer

This is a research prototype and is made available solely for result reproducibility. 
