from statistics import mean

import matplotlib
import matplotlib.pyplot as plt
import scipy.stats as st

from color_config import MARKERS, LINESTYLES, COLORS, TITLES, FONT, FONT_SIZE, COLUMN_SPACING, BOX, SPACING, LEGEND_POS, \
    MARKER_SPACING, HANDLE_LENGTH, YLABEL_PADDING, MARKER_SIZE
from parse_trace import parse_results

runs = 5


def compute(scaling, algo, base, frequency):
    data = []

    for i in frequency:
        average_over_runs = []

        for run in range(1, runs + 1):

            files = []

            for b in base:
                for j in range(0, scaling):
                    files.append(b + j)

            sum_over_processes = 0

            for file in files:

                results = parse_results(["../results/experiment4/" + str(
                    scaling) + "/" + algo + "/100-100-100-100-100/" + str(i) + "/" + str(run)], str(file) + ".txt")

                ops = 0
                for r in results[0]:
                    if r.opType == "OPS":
                        ops = r.time
                time_in_sec = 60
                throughput = (ops / 1_000_000) / time_in_sec
                sum_over_processes += throughput
            average_over_runs.append(sum_over_processes)
        m = mean(average_over_runs)
        data.append(m)
    return data


def plot_agg(scaling):
    base = [0, 16, 32, 48, 64]
    frequency = [10, 20, 40, 80, 160, 320, 640]
    plt.xticks(ticks=range(0, len(frequency)), labels=[str(x) for x in frequency])

    color_i = 0

    for algo in algos:
        data_not_normalized = compute(scaling, algo, base, frequency)
        data = data_not_normalized
        label = labels[color_i]
        plt.plot(range(0, len(frequency)), data, label=TITLES[label], marker=MARKERS[label],
                 linestyle=LINESTYLES[label],
                 color=COLORS[label], markersize=MARKER_SIZE)
        color_i += 1


def generate_plot():
    fig = plt.figure(figsize=(6.4, 4.8))
    fig.subplots_adjust(**SPACING)
    plt.grid(axis='x', color='0.9')
    plt.grid(axis='y', color='0.9')
    plt.gca().spines[['right', 'top']].set_visible(False)

    plt.title("16 Processes per Region", fontsize=FONT_SIZE)
    plt.xlabel("RMW Interval (ms)", fontsize=FONT_SIZE)
    plt.ylabel("Throughput (MOPS/sec)", fontsize=FONT_SIZE, labelpad=8)

    plot_agg(16)

    lgd = plt.legend(loc='upper center', bbox_to_anchor=(.4, 1.305),
                     ncol=5, columnspacing=COLUMN_SPACING, handletextpad=MARKER_SPACING, fontsize=FONT_SIZE,
                     frameon=False, handlelength=HANDLE_LENGTH)

    plt.savefig(f'experiment-4-vary-writes.svg', bbox_extra_artists=(lgd,), bbox_inches=BOX)


algos = ["LEADER_READS", "CHT", "BHT-108-58", "US-134", "US2-92"]
labels = ["LR", "EAG", "DEL", "PL", "PA"]
matplotlib.rc('font', **FONT)
matplotlib.rcParams['mathtext.fontset'] = 'stix'
generate_plot()
