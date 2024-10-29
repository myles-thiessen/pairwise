from statistics import mean

import matplotlib
import matplotlib.pyplot as plt
import scipy.stats as st

from color_config import COLORS, MARKERS, LINESTYLES, TITLES, FONT, FONT_SIZE, LEGEND_POS, COLUMN_SPACING, BOX, \
    MARKER_SPACING, SPACING, YLABEL_PADDING, HANDLE_LENGTH, MARKER_SIZE
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
        plt.plot([promise_times[p_index], promise_times[p_index]], [cil, ciu], color=COLORS[labels[index]])
        p_index += 1

    print(f'{to_return}')
    return to_return


def plot_multiple(data, index):
    label = labels[index]
    plt.plot(promise_times, data, label=TITLES[label], color=COLORS[label], marker=MARKERS[label],
             linestyle=LINESTYLES[label], markersize=MARKER_SIZE)


def generate_plot():
    fig = plt.figure(figsize=(6.4, 4.8))
    fig.subplots_adjust(**SPACING)

    ax = plt.subplot(1, 1, 1)

    plt.xlabel("$\\alpha$ (ms)", fontsize=FONT_SIZE)
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
        plt.xlim(-7.5, 127.5)
        if first:
            plt.ylim(75, 167.5)
            plt.yticks([80, 100, 120, 140, 160])
        else:
            plt.ylim(-5, 87.5)
            plt.yticks([0, 40, 80])
        plt.xticks([0, 30, 60, 90, 120])

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
        eag_extracted_results = extract(["../results/experiment1/EAG"] * len(promise_times), file, first, 0)
        plot_multiple(eag_extracted_results, 0)

        print("DEL")
        del_extracted_results = extract(
            ["../results/experiment1/DEL-" + str(p) + "-" + clock_skew for p in promise_times], file, first, 1)
        plot_multiple(del_extracted_results, 1)

        print("PL")
        pl_extracted_results = extract(["../results/experiment1/PL-" + str(p) for p in promise_times], file, first,
                                       2)
        plot_multiple(pl_extracted_results, 2)

        print("PA")
        pa_extracted_results = extract(["../results/experiment1/PA-" + str(p) for p in promise_times], file, first,
                                       3)
        plot_multiple(pa_extracted_results, 3)

        if first:
            lgd = plt.legend(loc='upper center', bbox_to_anchor=LEGEND_POS,
                             ncol=5, columnspacing=COLUMN_SPACING, handletextpad=MARKER_SPACING, fontsize=FONT_SIZE,
                             frameon=False, handlelength=HANDLE_LENGTH)

        c += 1
    plt.savefig(f'experiment-1.svg', bbox_extra_artists=(lgd,), bbox_inches=BOX)


promise_times = [x for x in range(0, 121, 10)]
clock_skew = "40"

labels = ["EAG", "DEL", "PL", "PA"]

matplotlib.rc('font', **FONT)
matplotlib.rcParams['mathtext.fontset'] = 'stix'
generate_plot()
