package com

import java.util.Locale.getDefault

/**
 * @class PacketParser
 * @brief Parses a raw packet string into a Command, via lexing, grammar parsing, and
 * conversion of the resulting field map into the appropriate Command subtype.
 */
object PacketParser {
    fun parse(packet: String): Command {
        val tokens = Lexer(packet).tokenize()
        val raw = GrammarParser(tokens, packet).parsePacket()
        return build(raw)
    }

    private enum class TokType { STRING, WORD, COLON, SEMI, COMMA, LPAREN, RPAREN, HASH, EOF }

    private data class Token(
        val type: TokType,
        val text: String,
        val pos: Int,
    )

    private val WORD_STOP = charArrayOf(':', ';', ',', '(', ')', '#', '"')

    /**
     * @class Lexer
     * @brief Splits a packet string into a flat token stream (words, strings, punctuation).
     */
    private class Lexer(
        private val input: String,
    ) {
        private var pos = 0

        fun tokenize(): List<Token> {
            val tokens = mutableListOf<Token>()
            while (true) {
                skipWhitespace()
                if (pos >= input.length) {
                    tokens.add(Token(TokType.EOF, "", pos))
                    break
                }
                val start = pos
                when (input[pos]) {
                    ':' -> {
                        pos++
                        tokens.add(Token(TokType.COLON, ":", start))
                    }

                    ';' -> {
                        pos++
                        tokens.add(Token(TokType.SEMI, ";", start))
                    }

                    ',' -> {
                        pos++
                        tokens.add(Token(TokType.COMMA, ",", start))
                    }

                    '(' -> {
                        pos++
                        tokens.add(Token(TokType.LPAREN, "(", start))
                    }

                    ')' -> {
                        pos++
                        tokens.add(Token(TokType.RPAREN, ")", start))
                    }

                    '#' -> {
                        pos++
                        tokens.add(Token(TokType.HASH, "#", start))
                    }

                    '"' -> {
                        tokens.add(readString(start))
                    }

                    else -> {
                        tokens.add(readWord(start))
                    }
                }
            }
            return tokens
        }

        private fun skipWhitespace() {
            while (pos < input.length && input[pos].isWhitespace()) pos++
        }

        /**
         * @brief Reads a quoted string starting at the opening '"', resolving backslash escapes.
         * @throws PacketSyntaxException if the string is not closed before end of input.
         */
        private fun readString(start: Int): Token {
            pos++
            val sb = StringBuilder()
            while (pos < input.length && input[pos] != '"') {
                if (input[pos] == '\\' && pos + 1 < input.length) {
                    sb.append(input[pos + 1])
                    pos += 2
                } else {
                    sb.append(input[pos])
                    pos++
                }
            }
            if (pos >= input.length) {
                throw PacketSyntaxException("Unterminated string starting at position $start")
            }
            pos++
            return Token(TokType.STRING, sb.toString(), start)
        }

        /**
         * @throws PacketSyntaxException if no characters can be consumed (e.g. unexpected symbol).
         */
        private fun readWord(start: Int): Token {
            val sb = StringBuilder()
            while (pos < input.length && !input[pos].isWhitespace() && WORD_STOP.none { it == input[pos] }) {
                sb.append(input[pos])
                pos++
            }
            if (sb.isEmpty()) {
                throw PacketSyntaxException("Unexpected character '${input[pos]}' at position $pos")
            }
            return Token(TokType.WORD, sb.toString(), start)
        }
    }

    private data class RawPacket(
        val command: String,
        val fields: Map<String, RawValue>,
    )

