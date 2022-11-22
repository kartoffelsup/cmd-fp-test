package io.github.kartoffelsup.argparsing

import arrow.core.Either
import arrow.core.Nel
import arrow.core.NonEmptyList
import arrow.core.handleError
import arrow.core.right
import arrow.core.rightIfNotNull
import arrow.core.toNonEmptyListOrNull

fun String.shortOption() = ShortName(this)
fun String.longOption() = LongName(this)

@JvmInline
value class ShortName(val name: String)

@JvmInline
value class LongName(val name: String)

sealed class ArgParserError {
    abstract val message: String
}

class ArgumentMissing(shortName: ShortName, longName: LongName) : ArgParserError() {
    override val message = "Argument '${longName.name}' ('${shortName.name}') is missing."
}

class ArgParser(
    args: Array<String>
) {
    private val args: Nel<String>? = args.toList().takeIf { it.isNotEmpty() }?.toNonEmptyListOrNull()

    private fun argument(shortName: ShortName, longName: LongName): Pair<Int, Nel<String>>? =
        args?.let { arguments ->
            val indexOfFirst = arguments.all
                .indexOfFirst { it == "-${shortName.name}" || it.startsWith("--${longName.name}") }
            if (indexOfFirst != -1) {
                indexOfFirst to arguments
            } else {
                null
            }
        }

    fun value(shortName: ShortName, longName: LongName): Either<ArgParserError, String> =
        argument(shortName, longName)?.let { (indexOfFirst, arguments) ->
            val arg: String = arguments.all[indexOfFirst]
            when {
                arg.startsWith("--") -> longValueArgument(arg)
                arguments.all.size > (indexOfFirst + 1) -> shortValueArgument(arguments, indexOfFirst)
                else -> null
            }
        }.rightIfNotNull { ArgumentMissing(shortName, longName) }


    private fun shortValueArgument(arguments: Nel<String>, indexOfFirst: Int): String? =
        arguments.all[indexOfFirst + 1].takeIf { !it.startsWith("-") }

    private fun longValueArgument(arg: String): String? =
        arg.split('=').takeIf { it.size == 2 && it[1].isNotBlank() }?.let { it[1] }

    fun <T> value(
        shortName: ShortName,
        longName: LongName,
        transform: (String) -> T
    ): Either<ArgParserError, T> =
        value(shortName, longName).map(transform)

    fun flag(shortName: ShortName, longName: LongName): Either<ArgParserError, Boolean> =
        argument(shortName, longName)?.let { true.right() } ?: false.right()

    fun list(shortName: ShortName, longName: LongName): Either<ArgParserError, NonEmptyList<String>> =
        value(shortName, longName) {
           it.split(',').toNonEmptyListOrNull()!!
        }

    fun <T> Either<ArgParserError, T>.optional(): Either<ArgParserError, T?> = this.handleError { null }
}
