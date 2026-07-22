package tmg

import commonutils.*
import kotlin.random.Random
import commonutils.Target as CUTarget

/**
 * @class ModelParameters
 * @brief Named model parameter values with an associated relative error, adjustable via Strategy
 * commands (increase/decrease/set/randomize).
 */
class ModelParameters(
    var values: Map<String, Double>,
    var relativeError: Double,
) {
    fun adjust(command: String) = adjust(StrategyParser.parse(command))

    /**
     * @brief Applies a parsed Strategy to values, mutating targeted entries in place based on
     * its action (increase/decrease/set/randomize).
     */
    fun adjust(strategy: Strategy) {
        when (strategy) {
            is Increase -> {
                applyIncrease(resolveTargets(strategy.target), strategy.mode, resolveValue(strategy.value))
            }

            is Decrease -> {
                applyDecrease(resolveTargets(strategy.target), strategy.mode, resolveValue(strategy.value))
            }

            is SetValue -> {
                val amount = resolveValue(strategy.value)
                val targets = resolveTargets(strategy.target)
                values = values.toMutableMap().apply { targets.forEach { this[it] = amount } }
            }

            is Randomize -> {
                val targets = resolveTargets(strategy.target)
                values =
                    values.toMutableMap().apply {
                        targets.forEach { key -> this[key] = resolveRandomSource(strategy.source) }
                    }
            }
        }
    }

    fun isValid(threshold: Double): Boolean =
        relativeError.isFinite() &&
            kotlin.math.abs(relativeError) <= threshold &&
            values.values.all { it.isFinite() }

    private fun applyIncrease(
        targets: List<String>,
        mode: Mode,
        amount: Double,
    ) {
        values =
            values.toMutableMap().apply {
                targets.forEach { key ->
                    val current = this[key] ?: return@forEach
                    this[key] =
                        when (mode) {
                            Mode.BY -> current + amount
                            Mode.FACTOR -> current * amount
                        }
                }
            }
    }

    /**
     * @throws IllegalArgumentException if mode is FACTOR and amount is 0.
     */
    private fun applyDecrease(
        targets: List<String>,
        mode: Mode,
        amount: Double,
    ) {
        values =
            values.toMutableMap().apply {
                targets.forEach { key ->
                    val current = this[key] ?: return@forEach
                    this[key] =
                        when (mode) {
                            Mode.BY -> {
                                current - amount
                            }

                            Mode.FACTOR -> {
                                require(amount != 0.0) { "Cannot decrease by factor 0 (division by zero)" }
                                current / amount
                            }
                        }
                }
            }
    }

    /**
     * @return The parameter name(s) target refers to: all keys, a validated explicit list,
     * or one random key.
     * @throws IllegalArgumentException if target names any parameter not present in values.
     */
    private fun resolveTargets(target: CUTarget): List<String> =
        when (target) {
            is All -> {
                values.keys.toList()
            }

            is Parameters -> {
                val missing = target.names.filterNot { values.containsKey(it) }
                require(missing.isEmpty()) { "Unknown parameter(s): $missing" }
                target.names
            }

            is RandomParameter -> {
                listOf(values.keys.random())
            }
        }

    private fun resolveValue(value: Value): Double =
        when (value) {
            is Constant -> value.value
            is RandomValue -> Random.nextDouble(value.min, value.max)
        }

    private fun resolveRandomSource(source: RandomSource): Double =
        when (source) {
            is Interval -> randomInInterval(source)
            is Choices -> source.values.random()
            ExistingRandomValue -> values.values.random()
        }

    /**
     * @throws IllegalArgumentException if interval.min > interval.max.
     */
    private fun randomInInterval(interval: Interval): Double {
        require(interval.min <= interval.max) { "Invalid interval: $interval" }
        return Random.nextDouble(interval.min, interval.max)
    }
}
