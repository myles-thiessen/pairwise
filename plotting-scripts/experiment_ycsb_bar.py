from statistics import mean

import matplotlib
import matplotlib.pyplot as plt
import scipy.stats as st
from numpy import transpose

from color_config import MARKERS, LINESTYLES, COLORS, TITLES, FONT, FONT_SIZE, COLUMN_SPACING, BOX, SPACING, \
    YLABEL_PADDING
from parse_trace import parse_ycsb_throughput

runs = 5
processes = 20


def compute(algo):
    data = []
    for run in range(1, runs + 1):
        agg_throughput = 0
        for j in range(1, processes + 1):
            write = parse_ycsb_throughput(
                "../results/experiment-ycsb-realworld/" + algo + "/workload-mixed-100" + "/" + str(run) + "/" + str(j) + "-write.txt")
            read = parse_ycsb_throughput(
                "../results/experiment-ycsb-realworld/" + algo + "/workload-mixed-100" + "/" + str(run) + "/" + str(j) + "-read.txt")
            agg_throughput += write + read
        data.append(agg_throughput / 1000)
    m = mean(data)
    (cil, ciu) = st.norm.interval(0.99, loc=m, scale=st.sem(data))
    print(f'{algo} {m} {cil} {ciu}')
    return m, cil, ciu


def plot_agg():
    color_i = 0

    l = []
    d = []
    c = []
    yerr = []
    for algo in algos:
        label = labels[color_i]
        (m, cil, ciu) = compute(algo)
        color = COLORS[label]
        l.append(label)
        d.append(m)
        c.append(color)
        yerr.append((m - cil, ciu - m))
        color_i += 1
    plt.gca().grid(zorder=0)
    plt.bar(l, d, color=c, yerr=transpose(yerr).tolist(), ecolor=c, zorder=3)


def generate_plot():
    fig = plt.figure(figsize=(6.4, 4.8))
    fig.subplots_adjust(**SPACING)
    plt.grid(axis='x', color='0.9')
    plt.grid(axis='y', color='0.9')
    plt.gca().spines[['right', 'top']].set_visible(False)

    plt.ylabel("Throughput (KOPS/sec)", fontsize=FONT_SIZE, labelpad=YLABEL_PADDING)

    plot_agg()

    plt.savefig(f'experiment-ycsb-bar.svg', bbox_inches=BOX)


algos = ["LR", "EAG-RocksDBCD", "DEL-108-58-RocksDBCD", "PL-134-RocksDBCD", "PA-92-RocksDBCD"]
labels = ["LR", "EAG", "DEL", "PL", "PA"]
matplotlib.rc('font', **FONT)
matplotlib.rcParams['mathtext.fontset'] = 'stix'
generate_plot()
