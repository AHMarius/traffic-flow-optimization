package commonutils

sealed interface Strategy

data class Increase(
    val target: Target,
    val mode: Mode,
    val value: Value,
) : Strategy

data class Decrease(
    val target: Target,
    val mode: Mode,
    val value: Value,
) : Strategy

data class SetValue(
    val target: Target,
    val value: Value,
) : Strategy

data class Randomize(
    val target: Target,
    val source: RandomSource,
) : Strategy

sealed interface Target

object All : Target

data class Parameters(
    val names: List<String>,
) : Target

object RandomParameter : Target

enum class Mode { BY, FACTOR }

sealed interface Value

data class Constant(
    val value: Double,
) : Value

data class RandomValue(
    val min: Double,
    val max: Double,
) : Value

sealed interface RandomSource

data class Interval(
    val min: Double,
    val max: Double,
    val includeMin: Boolean,
    val includeMax: Boolean,
) : RandomSource

data class Choices(
    val values: List<Double>,
) : RandomSource

object ExistingRandomValue : RandomSource
