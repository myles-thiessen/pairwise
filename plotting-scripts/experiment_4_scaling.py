from statistics import mean

import matplotlib

from color_config import MARKERS, LINESTYLES, COLORS, TITLES, FONT, FONT_SIZE, LEGEND_POS, COLUMN_SPACING, BOX, \
    MARKER_SPACING, HANDLE_LENGTH, SPACING, YLABEL_PADDING, MARKER_SIZE
from parse_trace import parse_results
import matplotlib.pyplot as plt
import scipy.stats as st
import seaborn as sns

runs = 5


def plot_agg(delay):
    scaling = [1, 2, 4, 8, 16]

    color_i = 0

    for algo in algos:
        data = []
        for i in scaling:
            average_over_runs = []

            raw_data = []

            for run in range(1, runs + 1):

                files = [x for x in range(0, i * 5)]

                print(files)

                sum_over_processes = 0

                for file in files:

                    results = parse_results(
                        ["../results/experiment4/" + str(i) + "/" + algo + "/100-100-100-100-100/" + delay + "/" + str(run)],
                        str(file) + ".txt")

                    ops = 0
                    for r in results[0]:
                        if r.opType == "OPS":
                            ops = r.time
                    throughput = (ops / 1_000_000) / 60
                    sum_over_processes += throughput
                    raw_data.append(ops)
                average_over_runs.append(sum_over_processes)
            m = mean(average_over_runs)
            data.append(m)
            (cil, ciu) = st.norm.interval(0.99, loc=m, scale=st.sem(average_over_runs))
            plt.plot([i, i], [cil, ciu], color=COLORS[labels[color_i]])
            print(raw_data)
            print(
                f'Algorithm: {algo} delay: {delay} Agg Throughput: {m}')
        label = labels[color_i]
        plt.plot(scaling, data, label=TITLES[label], marker=MARKERS[label],
                 linestyle=LINESTYLES[label],
                 color=COLORS[label], markersize=MARKER_SIZE)
        color_i += 1


def generate_plot():
    fig = plt.figure(figsize=(6.4, 4.8))
    fig.subplots_adjust(**SPACING)
    ax = plt.subplot(1, 1, 1)

    ax.spines['top'].set_color('none')
    ax.spines['bottom'].set_color('none')
    ax.spines['left'].set_color('none')
    ax.spines['right'].set_color('none')
    ax.tick_params(labelcolor='w', top=False, bottom=False, left=False, right=False)

    plt.xlabel("Processes per Region", fontsize=FONT_SIZE)
    plt.ylabel("Throughput (MOPS/sec)", fontsize=FONT_SIZE, labelpad=YLABEL_PADDING)

    lgd = None
    c = 1
    for interval in ["20", "40", "80", "160"]:

        first = c == 1

        ax = plt.subplot(2, 2, c)
        ax.spines[['right', 'top']].set_visible(False)
        plt.grid(axis='x', color='0.9')
        plt.grid(axis='y', color='0.9')

        if c == 1 or c == 2:
            ax.xaxis.set_major_formatter(plt.NullFormatter())

        plt.ylim(-25, 550)
        plt.yticks([0, 250, 500])
        plt.xticks([1, 4, 8, 16])

        plt.title(f"{interval}ms", fontsize=FONT_SIZE)

        plot_agg(interval)

        if first:
            lgd = plt.legend(loc='upper center', bbox_to_anchor=LEGEND_POS,
                             ncol=5, columnspacing=COLUMN_SPACING, handletextpad=MARKER_SPACING, fontsize=FONT_SIZE,
                             frameon=False, handlelength=HANDLE_LENGTH)

        c += 1

    plt.savefig(f'experiment-4-scaling.svg', bbox_extra_artists=(lgd,), bbox_inches=BOX)


algos = ["LEADER_READS", "CHT", "BHT-108-58", "US-134", "US2-92"]
labels = ["LR", "EAG", "DEL", "PL", "PA"]
matplotlib.rc('font', **FONT)
matplotlib.rcParams['mathtext.fontset'] = 'stix'
generate_plot()