    /**
     * @class GrammarParser
     * @brief Recursive-descent parser that turns a token stream into a RawPacket
     * (command name + field map), validating packet grammar as it goes.
     */
    private class GrammarParser(
        private val tokens: List<Token>,
        private val source: String,
    ) {
        private var idx = 0

        private fun peek(): Token = tokens[idx]

        private fun advance(): Token = tokens[idx++]

        /**
         * @throws PacketSyntaxException if the next token isn't of the expected type.
         */
        private fun expect(type: TokType): Token {
            val tok = peek()
            if (tok.type != type) {
                throw PacketSyntaxException(
                    "Expected $type but found '${tok.text}' at position ${tok.pos} in: $source",
                )
            }
            return advance()
        }

        /**
         * @brief Parses "COMMAND: field: value; field: value; ...".
         * @throws PacketSyntaxException on malformed syntax or trailing content.
         */
        fun parsePacket(): RawPacket {
            val command = expect(TokType.WORD).text
            expect(TokType.COLON)

            val fields = LinkedHashMap<String, RawValue>()
            parseField(fields)
            while (peek().type == TokType.SEMI) {
                advance()
                parseField(fields)
            }
            if (peek().type != TokType.EOF) {
                val tok = peek()
                throw PacketSyntaxException("Unexpected trailing content '${tok.text}' at position ${tok.pos}")
            }
            return RawPacket(command, fields)
        }

        /**
         * @throws PacketSyntaxException if the field name is repeated within the same packet.
         */
        private fun parseField(into: MutableMap<String, RawValue>) {
            val keyTok = expect(TokType.WORD)
            val key = keyTok.text.lowercase(getDefault())
            expect(TokType.COLON)
            val value = parseValue()
            if (into.containsKey(key)) {
                throw PacketSyntaxException("Duplicate field '${keyTok.text}' at position ${keyTok.pos}")
            }
            into[key] = value
        }

        /**
         * @brief Parses a single value: string, #id, random/all/randomlist/list/region, or a bare
         * word/number.
         * @throws PacketSyntaxException on an unrecognized token or malformed construct.
         */
        private fun parseValue(): RawValue {
            val tok = peek()
            return when (tok.type) {
                TokType.STRING -> {
                    advance()
                    RStr(tok.text)
                }

                TokType.HASH -> {
                    advance()
                    val numTok = expect(TokType.WORD)
                    val id =
                        numTok.text.toLongOrNull()
                            ?: throw PacketSyntaxException(
                                "Expected an integer id after '#', got '${numTok.text}' at position ${numTok.pos}",
                            )
                    RId(id)
                }

                TokType.WORD -> {
                    when (tok.text.lowercase(getDefault())) {
                        "random" -> {
                            advance()
                            RRandom
                        }

                        "all" -> {
                            advance()
                            RAll
                        }

                        "randomlist" -> {
                            advance()
                            RRandomList(parseArgList())
                        }

                        "list" -> {
                            advance()
                            RList(parseArgList())
                        }

                        "region" -> {
                            advance()
                            val args = parseArgList()
                            if (args.size != 2) {
                                throw PacketSyntaxException(
                                    "Region(...) expects exactly 2 arguments (upperLeft, lowerRight), got ${args.size} at position ${tok.pos}",
                                )
                            }
                            RRegion(args[0], args[1])
                        }

                        else -> {
                            advance()
                            val asNum = tok.text.toDoubleOrNull()
                            if (asNum != null) RNum(asNum) else RWord(tok.text)
                        }
                    }
                }

                else -> {
                    throw PacketSyntaxException("Unexpected token '${tok.text}' at position ${tok.pos} while parsing a value")
                }
            }
        }

        private fun parseArgList(): List<RawValue> {
            expect(TokType.LPAREN)
            val items = mutableListOf<RawValue>()
            if (peek().type != TokType.RPAREN) {
                items.add(parseValue())
                while (peek().type == TokType.COMMA) {
                    advance()
                    items.add(parseValue())
                }
            }
            expect(TokType.RPAREN)
            return items
        }
    }

    /**
     * @return The Command built from raw, dispatched on raw.command.
     * @throws PacketSyntaxException if raw.command isn't a recognized command name.
     */
    private fun build(raw: RawPacket): Command =
        when (raw.command) {
            "RUN" -> buildRun(raw.fields)
            "STORE_USER" -> buildStoreUser(raw.fields)
            "STORE_MODEL" -> buildStoreModel(raw.fields)
            "STORE_SCENARIO" -> buildStoreScenario(raw.fields)
            "LOAD_AND_RUN" -> buildLoadAndRun(raw.fields)
            "PRELOAD" -> buildPreload(raw.fields)
            "RESET" -> buildReset(raw.fields)
            "CALIBRATE" -> buildCalibrate(raw.fields)
            "VALIDATE" -> buildValidate(raw.fields)
            "COMPARE" -> buildCompare(raw.fields)
            "STATUS" -> buildStatus(raw.fields)
            "CANCEL" -> buildCancel(raw.fields)
            else -> throw PacketSyntaxException("Unknown command '${raw.command}'")
        }

