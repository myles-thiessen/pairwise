from statistics import mean

import matplotlib
import matplotlib.pyplot as plt
import scipy.stats as st
from matplotlib.transforms import Bbox

from color_config import MARKERS, LINESTYLES, COLORS, TITLES, FONT, FONT_SIZE, COLUMN_SPACING, LEGEND_POS, BOX, \
    MARKER_SPACING, SPACING, YLABEL_PADDING, HANDLE_LENGTH, MARKER_SIZE
from parse_trace import parse_results

runs = 5
algos = ["CHT", "DEL-110-50", "PL-110", "PA-60"]
labels = ["EAG", "DEL", "PL", "PA"]


def plot(c, latencies):
    for file in ["1"]:

        color_i = 0

        for algo in algos:

            plots = []

            for latency in latencies:
                results = parse_results(
                    ["../results/experiment3/" + algo + "/" + (str(c).removeprefix("0") if c != 0 else str(
                        0)) + "/" + latency + "/" + str(r) for r in range(1, runs + 1)], file + ".txt")

                average_over_runs = []

                # for each run
                for result in results:
                    run_avg = []
                    for r in result:
                        if r.opType == "WRITE":
                            continue

                        time = r.time
                        run_avg.append(time)
                    if len(run_avg) > 0:
                        average_over_runs.append(max(run_avg))
                m = mean(average_over_runs)
                plots.append(m)
                (cil, ciu) = st.norm.interval(0.99, loc=m, scale=st.sem(average_over_runs))
                plt.plot([float(latency), float(latency)], [cil, ciu], color=COLORS[labels[color_i]])
            label = labels[color_i]
            plt.plot([float(w) for w in latencies], plots, label=TITLES[label], marker=MARKERS[label],
                     linestyle=LINESTYLES[label],
                     color=COLORS[label], markersize=MARKER_SIZE)
            color_i += 1


def generate_plot():
    fig = plt.figure(figsize=(6.4, 4.8))
    fig.subplots_adjust(**SPACING)

    ax = plt.subplot(1, 1, 1)

    plt.xlabel("$x$", fontsize=FONT_SIZE)
    plt.ylabel("Max Read Latency @ $p$ (ms)", fontsize=FONT_SIZE, labelpad=YLABEL_PADDING)

    ax.spines['top'].set_color('none')
    ax.spines['bottom'].set_color('none')
    ax.spines['left'].set_color('none')
    ax.spines['right'].set_color('none')
    ax.tick_params(labelcolor='w', top=False, bottom=False, left=False, right=False)

    lgd = None
    cntr = 1
    for c in [0, .3333333, .6666666, 1]:

        first = cntr == 1

        ax = plt.subplot(2, 2, cntr)
        ax.spines[['right', 'top']].set_visible(False)
        plt.grid(axis='x', color='0.9')
        plt.grid(axis='y', color='0.9')
        plt.ylim(-2.5, 110)
        plt.yticks([0, 25, 50, 75, 100])

        max_y = int(50 / (1 + c))

        plt.xlim(-2.5, 50 + 2.5)
        plt.xticks([0, 10, 20, 30, 40, 50])

        if c == 1:
            plt.title("$c = 1$", fontsize=FONT_SIZE)
        elif c == .3333333:
            plt.title("$c = 1/3$", fontsize=FONT_SIZE)
            ax.xaxis.set_major_formatter(plt.NullFormatter())
        elif c == .6666666:
            plt.title("$c = 2/3$", fontsize=FONT_SIZE)
        elif c == 0:
            plt.title("$c = 0$", fontsize=FONT_SIZE)
            ax.xaxis.set_major_formatter(plt.NullFormatter())

        latencies = [(str(x / 2).removesuffix(".0") if int(x / 2) != "0.0" else "0") for x in
                     range(0, 2 * max_y + 5, 10)]
        print(latencies)

        plot(c, latencies)

        if first:
            lgd = plt.legend(loc='upper center', bbox_to_anchor=LEGEND_POS,
                             ncol=5, columnspacing=COLUMN_SPACING, handletextpad=MARKER_SPACING, fontsize=FONT_SIZE,
                             frameon=False, handlelength=HANDLE_LENGTH)

        cntr += 1

    plt.savefig(f'experiment-3.svg', bbox_extra_artists=(lgd,), bbox_inches=BOX)


matplotlib.rc('font', **FONT)
matplotlib.rcParams['mathtext.fontset'] = 'stix'
generate_plot()
