package com

import commonutils.Graph
import commonutils.Scenario
import dal.DatabaseFacade
import tmg.ISimulationObserver
import tmg.ModelParameters
import tmg.NoOpSimulationObserver
import tmg.SimulationResult
import tmg.SnapshotRecorder
import tmg.Task
import tmg.TaskManagerFacade

/**
 * @class CommandDispatcher
 * @brief Parses and routes incoming commands to their corresponding handlers.
 */
class CommandDispatcher(
    private val taskManagerFacade: TaskManagerFacade,
    private val databaseFacade: DatabaseFacade,
    // private val optimisationAlgorithm: OptimiastionAlgorithm
) {
    companion object {
        private const val POLL_INTERVAL_MS = 1000L
        private const val MAX_POLL_ATTEMPTS = 30
    }

    fun dispatch(rawPacket: String) = dispatch(PacketParser.parse(rawPacket))

    /**
     * @brief Routes a parsed command to its handler based on runtime type.
     * @throws IllegalArgumentException if the command type has no registered handler.
     */
    fun dispatch(command: Command) {
        when (command) {
            is ResetCommand -> handleReset(command)

            is StoreCommand -> handleStore(command)

            is RunCommand -> handleRun(command)

            is LoadAndRunCommand -> handleLoadAndRun(command)

            is PreloadCommand -> handlePreload(command)

            is CalibrateCommand -> handleCalibrate(command)

            is ValidateCommand -> handleValidate(command)

            is CompareCommand -> handleCompare(command)

            is StatusCommand -> handleStatus(command)

            is CancelCommand -> handleCancel(command)

            else -> throw IllegalArgumentException(
                "Unsupported command type: ${command::class.simpleName}",
            )
        }
    }

    /**
     * @brief Polls statusProvider at fixed intervals while the submodule is waiting for resources,
     * until it reaches a terminal state.
     * @param status Initial status to evaluate.
     * @param statusProvider Callback polled for the submodule's current status.
     * @throws IllegalStateException if the poll limit is reached, or if the submodule fails or is interrupted.
     */
    fun awaitSubmoduleStatus(
        status: SubmoduleStatus,
        statusProvider: () -> SubmoduleStatus,
    ) {
        var currentStatus = status
        var attempts = 0

        while (currentStatus == SubmoduleStatus.SUBMODULE_WAITING_FOR_RESOURCES) {
            if (attempts >= MAX_POLL_ATTEMPTS) {
                throw IllegalStateException(
                    "Submodule still waiting for resources after $MAX_POLL_ATTEMPTS attempts",
                )
            }

            Thread.sleep(POLL_INTERVAL_MS)
            attempts++
            currentStatus = statusProvider()
        }

        when (currentStatus) {
            SubmoduleStatus.SUBMODULE_FINISHED -> {}

            SubmoduleStatus.SUBMODULE_FAILED -> {
                throw IllegalStateException("Submodule reported a failure")
            }

            SubmoduleStatus.SUBMODULE_INTERRUPTED -> {
                throw IllegalStateException("Submodule was interrupted")
            }

            SubmoduleStatus.SUBMODULE_WAITING_FOR_RESOURCES -> {
                error("loop is misbehaving")
            }
        }
    }

    private fun handleReset(command: ResetCommand) {
        println("handleReset: $command")
    }

    private fun handleStore(command: StoreCommand) {
        when (command) {
            is StoreUserCommand -> println("handleStore: $command")
            is StoreModelCommand -> println("handleStore: $command")
            is StoreScenarioCommand -> println("handleStore: $command")
        }
    }

    private fun handleLoadAndRun(command: LoadAndRunCommand) {
        println("handleLoadAndRun: $command")
    }

    private fun handlePreload(command: PreloadCommand) {
        println("handlePreload: $command")
    }

    /**
     * @brief Resolves the target city/scenario, loads the graph and scenario, and submits
     * command.repeat simulation task(s) via taskManagerFacade.
     *
     * @note command.region/focusRegions/priority/algorithm are not yet consumed — subdomain
     * mirrors domain until region-based restriction is implemented.
     */
    private fun handleRun(command: RunCommand) {
        val observer: ISimulationObserver =
            if (command.monitoring) SnapshotRecorder(databaseFacade) else NoOpSimulationObserver

        val city = refToCityName(resolveCityRef(command.cities))
        val scenarioIdentifier = refToScenarioIdentifier(resolveScenarioRef(command.scenario))

        val domain: Graph = databaseFacade.loadGraph(city)
        val scenario: Scenario = databaseFacade.loadScenario(city, scenarioIdentifier)

        // NOTE: `command.region` / `command.focusRegions` aren't applied yet
        val results = mutableListOf<SimulationResult>()

        repeat(command.repeat) {
            val task =
                Task(
                    domain = domain,
                    subdomain = domain,
                    scenarioCommand = scenario,
                    parameters = taskManagerFacade.calibrationParameters,
                )

            results += taskManagerFacade.submit(task, observer)

            if (command.monitoring) {
                println("handleRun: recorded snapshots for task=${task.id} (see databaseFacade.findByTaskId)")
            }
        }

        println(
            "handleRun: $command -> completed ${results.size} run(s) for city=$city scenario=$scenarioIdentifier",
        )
    }

    /**
     * @return The city Ref, resolved from either a direct RefValue or a random pick from RandomListSelector.
     * @throws IllegalArgumentException if field is any other FieldValue subtype.
     */
    private fun resolveCityRef(field: FieldValue): Ref =
        when (field) {
            is RefValue -> {
                field.ref
            }

            is RandomListSelector -> {
                field.options.random()
            }

            else -> {
                throw IllegalArgumentException(
                    "Unsupported city selector: ${field::class.simpleName} (expected RefValue or RandomListSelector)",
                )
            }
        }

    /**
     * @return The scenario Ref, resolved from either a direct RefValue or a random pick from RandomListSelector.
     * @throws NotImplementedError if field is RandomSelector (no lookup of all scenarios for a city exists yet).
     * @throws IllegalArgumentException if field is any other FieldValue subtype.
     */
    private fun resolveScenarioRef(field: FieldValue): Ref =
        when (field) {
            is RefValue -> {
                field.ref
            }

            is RandomListSelector -> {
                field.options.random()
            }

            RandomSelector -> {
                throw NotImplementedError(
                    "RandomSelector needs a source of all available scenarios for the city to pick " +
                        "from — that lookup doesn't exist yet.",
                )
            }

            else -> {
                throw IllegalArgumentException(
                    "Unsupported scenario selector: ${field::class.simpleName}",
                )
            }
        }

    /**
     * @return The city name for a ByName ref.
     * @throws UnsupportedOperationException if ref is ById (loadGraph only accepts a name, no id lookup exists yet).
     */
    private fun refToCityName(ref: Ref): String =
        when (ref) {
            is ByName -> {
                ref.name
            }

            is ById -> {
                throw UnsupportedOperationException(
                    "Resolving a city by numeric id isn't supported yet — DatabaseFacade.loadGraph only " +
                        "takes a city name. Use ByName for cities for now, or add an id -> name lookup.",
                )
            }
        }

    private fun refToScenarioIdentifier(ref: Ref): String =
        when (ref) {
            is ByName -> ref.name
            is ById -> ref.id.toString()
        }

    /**
     * @brief Loads the graph and scenario for the command's city, then calibrates model parameters
     * against them.
     *
     * @note subdomain mirrors domain, same caveat as handleRun.
     */
    private fun handleCalibrate(command: CalibrateCommand) {
        val domain: Graph = databaseFacade.loadGraph(command.city)
        val scenario: Scenario = databaseFacade.loadScenario(command.city, refToScenarioIdentifier(command.scenario))

        val task =
            Task(
                domain = domain,
                subdomain = domain,
                scenarioCommand = scenario,
                parameters = taskManagerFacade.calibrationParameters,
            )

        val calibratedParameters: ModelParameters =
            taskManagerFacade.calibrate(
                task = task,
                threshold = command.maxError,
                passes = command.repeat,
            )

        println("handleCalibrate: $command -> algorithm=${command.algorithm} result=$calibratedParameters")
    }

    private fun handleValidate(command: ValidateCommand) {
        println("handleValidate: $command")
    }

    private fun handleCompare(command: CompareCommand) {
        println("handleCompare: $command")
    }

    private fun handleStatus(command: StatusCommand) {
        println("handleStatus: $command")

        // TODO: once a job's Ref (ById/ByName) can be resolved to the UUID taskId
    }

    private fun handleCancel(command: CancelCommand) {
        println("handleCancel: $command")
    }
}
