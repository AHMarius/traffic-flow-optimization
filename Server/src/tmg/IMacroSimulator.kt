package tmg

interface IMacroSimulator {
    fun simulate(task: Task): PartialResult
}
