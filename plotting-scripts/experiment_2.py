from statistics import mean

import matplotlib
import matplotlib.pyplot as plt
import scipy.stats as st

from color_config import MARKERS, LINESTYLES, COLORS, TITLES, FONT, FONT_SIZE, LEGEND_POS, COLUMN_SPACING, BOX, \
    YLABEL_PADDING, SPACING, MARKER_SPACING, HANDLE_LENGTH, MARKER_SIZE
from parse_trace import parse_results

runs = 5


def plot(file, write):
    if write:
        check = "WRITE"
    else:
        check = "READ"

    index = 0

    for algo in algos:

        plots = []

        for w in wp:
            results = parse_results(
                ["../results/experiment2/" + algo + "/" + w + "/" + str(r) for r in range(1, runs + 1)],
                file + ".txt")

            average_over_runs = []

            # for each run
            for result in results:
                run_avg = []
                for r in result:
                    if r.opType != check:
                        continue

                    time = r.time
                    run_avg.append(time)
                if len(run_avg) > 0:
                    average_over_runs.append(mean(run_avg))
            m = mean(average_over_runs)
            plots.append(m)
            (cil, ciu) = st.norm.interval(0.99, loc=m, scale=st.sem(average_over_runs))
            plt.plot([int(1000 * float(w)), int(1000 * float(w))], [cil, ciu], color=COLORS[labels[index]])

        label = labels[index]
        print(f'{algo}: {plots}')
        plt.plot([int(1000 * float(w)) for w in wp], plots, label=TITLES[label], marker=MARKERS[label],
                 linestyle=LINESTYLES[label],
                 color=COLORS[label], markersize=MARKER_SIZE)
        index += 1


def generate_plot():
    fig = plt.figure(figsize=(6.4, 4.8))
    SPACING['right'] = .905
    fig.subplots_adjust(**SPACING)

    ax = plt.subplot(1, 1, 1)

    ax.spines['top'].set_color('none')
    ax.spines['bottom'].set_color('none')
    ax.spines['left'].set_color('none')
    ax.spines['right'].set_color('none')
    ax.tick_params(labelcolor='w', top=False, bottom=False, left=False, right=False)

    plt.xlabel("RMWs/sec", fontsize=FONT_SIZE)
    plt.ylabel("Avg Op Latency (ms)", fontsize=FONT_SIZE, labelpad=YLABEL_PADDING)

    lgd = None

    c = 1
    for file in ["0", "0", "1", "2"]:
        first = c == 1

        ax = plt.subplot(2, 2, c)
        ax.spines[['right', 'top']].set_visible(False)
        plt.grid(axis='x', color='0.9')
        plt.grid(axis='y', color='0.9')
        plt.xlim(15, 260)
        if first:
            plt.ylim(5, 140)
            plt.yticks([0, 65, 130])
        else:
            plt.ylim(-5, 85)
            plt.yticks([0, 40, 80])
        plt.xticks([50, 150, 250])

        if file == "0":
            if first:
                plt.title("RMWs @ $\\ell$", fontsize=FONT_SIZE)
            else:
                plt.title("Reads @ $\\ell$", fontsize=FONT_SIZE)
            ax.xaxis.set_major_formatter(plt.NullFormatter())
        elif file == "1":
            plt.title("Reads @ $p$", fontsize=FONT_SIZE)
        else:
            plt.title("Reads @ $q$", fontsize=FONT_SIZE)

        plot(file, first)

        if first:
            lgd = plt.legend(loc='upper center', bbox_to_anchor=LEGEND_POS,
                             ncol=5, columnspacing=COLUMN_SPACING, handletextpad=MARKER_SPACING, fontsize=FONT_SIZE,
                             frameon=False, handlelength=HANDLE_LENGTH)

        c += 1

    plt.savefig(f'experiment-2.svg', bbox_extra_artists=(lgd,), bbox_inches=BOX)


algos = ["LR", "EAG", "DEL-103-27", "PL-103", "PA-63"]
wp = [".025", ".05", ".075", ".1", ".125", ".15", ".175", ".2", ".225", ".25"]
labels = ["LR", "EAG", "DEL", "PL", "PA"]
matplotlib.rc('font', **FONT)
matplotlib.rcParams['mathtext.fontset'] = 'stix'
generate_plot()
