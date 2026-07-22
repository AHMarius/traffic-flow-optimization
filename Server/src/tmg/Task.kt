package tmg

import commonutils.Graph
import commonutils.Scenario
import java.util.UUID

/**
 * @class Task
 * @brief A simulation task: the domain/subdomain graphs, scenario, and model parameters to run.
 */
class Task(
    val id: UUID = UUID.randomUUID(),
    val domain: Graph,
    val subdomain: Graph,
    val scenarioCommand: Scenario,
    val parameters: ModelParameters,
) {
    /**
     * @brief Returns a copy of this task with parameters replaced, keeping the same id.
     */
    fun copy(parameters: ModelParameters): Task =
        Task(
            id = id,
            domain = domain,
            subdomain = subdomain,
            scenarioCommand = scenarioCommand,
            parameters = parameters,
        )
}
