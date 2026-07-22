package com

/**
 * @class Ref
 * @brief Reference to an entity, either by id or by name.
 */
sealed class Ref

data class ById(
    val id: Long,
) : Ref()

data class ByName(
    val name: String,
) : Ref()

/**
 * @class FieldValue
 * @brief Polymorphic value a command field can hold (a direct reference, literal, list, region, or selector).
 */
sealed class FieldValue

data class RefValue(
    val ref: Ref,
) : FieldValue()

data class Literal(
    val value: Double,
) : FieldValue()

data class ListValue(
    val items: List<FieldValue>,
) : FieldValue()

data class RegionValue(
    val upperLeft: Ref,
    val lowerRight: Ref,
) : FieldValue()

/** @brief Selects a random option among available candidates. */
object RandomSelector : FieldValue()

/** @brief Selects all available candidates. */
object AllSelector : FieldValue()

data class RandomListSelector(
    val options: List<Ref>,
) : FieldValue()

enum class Priority { LOW, NORMAL, HIGH }

/**
 * @class Command
 * @brief Base type for all commands accepted by the dispatcher.
 */
sealed class Command

/**
 * @brief Runs a simulation.
 * @param cities RefValue or RandomListSelector.
 * @param scenario RefValue, RandomSelector, or RandomListSelector.
 * @param region RegionValue, RefValue, RandomSelector, or AllSelector.
 * @param algorithm RefValue or RandomSelector.
 */
data class RunCommand(
    val cities: FieldValue,
    val scenario: FieldValue,
    val region: FieldValue?,
    val priority: Priority? = null,
    val repeat: Int = 1,
    val algorithm: FieldValue? = null,
    val focusRegions: ListValue? = null,
    val monitoring: Boolean = false,
) : Command()

sealed class StoreCommand : Command()

data class StoreUserCommand(
    val username: String,
    val role: String,
    val displayName: String?,
) : StoreCommand()

data class StoreModelCommand(
    val city: String,
    val name: String,
    val parameters: List<Double>,
    val error: Double,
    val algorithm: String,
) : StoreCommand()

data class StoreScenarioCommand(
    val name: String,
    val city: String,
    val region: RegionValue,
    val focusRegions: ListValue?,
    val source: Ref?,
) : StoreCommand()

data class LoadAndRunCommand(
    val file: Ref,
    val repeat: Int = 1,
    val algorithm: FieldValue? = null,
) : Command()

data class PreloadCommand(
    val city: String,
    val region: RegionValue?,
) : Command()

/** @param scope One of "network", "model", or "all". */
data class ResetCommand(
    val scope: String,
) : Command()

data class CalibrateCommand(
    val city: String,
    val scenario: Ref,
    val algorithm: String,
    val maxError: Double,
    val repeat: Int = 1,
) : Command()

data class ValidateCommand(
    val model: Ref,
    val referenceData: String,
    val threshold: Double,
) : Command()

data class CompareCommand(
    val baseline: Ref,
    val candidate: Ref,
    val delta: Double,
) : Command()

data class StatusCommand(
    val job: Ref,
) : Command()

data class CancelCommand(
    val job: Ref,
) : Command()