    /**
     * @throws PacketSyntaxException if key is absent.
     */
    private fun Map<String, RawValue>.required(key: String): RawValue =
        this[key.lowercase(getDefault())] ?: throw PacketSyntaxException("Missing required field '$key'")

    private fun Map<String, RawValue>.optional(key: String): RawValue? = this[key.lowercase(getDefault())]

    // The asX() conversions below all share the same contract: convert this RawValue to the
    // requested shape, or throw PacketSyntaxException if it isn't that shape.

    private fun RawValue.asRef(): Ref =
        when (this) {
            is RWord -> ByName(text)
            is RStr -> ByName(text)
            is RId -> ById(id)
            else -> throw PacketSyntaxException("Expected a reference (name or #id), got $this")
        }

    private fun RawValue.asString(): String =
        when (this) {
            is RStr -> text
            is RWord -> text
            RAll -> "all"
            RRandom -> "random"
            else -> throw PacketSyntaxException("Expected a word or string value, got $this")
        }

    private fun RawValue.asDouble(): Double =
        when (this) {
            is RNum -> {
                value
            }

            is RWord -> {
                text.toDoubleOrNull()
                    ?: throw PacketSyntaxException("Expected a number, got '$text'")
            }

            else -> {
                throw PacketSyntaxException("Expected a number, got $this")
            }
        }

    /**
     * @throws PacketSyntaxException if the numeric value isn't a whole number.
     */
    private fun RawValue.asInt(): Int {
        val d = asDouble()
        if (d != Math.floor(d)) throw PacketSyntaxException("Expected an integer, got $d")
        return d.toInt()
    }

    private fun RawValue.asRegionValue(): RegionValue =
        when (this) {
            is RRegion -> RegionValue(upperLeft.asRef(), lowerRight.asRef())
            else -> throw PacketSyntaxException("Expected Region(a, b), got $this")
        }

    private fun RawValue.asFieldValue(): FieldValue =
        when (this) {
            is RWord -> RefValue(ByName(text))
            is RStr -> RefValue(ByName(text))
            is RId -> RefValue(ById(id))
            is RNum -> Literal(value)
            RRandom -> RandomSelector
            RAll -> AllSelector
            is RRandomList -> RandomListSelector(options.map { it.asRef() })
            is RList -> ListValue(items.map { it.asFieldValue() })
            is RRegion -> asRegionValue()
        }

    private fun RawValue.asListValue(fieldName: String): ListValue =
        when (this) {
            is RList -> ListValue(items.map { it.asFieldValue() })
            else -> throw PacketSyntaxException("$fieldName must be List(...), got $this")
        }

    /**
     * @throws PacketSyntaxException if Repeat is present but < 1.
     */
    private fun buildRun(f: Map<String, RawValue>): RunCommand {
        val cities = f.required("City").asFieldValue()
        val scenario = f.required("Scenario").asFieldValue()
        val region = f.optional("Region")?.asFieldValue()

        val priority =
            f.optional("Priority")?.asString()?.let { p ->
                Priority.values().find { it.name.equals(p, ignoreCase = true) }
                    ?: throw PacketSyntaxException("Unknown priority '$p', expected one of low/normal/high")
            }

        val repeat = f.optional("Repeat")?.asInt() ?: 1
        if (repeat < 1) throw PacketSyntaxException("Repeat must be >= 1, got $repeat")

        val algorithm = f.optional("Algorithm")?.asFieldValue()
        val focusRegions = f.optional("FocusRegions")?.asListValue("FocusRegions")

        return RunCommand(cities, scenario, region, priority, repeat, algorithm, focusRegions)
    }

    /**
     * @throws PacketSyntaxException if Role isn't one of admin/operator/viewer.
     */
    private fun buildStoreUser(f: Map<String, RawValue>): StoreUserCommand {
        val username = f.required("Username").asString()
        val role = f.required("Role").asString().lowercase(getDefault())
        if (role !in setOf("admin", "operator", "viewer")) {
            throw PacketSyntaxException("Unknown role '$role', expected one of admin/operator/viewer")
        }
        val displayName = f.optional("DisplayName")?.asString()
        return StoreUserCommand(username, role, displayName)
    }

