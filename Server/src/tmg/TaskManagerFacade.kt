package tmg

/**
 * @class TaskManagerFacade
 * @brief Coordinates task splitting, micro/macro simulation, result aggregation, and calibration.
 */
class TaskManagerFacade(
    val splitter: TaskSpliter,
    val microSimulator: IMicroSimulator,
    val macroSimulator: IMacroSimulator,
    val aggregator: ResultAggregator,
    val calibrationParameters: ModelParameters,
) {
    /**
     * @brief Splits task into micro/macro parts, simulates each, and combines the results.
     */
    fun submit(
        task: Task,
        observer: ISimulationObserver,
    ): SimulationResult {
        val splitTask = splitter.split(task)

        val microResults = splitTask.microParts.map { microSimulator.simulate(it) }
        val macroResult = macroSimulator.simulate(splitTask.macroPart)

        return aggregator.combine(task.id, microResults, macroResult)
    }

    /**
     * @brief Runs calibrateOnce passes times and keeps the parameters with the lowest
     * relativeError.
     * @return The best ModelParameters found, or calibrationParameters if no pass improved on it.
     */
    fun calibrate(
        task: Task,
        threshold: Double,
        passes: Int = 1,
    ): ModelParameters {
        var best: ModelParameters? = null

        for (i in 0 until passes) {
            val candidate = calibrateOnce(task, threshold)
            val currentBest = best

            if (currentBest == null || candidate.relativeError < currentBest.relativeError) {
                best = candidate
            }
        }

        return best ?: calibrationParameters
    }

    /**
     * @brief Iteratively submits task under progressively randomized parameters until the
     * resulting relative error is within threshold or MAX_CALIBRATION_ITERATIONS is reached.
     * @return The parameters at the point of success (or exhaustion of iterations).
     */
    private fun calibrateOnce(
        task: Task,
        threshold: Double,
    ): ModelParameters {
        var parameters = calibrationParameters
        var iterations = 0

        while (iterations < MAX_CALIBRATION_ITERATIONS) {
            val candidateTask = task.copy(parameters = parameters)
            val result = submit(candidateTask, NoOpSimulationObserver)

            parameters =
                ModelParameters(
                    values = parameters.values,
                    relativeError = extractRelativeError(result),
                )

            if (parameters.isValid(threshold)) {
                return parameters
            }

            parameters.adjust("random all [-0.1,0.1]")
            iterations++
        }

        return parameters
    }

    private fun extractRelativeError(result: SimulationResult): Double =
        (result.data["relativeError"] as? Number)?.toDouble() ?: Double.POSITIVE_INFINITY

    companion object {
        private const val MAX_CALIBRATION_ITERATIONS = 100
    }
}
