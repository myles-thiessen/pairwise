from statistics import mean

import matplotlib

from color_config import MARKERS, LINESTYLES, COLORS, TITLES, FONT, FONT_SIZE, LEGEND_POS, COLUMN_SPACING, BOX
from parse_trace import parse_results
import matplotlib.pyplot as plt
import scipy.stats as st
import seaborn as sns

runs = 5


def plot_agg(delay):
    scaling = [1, 2, 4, 8, 16]

    plt.xticks(scaling)

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
                        ["../results/figure12-and-13/" + str(i) + "/" + algo + "/100-100-100-100-100/" + delay + "/" + str(run)],
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
                 color=COLORS[label])
        color_i += 1


def generate_plot():
    fig = plt.figure(figsize=(6.4, 4.8))
    fig.subplots_adjust(hspace=.45)

    ax = plt.subplot(1, 1, 1)

    ax.spines['top'].set_color('none')
    ax.spines['bottom'].set_color('none')
    ax.spines['left'].set_color('none')
    ax.spines['right'].set_color('none')
    ax.tick_params(labelcolor='w', top=False, bottom=False, left=False, right=False)

    plt.xlabel("Number of Processes per Region", fontsize=FONT_SIZE)
    plt.ylabel("Aggregate Read Throughput (MOPS/sec)", fontsize=FONT_SIZE)

    lgd = None
    c = 1
    for interval in ["20", "40", "80", "160"]:

        first = c == 1

        plt.subplot(2, 2, c)
        plt.grid(axis='x', color='0.95')
        plt.grid(axis='y', color='0.95')

        plt.ylim(-25, 550)
        plt.yticks([0, 125, 250, 375, 500])

        plt.title(f"RMW Interval {interval}ms", fontsize=FONT_SIZE)

        plot_agg(interval)

        if first:
            lgd = plt.legend(loc='upper center', bbox_to_anchor=LEGEND_POS,
                       ncol=5, columnspacing=COLUMN_SPACING, fontsize=FONT_SIZE)

        c += 1

    plt.savefig(f'figure12.svg', bbox_extra_artists=(lgd,), bbox_inches=BOX)


algos = ["LEADER_READS", "CHT", "BHT-108-58", "US-134", "US2-92"]
labels = ["LR", "CHT", "BHT", "PL", "PA"]
matplotlib.rc('font', **FONT)
matplotlib.rcParams['mathtext.fontset'] = 'stix'
generate_plot()