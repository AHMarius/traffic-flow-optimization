# Command Reference: CLI/API Packet DSL

This document explains the command language used to talk to the simulation server over
`CliChannel` and `ApiChannel`. Every command is a single line (or line with wrapped
whitespace) of the form:

```
COMMAND: Field1: value1; Field2: value2; Field3: value3
```

---

## 1. The basics

- Fields are separated by `;`, and each field is `Key: Value`.
- Keys and command names are **not case-sensitive** (`run` and `RUN` are the same).
- A value can be:

| Value form | Meaning | Example |
|---|---|---|
| Bare word | a name | `London` |
| Quoted string | a name with spaces/special characters | `"Rush Hour Central"` |
| `#<number>` | a numeric id | `#4210` |
| `Random` | "pick one automatically" | `Scenario: Random` |
| `All` | "use everything" | `Region: All` |
| `RandomList(a, b, c)` | "pick randomly from this set" | `RandomList(London, Paris)` |
| `List(a, b, c)` | an explicit list | `List(0.4, 1.1, 3.0)` |
| `Region(a, b)` | a bounding box, upper-left and lower-right node | `Region(#4210, #4231)` |
| `true` / `false` | a boolean flag | `Monitoring: true` |

Anything that can be identified by **id or name** (nodes, scenarios, models, jobs) accepts
either form interchangeably. `Region: #4210` and `Region: "Tatarasi"` are both valid; use
whichever you have on hand.

Whitespace and line breaks around `:` and `;` are ignored, so a packet can be split across
multiple lines for readability:

```
RUN: City: London;
     Scenario: #17;
     Region: Region(#4210, #4231)
```

---

## 2. Commands

### RUN
Starts a simulation. `City`, `Scenario`, and `Region` are the minimum needed; everything else
is optional and defaults sensibly.

```
RUN: City: London; Scenario: #17; Region: Region(#4210, #4231)
```
Run a specific, known scenario over an exact bounding box.

```
RUN: City: London; Scenario: Random; Region: "Tatarasi"; Priority: high
```
Let the server pick a scenario for a named region, and flag the job as high priority.

```
RUN: City: RandomList(London, Birmingham, Manchester);
     Scenario: RandomList(#1, #2, "THIS");
     Region: Random;
     Repeat: 3;
     Algorithm: Random;
     FocusRegions: List(Region(#4210, #4231), "Tatarasi")
```
A randomized sweep: pick a city, a scenario, and a region at random, repeat the whole thing
3 times, let the server choose the optimization algorithm, and pay extra attention to two
specific focus areas while doing it.

```
RUN: City: London; Scenario: #17; Region: Region(#4210, #4231); Monitoring: true
```
Same run as the first example, but with per-step snapshots recorded: agent positions and
cohort states are captured at every simulation step and can be queried later by job id.

| Field | Required | Notes |
|---|---|---|
| `City` | yes | name, `RandomList(...)`, or `Random` |
| `Scenario` | yes | id, name, `Random`, or `RandomList(...)` |
| `Region` | no | `Region(a,b)`, a named/id region, `Random`, or `All` |
| `Priority` | no | `low` / `normal` / `high` |
| `Repeat` | no | integer ≥ 1, defaults to `1` |
| `Algorithm` | no | name, id, or `Random` |
| `FocusRegions` | no | `List(...)` of regions/names to prioritize |
| `Monitoring` | no | `true` / `false`, defaults to `false` (records a snapshot of agent positions and cohort states at every simulation step when enabled) |

---

### STORE_USER
Persists a user record.

```
STORE_USER: Username: "alex"; Role: operator; DisplayName: "Alex H."
```

| Field | Required | Notes |
|---|---|---|
| `Username` | yes | |
| `Role` | yes | `admin` / `operator` / `viewer` |
| `DisplayName` | no | |

---

### STORE_MODEL
Persists a calibrated model: the parameter set `P*` produced by `CALIBRATE`, along with its
measured error.

