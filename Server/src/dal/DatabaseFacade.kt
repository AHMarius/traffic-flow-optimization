package dal

import commonutils.Edge
import commonutils.Graph
import commonutils.Node
import commonutils.Scenario
import tmg.MapSnapshotRow
import java.time.LocalDateTime
import java.util.UUID

/**
 * @class DatabaseFacade
 * @brief Loads graphs/scenarios and persists/retrieves snapshots via a query executor.
 */
class DatabaseFacade(
    private val queryExecutor: IQueryExecutor,
) {
    fun submit(request: QueryRequest): QueryResult = queryExecutor.execute(request.sql, request.params)

    /**
     * @brief Loads all nodes and edges for city and assembles them into a Graph.
     * Edges referencing a missing source or target node are skipped.
     */
    fun loadGraph(city: String): Graph {
        val nodeRows =
            query(
                "SELECT id, flags, longitude, latitude FROM nodes WHERE city = ?",
                listOf(city),
            )

        val edgeRows =
            query(
                "SELECT source_id, target_id, distance, flags, density, average_speed, congestion_factor, weight " +
                    "FROM edges WHERE city = ?",
                listOf(city),
            )

        val graph = Graph()
        val nodesById = mutableMapOf<Int, Node>()

        nodeRows.forEach { row ->
            val node =
                Node(
                    id = (row["id"] as Number).toInt(),
                    flags = parseFlags(row["flags"]),
                    longitude = (row["longitude"] as Number).toDouble(),
                    latitude = (row["latitude"] as Number).toDouble(),
                )
            nodesById[node.id] = node
            graph.addNode(node)
        }

        edgeRows.forEach { row ->
            val source = nodesById[(row["source_id"] as Number).toInt()] ?: return@forEach
            val target = nodesById[(row["target_id"] as Number).toInt()] ?: return@forEach

            graph.addEdge(
                Edge(
                    sourceNode = source,
                    targetNode = target,
                    distance = (row["distance"] as Number).toDouble(),
                    flags = row["flags"] as? Boolean ?: false,
                    density = (row["density"] as Number).toDouble(),
                    averageSpeed = (row["average_speed"] as Number).toDouble(),
                    congestionFactor = (row["congestion_factor"] as Number).toDouble(),
                    weight = (row["weight"] as Number).toDouble(),
                ),
            )
        }

        return graph
    }

    /**
     * @brief Loads the scenario matching city and scenario (by id or name), reloading the
     * city's graph for it.
     * @throws NoSuchElementException if no matching scenario row is found.
     */
    fun loadScenario(
        city: String,
        scenario: String,
    ): Scenario {
        val row =
            query(
                "SELECT id, name, time_interval, environment_conditions, creation_date " +
                    "FROM scenarios WHERE city = ? AND (id = ? OR name = ?) LIMIT 1",
                listOf(city, scenario, scenario),
            ).firstOrNull() ?: throw NoSuchElementException("No scenario found for city=$city scenario=$scenario")

        return Scenario(
            id = UUID.fromString(row["id"].toString()),
            name = row["name"] as String,
            graph = loadGraph(city),
            timeInterval = row["time_interval"] as String,
            environmentConditions = parseEnvironmentConditions(row["environment_conditions"]),
            creationDate = row["creation_date"] as? LocalDateTime ?: LocalDateTime.now(),
        )
    }

    /**
     * @throws IllegalStateException if the query fails.
     */
    private fun query(
        sql: String,
        params: List<Any>,
    ): List<Map<String, Any>> {
        val result = queryExecutor.execute(sql, params)
        check(result.success) { "Query failed: ${result.errorMessage}" }
        return result.rows
    }

    /**
     * @brief Parses a flags column value (comma-separated string or collection) into a set of strings.
     */
    private fun parseFlags(value: Any?): MutableSet<String> =
        when (value) {
            is String -> {
                value
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toMutableSet()
            }

            is Collection<*> -> {
                value.filterIsInstance<String>().toMutableSet()
            }

            else -> {
                mutableSetOf()
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun parseEnvironmentConditions(value: Any?): MutableMap<String, Any> =
        when (value) {
            is Map<*, *> -> value.entries.associate { (k, v) -> k.toString() to (v as Any) }.toMutableMap()
            else -> mutableMapOf()
        }

    // Snapshot Repository

    fun findByTaskId(taskId: UUID): List<MapSnapshotRow> {
        val rows =
            query(
                "SELECT task_id, step_index, timestamp, agent_positions, cohort_states " +
                    "FROM snapshots WHERE task_id = ? ORDER BY step_index",
                listOf(taskId.toString()),
            )
        return rows.map(::toSnapshotRow)
    }

    fun findByTaskIdAndStep(
        taskId: UUID,
        stepIndex: Int,
    ): List<MapSnapshotRow> {
        val rows =
            query(
                "SELECT task_id, step_index, timestamp, agent_positions, cohort_states " +
                    "FROM snapshots WHERE task_id = ? AND step_index = ?",
                listOf(taskId.toString(), stepIndex),
            )
        return rows.map(::toSnapshotRow)
    }

    private fun toSnapshotRow(row: Map<String, Any>): MapSnapshotRow =
        MapSnapshotRow(
            taskId = UUID.fromString(row["task_id"].toString()),
            stepIndex = (row["step_index"] as Number).toInt(),
            timestamp = (row["timestamp"] as Number).toDouble(),
            agentPositionsJson = row["agent_positions"] as String,
            cohortStatesJson = row["cohort_states"] as String,
        )
}
