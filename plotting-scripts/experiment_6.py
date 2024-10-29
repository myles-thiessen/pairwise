from statistics import mean

import matplotlib
import matplotlib.pyplot as plt
import scipy.stats as st
from matplotlib.transforms import Bbox

from color_config import COLORS, MARKERS, LINESTYLES, TITLES, FONT, FONT_SIZE, BOX, COLUMN_SPACING, LEGEND_POS, SPACING, \
    YLABEL_PADDING, MARKER_SPACING, HANDLE_LENGTH, MARKER_SIZE
from parse_trace import parse_results

runs = 5


# This experiment is the one where there is a single write and reads every 1 ms.
def extract(directories, file, write, index):
    if write:
        check = "WRITE"
    else:
        check = "READ"

    p_index = 0

    to_return = []

    for dir in directories:
        results = parse_results([dir + "/" + str(run) for run in range(1, runs + 1)], file + ".txt")

        average_over_runs = []
        for result in results:
            data = []
            for r in result:
                if r.opType != check:
                    continue

                time = r.time
                data.append(time)
            average_over_runs.append(max(data))

        m = mean(average_over_runs)
        to_return.append(m)
        (cil, ciu) = st.norm.interval(0.99, loc=m, scale=st.sem(average_over_runs))
        plt.plot([int(lbps[p_index] * 100), int(lbps[p_index] * 100)], [cil, ciu], color=COLORS[labels[index]])
        p_index += 1

    print(f'{to_return}')
    return to_return


def plot_multiple(data, index):
    label = labels[index]
    plt.plot([int(lbp * 100) for lbp in lbps], data, label=TITLES[label], color=COLORS[label], marker=MARKERS[label],
             linestyle=LINESTYLES[label], markersize=MARKER_SIZE)


def generate_plot():
    fig = plt.figure(figsize=(6.4, 4.8))
    fig.subplots_adjust(**SPACING)

    ax = plt.subplot(1, 1, 1)

    plt.xlabel("$x$ ($\\forall p q\\ \\delta^{\\min}_{pq} = \\frac{x}{100} \\cdot \\delta_{pq}$)", fontsize=FONT_SIZE)
    plt.ylabel("Max Op Latency (ms)", fontsize=FONT_SIZE, labelpad=YLABEL_PADDING)

    ax.spines['top'].set_color('none')
    ax.spines['bottom'].set_color('none')
    ax.spines['left'].set_color('none')
    ax.spines['right'].set_color('none')
    ax.tick_params(labelcolor='w', top=False, bottom=False, left=False, right=False)

    lgd = None
    c = 1
    for file in ["0", "0", "1", "2"]:

        first = c == 1

        ax = plt.subplot(2, 2, c)
        ax.spines[['right', 'top']].set_visible(False)
        plt.grid(axis='x', color='0.9')
        plt.grid(axis='y', color='0.9')
        plt.xlim(-7.5, 107.5)
        if first:
            plt.ylim(77.5, 137.5)
            plt.yticks([80, 90, 100, 110, 120, 130])
        else:
            plt.ylim(-5, 90)
            plt.yticks([0, 20, 40, 60, 80])
        plt.xticks([0, 25, 50, 75, 100])

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

        print("EAG")
        pql_extracted_results = extract(["../results/experiment6/EAG"] * len(lbps), file, first, 0)
        plot_multiple(pql_extracted_results, 0)

        print("DEL")
        bht_extracted_results = extract(
            ["../results/experiment6/DEL-" + str(int(base_del_promise_time + lbp * 40)) + "-" + str(int(
                base_clock_skew - lbp * 40)) for lbp in lbps], file, first, 1)
        plot_multiple(bht_extracted_results, 1)

        print("PL")
        us_extracted_results = extract(
            ["../results/experiment6/PL-" + str(int(base_pl_promise_time + lbp * 40)) for lbp in lbps], file, first,
            2)
        plot_multiple(us_extracted_results, 2)

        print("PA")
        us2_extracted_results = extract(
            ["../results/experiment6/PA-" + str(int(base_pa_promise_time + lbp * 40)) for lbp in lbps], file,
            first,
            3)
        plot_multiple(us2_extracted_results, 3)

        if first:
            lgd = plt.legend(loc='upper center', bbox_to_anchor=LEGEND_POS,
                             ncol=5, columnspacing=COLUMN_SPACING, handletextpad=MARKER_SPACING, fontsize=FONT_SIZE,
                             frameon=False, handlelength=HANDLE_LENGTH)

        c += 1

    plt.savefig(f'experiment-6.svg', bbox_extra_artists=(lgd,), bbox_inches=BOX)


lbps = [0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1]
base_del_promise_time = 90
base_pl_promise_time = 90
base_pa_promise_time = 50
base_clock_skew = 40

labels = ["EAG", "DEL", "PL", "PA"]

matplotlib.rc('font', **FONT)
matplotlib.rcParams['mathtext.fontset'] = 'stix'
generate_plot()
