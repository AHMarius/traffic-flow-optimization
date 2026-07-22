package commonutils

/**
 * @class StrategyParser
 * @brief Parses a strategy command string (e.g. "increase <param> by 5", "random all") into a Strategy.
 */
object StrategyParser {
    /**
     * @brief Matches: <...> parameter lists, [...]/(...) bracketed ranges or value lists
     * (allowing mixed closing bracket/paren), or any other whitespace-delimited token.
     */
    private val tokenRegex = Regex("""<[^>]*>|\[[^\]]*[)\]]|\([^)]*[)\]]|\S+""")

    /**
     * @brief Tokenizes command and builds the matching Strategy for its leading action
     * (increase/decrease/set/random).
     * @throws IllegalArgumentException/IllegalStateException on empty input, unknown action, or malformed arguments.
     */
    fun parse(command: String): Strategy {
        val tokens = tokenRegex.findAll(command.trim()).map { it.value }.toList()
        require(tokens.isNotEmpty()) { "Empty command" }

        val action = tokens[0].lowercase()
        var idx = 1

        return when (action) {
            "increase", "decrease" -> {
                val target = parseTarget(tokens[idx])
                idx++
                val mode =
                    when (tokens[idx].lowercase()) {
                        "factor" -> Mode.FACTOR
                        "by" -> Mode.BY
                        else -> error("Unknown mode '${tokens[idx]}'")
                    }
                idx++
                val value = parseValue(tokens.drop(idx))
                if (action == "increase") {
                    Increase(target, mode, value)
                } else {
                    Decrease(target, mode, value)
                }
            }

            "set" -> {
                val target = parseTarget(tokens[idx])
                idx++
                SetValue(target, parseValue(tokens.drop(idx)))
            }

            "random" -> {
                val target = parseTarget(tokens[idx])
                idx++
                val remaining = tokens.drop(idx)
                val source = if (remaining.isEmpty()) ExistingRandomValue else parseSource(remaining)
                Randomize(target, source)
            }

            else -> {
                error("Unknown action '$action'")
            }
        }
    }

    /**
     * @brief Parses a target token: <param1,param2> as Parameters, or the literals "all"/"random".
     * @throws IllegalStateException if token matches none of the recognized forms.
     */
    private fun parseTarget(token: String): Target =
        when {
            token.startsWith("<") -> {
                Parameters(
                    token
                        .removePrefix("<")
                        .removeSuffix(">")
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() },
                )
            }

            token.equals("all", ignoreCase = true) -> {
                All
            }

            token.equals("random", ignoreCase = true) -> {
                RandomParameter
            }

            else -> {
                error("Unknown target '$token'")
            }
        }

    /**
     * @brief Parses either "random min,max" as a RandomValue or a single number as a Constant.
     * @throws IllegalArgumentException/IllegalStateException if tokens is empty or malformed.
     */
    private fun parseValue(tokens: List<String>): Value {
        require(tokens.isNotEmpty()) { "Missing value" }
        return if (tokens[0].equals("random", ignoreCase = true)) {
            val (min, max) = parseNumberPair(tokens.getOrNull(1) ?: error("Expected range after 'random'"))
            RandomValue(min, max)
        } else {
            Constant(tokens[0].toDouble())
        }
    }

    /**
     * @brief Parses a random source: "random" (existing value), "values [...]" (discrete choices),
     * or a bracketed/parenthesized interval.
     * @throws IllegalStateException on an unrecognized or malformed source.
     */
    private fun parseSource(tokens: List<String>): RandomSource {
        val first = tokens[0]
        return when {
            first.equals("random", ignoreCase = true) -> {
                ExistingRandomValue
            }

            first.equals("values", ignoreCase = true) -> {
                val listToken = tokens.getOrNull(1) ?: error("Expected list after 'values'")
                Choices(
                    listToken
                        .trim('[', ']', '(', ')')
                        .split(",")
                        .map { it.trim().toDouble() },
                )
            }

            first.startsWith("[") || first.startsWith("(") -> {
                parseInterval(first)
            }

            else -> {
                error("Unknown source '$first'")
            }
        }
    }

    /**
     * @brief Parses an interval token like "[1,5)", deriving inclusivity from the bracket style.
     */
    private fun parseInterval(token: String): Interval {
        val includeMin = token.startsWith("[")
        val includeMax = token.endsWith("]")
        val (min, max) = parseNumberPair(token)
        return Interval(min, max, includeMin, includeMax)
    }

    /**
     * @throws IllegalArgumentException if token doesn't contain exactly two comma-separated numbers.
     */
    private fun parseNumberPair(token: String): Pair<Double, Double> {
        val parts = token.trim('[', ']', '(', ')').split(",").map { it.trim().toDouble() }
        require(parts.size == 2) { "Expected two numbers in '$token'" }
        return parts[0] to parts[1]
    }
}
