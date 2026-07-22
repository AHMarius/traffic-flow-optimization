package tmg

import java.util.UUID

/**
 * @class ResultAggregator
 * @brief Merges partial results from micro/macro simulation runs into a single SimulationResult.
 */
class ResultAggregator {
    /**
     * @brief Merges data maps from microResults and macroResult into one, later results
     * overwriting earlier ones on key collision.
     * @return A SimulationResult whose success is false if any key held conflicting values
     * across results.
     */
    fun combine(
        taskId: UUID,
        microResults: List<PartialResult>,
        macroResult: PartialResult,
    ): SimulationResult {
        val allResults = microResults + macroResult
        val mergedData = mutableMapOf<String, Any>()
        var hasConflict = false

        for (result in allResults) {
            for ((key, value) in result.data) {
                val existing = mergedData[key]
                if (existing != null && existing != value) {
                    hasConflict = true
                }
                mergedData[key] = value
            }
        }

        return SimulationResult(
            taskId = taskId,
            data = mergedData,
            success = !hasConflict,
        )
    }
}
