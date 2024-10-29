from statistics import mean

import matplotlib
import matplotlib.pyplot as plt
import scipy.stats as st

from color_config import MARKERS, LINESTYLES, COLORS, TITLES, FONT, FONT_SIZE, LEGEND_POS, COLUMN_SPACING, BOX
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
                ["../results/figure11/" + algo + "/" + w + "/" + str(r) for r in range(1, runs + 1)],
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
                 color=COLORS[label])
        index += 1


def generate_plot():
    fig = plt.figure(figsize=(6.4, 4.8))
    fig.subplots_adjust(hspace=.45)

    ax = plt.subplot(1, 1, 1)

    ax.spines['top'].set_color('none')
    ax.spines['bottom'].set_color('none')
    ax.spines['left'].set_color('none')
    ax.spines['right'].set_color('none')
    ax.tick_params(labelcolor='w', top=False, bottom=False, left=False, right=False)

    plt.xlabel("RMW Operations/sec", fontsize=FONT_SIZE)
    plt.ylabel("Average Operation Latency (ms)", fontsize=FONT_SIZE)

    lgd = None

    c = 1
    for file in ["0", "0", "1", "2"]:
        first = c == 1

        plt.subplot(2, 2, c)
        plt.grid(axis='x', color='0.95')
        plt.grid(axis='y', color='0.95')
        plt.xlim(15, 260)
        if first:
            plt.ylim(5, 140)
            plt.yticks([0, 25, 50, 75, 100, 125])
        else:
            plt.ylim(-5, 85)
            plt.yticks([0, 20, 40, 60, 80])
        plt.xticks([50, 100, 150, 200, 250])

        if file == "0":
            if first:
                plt.title("RMW latency @ $\\ell$", fontsize=FONT_SIZE)
            else:
                plt.title("Read latency @ $\\ell$", fontsize=FONT_SIZE)
        elif file == "1":
            plt.title("Read latency @ $p$", fontsize=FONT_SIZE)
        else:
            plt.title("Read latency @ $q$", fontsize=FONT_SIZE)

        plot(file, first)

        if first:
            lgd = plt.legend(loc='upper center', bbox_to_anchor=LEGEND_POS,
                       ncol=5, columnspacing=COLUMN_SPACING, fontsize=FONT_SIZE)

        c += 1

    plt.savefig(f'figure11.svg', bbox_extra_artists=(lgd,), bbox_inches=BOX)


algos = ["LEADER_READS", "CHT", "BHT-103-27", "US-103", "US2-63"]
wp = [".025", ".05", ".075", ".1", ".125", ".15", ".175", ".2", ".225", ".25"]
labels = ["LR", "CHT", "BHT", "PL", "PA"]
matplotlib.rc('font', **FONT)
matplotlib.rcParams['mathtext.fontset'] = 'stix'
generate_plot()
