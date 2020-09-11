package io.github.kartoffelsup.argparsing

import arrow.Kind
import arrow.core.Nel
import arrow.core.NonEmptyList
import arrow.core.Tuple2
import arrow.core.toT
import arrow.typeclasses.ApplicativeError

fun String.shortOption() = ShortName(this)
fun String.longOption() = LongName(this)

inline class ShortName(val name: String)
inline class LongName(val name: String)

sealed class ArgParserError {
    abstract val message: String
}

class ArgumentMissing(shortName: ShortName, longName: LongName) : ArgParserError() {
    override val message = "Argument '${longName.name}' ('${shortName.name}') is missing."
}

class InvalidValue(msg: String) : ArgParserError() {
    override val message = msg
}

class ArgParser<F>(
    private val AE: ApplicativeError<F, ArgParserError>,
    args: Array<String>
) : ApplicativeError<F, ArgParserError> by AE {
    private val args: Nel<String>? = args.toList().takeIf { it.isNotEmpty() }?.let { NonEmptyList.fromListUnsafe(it) }

    private fun argument(shortName: ShortName, longName: LongName): Tuple2<Int, Nel<String>>? =
        args?.let { arguments ->
            val indexOfFirst = arguments.all
                .indexOfFirst { it == "-${shortName.name}" || it.startsWith("--${longName.name}") }
            if (indexOfFirst != -1) {
                indexOfFirst toT arguments
            } else {
                null
            }
        }

    fun value(shortName: ShortName, longName: LongName): Kind<F, String> =
        argument(shortName, longName)?.let { (indexOfFirst, arguments) ->
            val arg = arguments.all[indexOfFirst]
            when {
                arg.startsWith("--") -> longValueArgument(arg)
                arguments.all.size > (indexOfFirst + 1) -> shortValueArgument(arguments, indexOfFirst)
                else -> null
            }
        }?.let { just(it) } ?: AE.raiseError(ArgumentMissing(shortName, longName))

    private fun shortValueArgument(arguments: Nel<String>, indexOfFirst: Int): String? =
        arguments.all[indexOfFirst + 1].takeIf { !it.startsWith("-") }

    private fun longValueArgument(arg: String): String? =
        arg.split('=').takeIf { it.size == 2 && it[1].isNotBlank() }?.let { it[1] }

    fun <T> value(
        shortName: ShortName,
        longName: LongName,
        transform: (String) -> T
    ): Kind<F, T> =
        value(shortName, longName).map(transform)

    fun flag(shortName: ShortName, longName: LongName): Kind<F, Boolean> =
        argument(shortName, longName)?.let { just(true) } ?: just(false)

    fun list(shortName: ShortName, longName: LongName): Kind<F, NonEmptyList<String>> =
        value(shortName, longName) {
            Nel.fromListUnsafe(it.split(','))
        }

    fun <T> Kind<F, T>.optional(): Kind<F, T?> = this.handleError { null }
}
