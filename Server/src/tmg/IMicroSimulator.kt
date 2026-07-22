package tmg

interface IMicroSimulator {
    fun simulate(task: Task): PartialResult
}
