from statistics import mean

import matplotlib
import matplotlib.pyplot as plt
import scipy.stats as st
from matplotlib.transforms import Bbox

from color_config import COLORS, MARKERS, LINESTYLES, TITLES, FONT, FONT_SIZE, LEGEND_POS, COLUMN_SPACING, BOX
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
             linestyle=LINESTYLES[label])


def generate_plot():
    fig = plt.figure(figsize=(6.4, 4.8))
    fig.subplots_adjust(hspace=.45)

    ax = plt.subplot(1, 1, 1)

    plt.xlabel("$\\alpha$ (ms)", fontsize=FONT_SIZE)
    plt.ylabel("Max Operation Latency (ms)", fontsize=FONT_SIZE)

    ax.spines['top'].set_color('none')
    ax.spines['bottom'].set_color('none')
    ax.spines['left'].set_color('none')
    ax.spines['right'].set_color('none')
    ax.tick_params(labelcolor='w', top=False, bottom=False, left=False, right=False)

    lgd = None
    c = 1
    for file in ["0", "0", "1", "2"]:

        first = c == 1

        plt.subplot(2, 2, c)
        plt.grid(axis='x', color='0.95')
        plt.grid(axis='y', color='0.95')
        plt.xlim(-7.5, 127.5)
        if first:
            plt.ylim(75, 167.5)
            plt.yticks([80, 100, 120, 140, 160])
        else:
            plt.ylim(-5, 87.5)
            plt.yticks([0, 20, 40, 60, 80])
        plt.xticks([0, 30, 60, 90, 120])

        if file == "0":
            if first:
                plt.title("RMW latency @ $\\ell$", fontsize=FONT_SIZE)
            else:
                plt.title("Read latency @ $\\ell$", fontsize=FONT_SIZE)
        elif file == "1":
            plt.title("Read latency @ $p$", fontsize=FONT_SIZE)
        else:
            plt.title("Read latency @ $q$", fontsize=FONT_SIZE)

        print("BHT")
        bht_extracted_results = extract(
            ["../results/figure7/BHT-" + str(p) + "-" + clock_skew for p in promise_times], file, first, 0)
        plot_multiple(bht_extracted_results, 0)

        print("US")
        us_extracted_results = extract(["../results/figure7/US-" + str(p) for p in promise_times], file, first,
                                       1)
        plot_multiple(us_extracted_results, 1)

        print("US2")
        us2_extracted_results = extract(["../results/figure7/US2-" + str(p) for p in promise_times], file, first,
                                        2)
        plot_multiple(us2_extracted_results, 2)

        if first:
            lgd = plt.legend(loc='upper center', bbox_to_anchor=LEGEND_POS,
                       ncol=5, columnspacing=COLUMN_SPACING, fontsize=FONT_SIZE)

        c += 1
    plt.savefig(f'figure7.svg', bbox_extra_artists=(lgd,), bbox_inches=BOX)

promise_times = [x for x in range(0, 121, 10)]
clock_skew = "40"

labels = ["BHT", "PL", "PA"]

matplotlib.rc('font', **FONT)
matplotlib.rcParams['mathtext.fontset'] = 'stix'
generate_plot()
