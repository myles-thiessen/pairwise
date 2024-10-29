import seaborn as sns
from matplotlib.transforms import Bbox

LABELS = ["LR", "PQL", "CHT", "BHT", "PL", "PA"]

c = sns.color_palette("muted")
colors = [c[0], c[6], c[1], c[2], c[3], c[4]]

TITLES = {
    LABELS[0]: "LR",
    LABELS[1]: "INV",
    LABELS[2]: "EAG",
    LABELS[3]: "DEL",
    LABELS[4]: "PL",
    LABELS[5]: "PA",
}

COLORS = {
    LABELS[0]: c[0],
    LABELS[1]: c[5],
    LABELS[2]: c[1],
    LABELS[3]: c[2],
    LABELS[4]: c[3],
    LABELS[5]: c[4],
}

LINESTYLES = {
    LABELS[0]: "solid",
    LABELS[1]: "dotted",
    LABELS[2]: "dashed",
    LABELS[3]: "solid",
    LABELS[4]: "dotted",
    LABELS[5]: "dashed"
}

MARKERS = {
    LABELS[0]: "o",
    LABELS[1]: "P",
    LABELS[2]: "^",
    LABELS[3]: ">",
    LABELS[4]: "D",
    LABELS[5]: "X"
}
FONT_SIZE = 16

FONT = {'family': 'Linux Libertine O',
        'size': FONT_SIZE}

LEGEND_POS = (1, 1.6)
COLUMN_SPACING = 0.8
BOX = Bbox([[0, -0.2], [6, 5.2]])
