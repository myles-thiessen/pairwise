import collections
from dataclasses import dataclass
from typing import List, Dict, Tuple


@dataclass
class Trace:
    opType: str
    time: int


def parse_trace(trace_dir: str, files: List[str]) -> List[Dict[Tuple[int, int], Dict[str, Trace]]]:
    traces = []
    for file in files:
        d = dict()
        with open(trace_dir + "/" + file) as f:
            for line in f:
                measurement = line.strip().split(" ")
                op_id = (int(measurement[1]), int(measurement[2]))
                t = Trace(measurement[0], int(measurement[4]))
                if op_id not in d:
                    d[op_id] = dict()
                d[op_id][str(measurement[3])] = t
        traces.append(collections.OrderedDict(sorted(d.items())))
    return traces


@dataclass
class Result:
    opType: str
    time: float


def parse_results(dirs: List[str], file: str) -> List[List[Result]]:
    results = []
    for d in dirs:
        result = []
        with open(d + "/" + file) as f:
            for line in f:
                measurement = line.strip().split(" ")
                if len(measurement) < 2:
                    continue
                r = Result(str(measurement[0]), float(measurement[1]))
                result.append(r)
        results.append(result)
    return results
