import seaborn as sns
from matplotlib.transforms import Bbox

LABELS = ["LR", "INV", "EAG", "DEL", "PL", "PA", "PL-P", "PA-P"]

C = sns.color_palette("muted")

TITLES = {
    LABELS[0]: "LR",
    LABELS[1]: "INV",
    LABELS[2]: "EAG",
    LABELS[3]: "DEL",
    LABELS[4]: "PL",
    LABELS[5]: "PA",
    LABELS[6]: "PL-P",
    LABELS[7]: "PA-P"
}

COLORS = {
    LABELS[0]: C[0],
    LABELS[1]: C[5],
    LABELS[2]: C[1],
    LABELS[3]: C[2],
    LABELS[4]: C[3],
    LABELS[5]: C[4],
    LABELS[6]: C[9],
    LABELS[7]: C[8],
}

LINESTYLES = {
    LABELS[0]: "solid",
    LABELS[1]: "dotted",
    LABELS[2]: "dashed",
    LABELS[3]: "solid",
    LABELS[4]: "dotted",
    LABELS[5]: "dashed",
    LABELS[6]: "solid",
    LABELS[7]: "dotted"
}

MARKERS = {
    LABELS[0]: "o",
    LABELS[1]: "P",
    LABELS[2]: "^",
    LABELS[3]: ">",
    LABELS[4]: "D",
    LABELS[5]: "X",
    LABELS[6]: "o",
    LABELS[7]: "^"
}

# Settings for all other figures
# FONT_SIZE = 20
# MARKER_SIZE=6.5
# YLABEL_PADDING=10
# SPACING = {
#     'hspace': .25,
#     'wspace': .25,
#     'top': 0.95,
#     'bottom': 0.12,
#     'left': 0.13,
#     'right': 0.915
# }
# LEGEND_POS = (0.975, 1.5)
# COLUMN_SPACING = 0.75
# MARKER_SPACING = 0.75
# HANDLE_LENGTH = 1.75

# Settings for Figure 12, 13, 14, 15
FONT_SIZE = 28
MARKER_SIZE=8.5
YLABEL_PADDING=2.5
SPACING = {
    'hspace': .5,
    'wspace': .35,
    'top': 0.9,
    'bottom': 0.14,
    'left': 0.175,
    'right': 0.915
}
LEGEND_POS = (0.975, 1.85)
COLUMN_SPACING = 0.25
MARKER_SPACING = 0.25
HANDLE_LENGTH = 1.1

FONT = {'family': 'Linux Libertine O', 'size': FONT_SIZE}
BOX = Bbox([[0, -0.2], [6, 5.2]])
HALF_BOX = Bbox([[0, -0.1], [6, 2.6]])
