package io.github.kartoffelsup.argparsing

import arrow.Kind
import arrow.core.None
import arrow.core.Option
import arrow.core.Predicate
import arrow.core.Some
import arrow.core.Tuple2
import arrow.core.maybe
import arrow.core.toT
import arrow.data.Nel
import arrow.data.NonEmptyList
import arrow.typeclasses.ApplicativeError

fun sn(name: String) = ShortName(name)
fun ln(name: String) = LongName(name)

inline class ShortName(val name: String)
inline class LongName(val name: String)

private inline fun <T> T.takeIf(predicate: Predicate<T>): Option<T> =
  predicate(this).maybe { this }

abstract class ArgParserError {
  abstract val message: String
}

class ArgumentMissing(shortName: ShortName, longName: LongName) : ArgParserError() {
  override val message = "Argument '${longName.name}' ('${shortName.name}') is missing."
}

class ArgParser<F>(
  private val AE: ApplicativeError<F, ArgParserError>,
  args: Array<String>
) : ApplicativeError<F, ArgParserError> by AE {
  private val args: Option<Nel<String>> = Nel.fromList(args.toList())

  private fun argument(shortName: ShortName, longName: LongName): Option<Tuple2<Int, Nel<String>>> =
    args.flatMap { arguments ->
      val indexOfFirst = arguments.all
        .indexOfFirst { it == "-${shortName.name}" || it.startsWith("--${longName.name}") }
      (indexOfFirst != -1).maybe {
        indexOfFirst toT arguments
      }
    }

  fun value(shortName: ShortName, longName: LongName): Kind<F, String> =
    argument(shortName, longName)
      .flatMap { (indexOfFirst, arguments) ->
        val arg = arguments.all[indexOfFirst]
        when {
          arg.startsWith("--") -> longValueArgument(arg)
          arguments.all.size > (indexOfFirst + 1) -> shortValueArgument(arguments, indexOfFirst)
          else -> None
        }
      }.fold(
        {
          AE.raiseError(ArgumentMissing(shortName, longName))
        },
        { just(it) }
      )

  private fun shortValueArgument(arguments: Nel<String>, indexOfFirst: Int): Option<String> =
    arguments.all[indexOfFirst + 1].takeIf { !it.startsWith("-") }

  private fun longValueArgument(arg: String): Option<String> =
    arg.split('=').takeIf { it.size == 2 && it[1].isNotBlank() }.map { it[1] }

  fun <T> value(
    shortName: ShortName,
    longName: LongName,
    transform: (String) -> T
  ): Kind<F, T> =
    value(shortName, longName).map(transform)

  fun flag(shortName: ShortName, longName: LongName): Kind<F, Boolean> =
    argument(shortName, longName)
      .fold({ just(false) }, { just(true) })

  fun list(shortName: ShortName, longName: LongName): Kind<F, NonEmptyList<String>> =
    value(shortName, longName) {
      Nel.fromListUnsafe(it.split(','))
    }

  fun <T> Kind<F, T>.optional(): Kind<F, Option<T>> = this.map { Some(it) }.handleError { None }
}