```
STORE_MODEL: City: London; Name: "London-v1"; Algorithm: genetic;
             Parameters: List(0.42, 1.15, 3.0); Error: 0.031
```

| Field | Required | Notes |
|---|---|---|
| `City` | yes | |
| `Name` | yes | |
| `Algorithm` | yes | which method produced the parameters |
| `Parameters` | yes | `List(...)` of numbers, the ordered vector `P*` |
| `Error` | yes | measured relative error `E(P*)` |

---

### STORE_SCENARIO
Persists a reusable scenario definition.

```
STORE_SCENARIO: Name: "Rush-Hour-Central"; City: London;
                 Region: Region(#4210, #4231);
                 FocusRegions: List("Tatarasi");
                 Source: #88
```

| Field | Required | Notes |
|---|---|---|
| `Name` | yes | |
| `City` | yes | |
| `Region` | yes | `Region(upperLeft, lowerRight)` |
| `FocusRegions` | no | `List(...)` |
| `Source` | no | id/name of the `RUN` job that generated this scenario, if any |

---

### LOAD_AND_RUN
Loads a previously stored scenario or model and runs it immediately.

```
LOAD_AND_RUN: File: "london_rush_hour.scn"; Repeat: 4; Algorithm: aco
```

| Field | Required | Notes |
|---|---|---|
| `File` | yes | id or name of the stored scenario/model |
| `Repeat` | no | integer ≥ 1, defaults to `1` |
| `Algorithm` | no | overrides the stored default, if any |

---

### PRELOAD
Warms the network/dataset cache ahead of time, so a following `RUN` starts faster.

```
PRELOAD: City: London; Region: Region(#4210, #4231)
```

| Field | Required | Notes |
|---|---|---|
| `City` | yes | |
| `Region` | no | limits preloading to a bounding box; omit to preload the whole city |

---

### RESET
Clears engine state.

```
RESET: Scope: all
```

| Field | Required | Notes |
|---|---|---|
| `Scope` | yes | `network` / `model` / `all` |

---

### CALIBRATE
Runs the calibration search for the optimal parameter set `P*`, minimizing the relative error
`E(P)` against real data.

```
CALIBRATE: City: London; Scenario: #17; Algorithm: genetic;
           MaxError: 0.05; Repeat: 10
```

| Field | Required | Notes |
|---|---|---|
| `City` | yes | |
| `Scenario` | yes | |
| `Algorithm` | yes | e.g. `genetic` |
| `MaxError` | yes | target ceiling for `E(P*)` |
| `Repeat` | no | number of calibration passes, defaults to `1` |

---

### VALIDATE
Checks whether a stored model is `k`-credible against a reference dataset, i.e. whether its
average relative error stays under `k%`.

```
VALIDATE: Model: #204; ReferenceData: "iasi_2026_q2"; Threshold: 5
```

| Field | Required | Notes |
|---|---|---|
| `Model` | yes | id or name of a stored model |
| `ReferenceData` | yes | name of the reference dataset |
| `Threshold` | yes | `k`, as a percentage |

---

### COMPARE
Compares two stored models and reports which one performs better (lower average waiting
time, using stop count as a tiebreaker).

```
COMPARE: Baseline: #200; Candidate: #204; Delta: 0.5
```

| Field | Required | Notes |
|---|---|---|
| `Baseline` | yes | model to compare against |
| `Candidate` | yes | model being evaluated |
| `Delta` | yes | minimum improvement to count as significant |

---

### STATUS
Checks the status of a running or queued job.

```
STATUS: Job: #55
```

| Field | Required | Notes |
|---|---|---|
| `Job` | yes | id or name of the job |

---

### CANCEL
Stops a running job.

```
CANCEL: Job: #55
```

| Field | Required | Notes |
|---|---|---|
| `Job` | yes | id or name of the job |

---

## 3. Errors

Malformed packets are rejected before they reach the simulation engine. A missing required
field, an unknown command, an unterminated string, or a `Region(...)` with the wrong number
of arguments all produce a clear error message describing exactly what was wrong and where,
instead of a partial or silently-wrong command.