## Repository Structure

There are three folders:
- code
- experiment-scripts
- plotting-scripts

The code folder contains our implementations of Leader Reads, Invalidation, Eager Stamping, Delayed Stamping, Pairwise-Leader, and Pairwise-All.

The experiment-script folder contains the bash scripts used to generate the results and the plotting-script folder contains the python scripts used to parse these results and generate the graphs.

## Running the Experiments

To run the experiments for figures 6, 7, and 8 you need to do the following:

1. Start three machines inside the same local area network. We ran ours in the North Virginia AWS region and used c5.4xlarge instances. Each machine is assumed to have a user account named ubuntu with sudo permissions and be accessible with the same ssh-key. Furthermore, the master machine (pick any one of them) must store this ssh-key at /home/ubuntu/.ssh/myles.pem.
2. Clone this repository on all machines in the home directory /home/ubuntu.
3. Install Java 17 or later, and run mvn package inside the code folder on all machines.
4. On the master machine, you need to fill in the "TODO"s inside experiment-scripts/figureX.sh where X is 6, 7, and 8. These are the IP addresses of the of the three machines.
5. Run experiment-scripts/figureX.sh on the master machine to run the experiment for figure X. You should do this in a tmux session (or something equivalent like screen) as the experiments take a few hours to run.

To run the experiment for figure 11 the procedure is the same except the machines need to be in Montreal, North Virginia, and North California AWS regions to get matching results. Specifically, l (id 1) is in Montreal, p (id 2) is in North Virginia, and q (id 3) is in North California.

To run the experiment for figure 12 and 13 the procedure is the same except you need between 1 and 16 machines in the following AWS regions: Montreal, North Virginia, North California, Frankfurt, and Stockholm. For each x in 1, 2, 4, 8, 16 the processes variable in figure12-and-13.sh needs to be set to x and TOPOLOGY needs to be of the form "IP:1099:1100:1 ... IP:1099:1100:(x * 5)" where the first x entries in TOPOLOGY are the machines in Montreal, the second x entries are the machines in North Virginia, and so forth. While we do not provide a script for doing so, it is easy to generate the TOPOLOGY variable automatically by fetching the ip addresses of all machines through your cloud provider's API.

## Plotting the Results

When you run the above experiments, they will create a fourth top level folder called results. Once you are done, run the associated python scripts in plotting-scripts on the machine you ran the experiment-scripts in step 5. Specifically, if you ran experiment-scripts/figureX.sh then run plotting-scripts/figureX.py to generate the plot. The exception is figures 12 and 13 which have a single experiment script "figure12-and-13.sh" but have two plotting scripts "figure12.py" and "figure13.py". All of these python scripts generate a single svg in the same directory, i.e, "figureX.py" generates "figureX.svg". 

## Disclaimer

This is a research prototype and is made available solely for result reproducibility. 