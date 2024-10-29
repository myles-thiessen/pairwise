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


def parse_ycsb_throughput(file: str) -> float:
    with open(file) as f:
        for line in f:
            if line.startswith("[OVERALL], Throughput(ops/sec),"):
                return float(line.removeprefix("[OVERALL], Throughput(ops/sec),"))


def parse_ycsb_time(file: str) -> float:
    with open(file) as f:
        for line in f:
            if line.startswith("[OVERALL], RunTime(ms),"):
                return float(line.removeprefix("[OVERALL], RunTime(ms),"))


def parse_ycsb_lat_read_avg(file: str) -> float:
    with open(file) as f:
        for line in f:
            if line.startswith("[READ], AverageLatency(us),"):
                return float(line.removeprefix("[READ], AverageLatency(us),"))


def parse_ycsb_read_ops(file: str) -> float:
    with open(file) as f:
        for line in f:
            if line.startswith("[READ], Operations,"):
                return float(line.removeprefix("[READ], Operations,"))


def parse_ycsb_lat_update_avg(file: str) -> float:
    with open(file) as f:
        for line in f:
            if line.startswith("[UPDATE], AverageLatency(us),"):
                return float(line.removeprefix("[UPDATE], AverageLatency(us),"))


def parse_ycsb_update_ops(file: str) -> float:
    with open(file) as f:
        for line in f:
            if line.startswith("[UPDATE], Operations,"):
                return float(line.removeprefix("[UPDATE], Operations,"))
