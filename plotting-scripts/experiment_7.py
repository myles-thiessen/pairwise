from statistics import mean

import matplotlib
import matplotlib.pyplot as plt
import scipy.stats as st
from matplotlib.transforms import Bbox

from color_config import MARKERS, LINESTYLES, COLORS, TITLES, FONT, FONT_SIZE, COLUMN_SPACING, LEGEND_POS, BOX, \
    MARKER_SPACING, SPACING, YLABEL_PADDING, HANDLE_LENGTH, MARKER_SIZE
from parse_trace import parse_results

runs = 5
variability_algos = ["PL-103", "PA-63", "PL-183", "PA-103"]
variability_labels = ["PL", "PA", "PL-P", "PA-P"]
failure_algos = ["PLFFT-103", "PAFFT-63"]
failure_labels = ["PL", "PA"]


def compute_variability(algo, process, write):
    if write:
        check = "WRITE"
    else:
        check = "READ"

    raw = []
    for run in range(1, runs + 1):
        data = []
        results = \
            parse_results(["../results/experiment-network-variability/" + algo + "/" + str(run)],
                          str(process) + ".txt")[0]

        for r in results:
            if r.opType != check:
                continue
            data.append(r.time)
        raw.append(data)

    final = []
    for i in range(0, len(raw[0])):
        sum = 0
        base = 0
        for run in range(1, runs + 1):
            sum += raw[base][i]
            base += 1
        final.append(sum / runs)

    return final


def compute_failure(algo, process, write):
    if write:
        check = "WRITE"
    else:
        check = "READ"

    raw = []
    for run in range(1, runs + 1):
        data = []
        results = \
            parse_results(["../results/experiment-follower-failure/" + algo + "/" + str(run)],
                          str(process) + ".txt")[0]

        for r in results:
            if r.opType != check:
                continue
            data.append(r.time)
        raw.append(data)

    final = []
    for i in range(0, len(raw[0])):
        sum = 0
        base = 0
        for run in range(1, runs + 1):
            sum += raw[base][i]
            base += 1
        final.append(sum / runs)

    return final


def plot_variability(process, write):
    plt.xticks([0, 5000, 10000, 15000], labels=["0", "5", "10", "15"])
    color_i = 0

    if write:
        plt.ylim(80, 200)
        plt.yticks([80, 110, 140, 170, 200])
    else:
        plt.ylim([-5, 105])
        plt.yticks([0, 25, 50, 75, 100])

    for algo in variability_algos:
        data = compute_variability(algo, process, write)
        label = variability_labels[color_i]
        plt.plot(range(0, len(data)), data, label=TITLES[label], color=COLORS[label], marker=MARKERS[label],
             linestyle=LINESTYLES[label], markersize=MARKER_SIZE, markevery=1650)
        color_i += 1


def plot_failure(process, write):
    plt.xticks([0, 5000, 10000, 15000], labels=["0", "5", "10", "15"])
    color_i = 0
    plt.ylim([1, 10 ** 4])

    for algo in failure_algos:
        data = compute_failure(algo, process, write)
        label = failure_labels[color_i]
        plt.plot(range(0, len(data)), data, label=TITLES[label], color=COLORS[label], marker=MARKERS[label],
             linestyle=LINESTYLES[label], markersize=MARKER_SIZE, markevery=1650)
        color_i += 1


def generate_plot():
    fig = plt.figure(figsize=(6.4, 4.8))
    fig.subplots_adjust(**SPACING)

    ax = plt.subplot(1, 1, 1)

    plt.xlabel("Time (sec)", fontsize=FONT_SIZE)
    plt.ylabel("Op Latency (ms)", fontsize=FONT_SIZE, labelpad=YLABEL_PADDING)

    ax.spines['top'].set_color('none')
    ax.spines['bottom'].set_color('none')
    ax.spines['left'].set_color('none')
    ax.spines['right'].set_color('none')
    ax.tick_params(labelcolor='w', top=False, bottom=False, left=False, right=False)

    lgd = None
    for c in [1, 2, 3, 4]:

        ax = plt.subplot(2, 2, c)
        ax.spines[['right', 'top']].set_visible(False)
        plt.grid(axis='x', color='0.9')
        plt.grid(axis='y', color='0.9')

        if c == 1:
            plt.title("RMWs @ $\ell$", fontsize=FONT_SIZE)

            plot_variability("0", True)

            ax.xaxis.set_major_formatter(plt.NullFormatter())
        elif c == 2:
            plt.title("Reads @ $p$", fontsize=FONT_SIZE)

            plot_variability("1", False)

            ax.xaxis.set_major_formatter(plt.NullFormatter())
        elif c == 3:
            plt.title("RMWs @ $\ell$", fontsize=FONT_SIZE)

            plot_failure("0", True)

            ax.set_yscale("log")
        elif c == 4:
            plt.title("Reads @ $q$", fontsize=FONT_SIZE)

            plot_failure("1", False)

            ax.set_yscale("log")

        if c == 1:
            lgd = plt.legend(loc='upper center', bbox_to_anchor=LEGEND_POS,
                             ncol=5, columnspacing=COLUMN_SPACING, handletextpad=MARKER_SPACING, fontsize=FONT_SIZE,
                             frameon=False, handlelength=HANDLE_LENGTH)

    plt.savefig(f'experiment-7.svg', bbox_extra_artists=(lgd,), bbox_inches=BOX)


matplotlib.rc('font', **FONT)
matplotlib.rcParams['mathtext.fontset'] = 'stix'
generate_plot()