    /**
     * @throws PacketSyntaxException if any Parameters entry isn't a numeric Literal.
     */
    private fun buildStoreModel(f: Map<String, RawValue>): StoreModelCommand {
        val city = f.required("City").asString()
        val name = f.required("Name").asString()
        val algorithm = f.required("Algorithm").asString()
        val parameters =
            f.required("Parameters").asListValue("Parameters").items.map {
                (it as? Literal)?.value
                    ?: throw PacketSyntaxException("Parameters must be a List(...) of numbers")
            }
        val error = f.required("Error").asDouble()
        return StoreModelCommand(city, name, parameters, error, algorithm)
    }

    private fun buildStoreScenario(f: Map<String, RawValue>): StoreScenarioCommand {
        val name = f.required("Name").asString()
        val city = f.required("City").asString()
        val region = f.required("Region").asRegionValue()
        val focusRegions = f.optional("FocusRegions")?.asListValue("FocusRegions")
        val source = f.optional("Source")?.asRef()
        return StoreScenarioCommand(name, city, region, focusRegions, source)
    }

    /**
     * @throws PacketSyntaxException if Repeat is present but < 1.
     */
    private fun buildLoadAndRun(f: Map<String, RawValue>): LoadAndRunCommand {
        val file = f.required("File").asRef()
        val repeat = f.optional("Repeat")?.asInt() ?: 1
        if (repeat < 1) throw PacketSyntaxException("Repeat must be >= 1, got $repeat")
        val algorithm = f.optional("Algorithm")?.asFieldValue()
        return LoadAndRunCommand(file, repeat, algorithm)
    }

    private fun buildPreload(f: Map<String, RawValue>): PreloadCommand {
        val city = f.required("City").asString()
        val region = f.optional("Region")?.asRegionValue()
        return PreloadCommand(city, region)
    }

    /**
     * @throws PacketSyntaxException if Scope isn't one of network/model/all.
     */
    private fun buildReset(f: Map<String, RawValue>): ResetCommand {
        val scope = f.required("Scope").asString().lowercase(getDefault())
        if (scope !in setOf("network", "model", "all")) {
            throw PacketSyntaxException("Unknown reset scope '$scope', expected one of network/model/all")
        }
        return ResetCommand(scope)
    }

    private fun buildCalibrate(f: Map<String, RawValue>): CalibrateCommand {
        val city = f.required("City").asString()
        val scenario = f.required("Scenario").asRef()
        val algorithm = f.required("Algorithm").asString()
        val maxError = f.required("MaxError").asDouble()
        val repeat = f.optional("Repeat")?.asInt() ?: 1
        return CalibrateCommand(city, scenario, algorithm, maxError, repeat)
    }

    private fun buildValidate(f: Map<String, RawValue>): ValidateCommand {
        val model = f.required("Model").asRef()
        val referenceData = f.required("ReferenceData").asString()
        val threshold = f.required("Threshold").asDouble()
        return ValidateCommand(model, referenceData, threshold)
    }

    private fun buildCompare(f: Map<String, RawValue>): CompareCommand {
        val baseline = f.required("Baseline").asRef()
        val candidate = f.required("Candidate").asRef()
        val delta = f.required("Delta").asDouble()
        return CompareCommand(baseline, candidate, delta)
    }

    private fun buildStatus(f: Map<String, RawValue>): StatusCommand = StatusCommand(f.required("Job").asRef())

    private fun buildCancel(f: Map<String, RawValue>): CancelCommand = CancelCommand(f.required("Job").asRef())
}

sealed class RawValue

private data class RWord(
    val text: String,
) : RawValue()

private data class RStr(
    val text: String,
) : RawValue()

private data class RNum(
    val value: Double,
) : RawValue()

private data class RId(
    val id: Long,
) : RawValue()

private object RRandom : RawValue()

private object RAll : RawValue()

private data class RRandomList(
    val options: List<RawValue>,
) : RawValue()

private data class RList(
    val items: List<RawValue>,
) : RawValue()

private data class RRegion(
    val upperLeft: RawValue,
    val lowerRight: RawValue,
) : RawValue()

class PacketSyntaxException(
    message: String,
) : Exception(message)
