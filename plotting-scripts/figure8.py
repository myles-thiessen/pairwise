from statistics import mean

import matplotlib
import matplotlib.pyplot as plt
import scipy.stats as st

from color_config import COLORS, MARKERS, LINESTYLES, TITLES, FONT, FONT_SIZE, BOX, COLUMN_SPACING, LEGEND_POS
from parse_trace import parse_results

runs = 5


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
             linestyle=LINESTYLES[label])


def generate_plot():
    fig = plt.figure(figsize=(6.4, 4.8))
    fig.subplots_adjust(hspace=.45)

    ax = plt.subplot(1, 1, 1)

    plt.xlabel("For all processes $p$ and $q$, $\\delta^{\\min}_{pq} = \\frac{x}{100} \\cdot \\delta_{pq}$ (ms)",
               fontsize=FONT_SIZE)
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
        plt.xlim(-7.5, 107.5)
        if first:
            plt.ylim(77.5, 135)
            plt.yticks([80, 95, 110, 125])
        else:
            plt.ylim(-5, 90)
            plt.yticks([0, 20, 40, 60, 80])
        plt.xticks([0, 20, 40, 60, 80, 100])

        if file == "0":
            if first:
                plt.title("RMW latency @ $\\ell$", fontsize=FONT_SIZE)
            else:
                plt.title("Read latency @ $\\ell$", fontsize=FONT_SIZE)
        elif file == "1":
            plt.title("Read latency @ $p$", fontsize=FONT_SIZE)
        else:
            plt.title("Read latency @ $q$", fontsize=FONT_SIZE)

        print("PQL")
        pql_extracted_results = extract(["../results/figure8/PQL"] * len(lbps), file, first, 0)
        plot_multiple(pql_extracted_results, 0)

        print("CHT")
        pql_extracted_results = extract(["../results/figure8/CHT"] * len(lbps), file, first, 1)
        plot_multiple(pql_extracted_results, 1)

        print("BHT")
        bht_extracted_results = extract(
            ["../results/figure8/BHT-" + str(int(base_bht_promise_time + lbp * 40)) + "-" + str(int(
                base_clock_skew - lbp * 40)) for lbp in lbps], file, first, 2)
        plot_multiple(bht_extracted_results, 2)

        print("US")
        us_extracted_results = extract(
            ["../results/figure8/US-" + str(int(base_us_promise_time + lbp * 40)) for lbp in lbps], file, first,
            3)
        plot_multiple(us_extracted_results, 3)

        print("US2")
        us2_extracted_results = extract(
            ["../results/figure8/US2-" + str(int(base_us2_promise_time + lbp * 40)) for lbp in lbps], file,
            first,
            4)
        plot_multiple(us2_extracted_results, 4)

        if first:
            lgd = plt.legend(loc='upper center', bbox_to_anchor=LEGEND_POS,
                       ncol=5, columnspacing=COLUMN_SPACING, fontsize=FONT_SIZE)

        c += 1

    plt.savefig(f'figure8.svg', bbox_extra_artists=(lgd,), bbox_inches=BOX)


lbps = [0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1]
base_bht_promise_time = 90
base_us_promise_time = 90
base_us2_promise_time = 50
base_clock_skew = 40

labels = ["PQL", "CHT", "BHT", "PL", "PA"]

matplotlib.rc('font', **FONT)
matplotlib.rcParams['mathtext.fontset'] = 'stix'
generate_plot()
