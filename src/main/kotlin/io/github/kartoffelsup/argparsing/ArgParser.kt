package io.github.kartoffelsup.argparsing

import arrow.Kind
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.Tuple2
import arrow.core.toT
import arrow.data.Nel
import arrow.data.NonEmptyList
import arrow.typeclasses.ApplicativeError

inline class ShortName(val name: String)
inline class LongName(val name: String)

class ArgParser<F, E>(
  private val AE: ApplicativeError<F, E>,
  private val eProvider: (String) -> E,
  args: Array<String>
) : ApplicativeError<F, E> by AE {

  private val args: Option<Nel<String>> = Nel.fromList(args.toList())

  private fun argument(shortName: ShortName, longName: LongName): Option<Tuple2<Int, Nel<String>>> =
    args.flatMap { arguments ->
      val indexOfFirst = arguments.all
        .indexOfFirst { it == "-${shortName.name}" || it.startsWith("--${longName.name}") }
      if (indexOfFirst != -1) {
        Some(indexOfFirst toT arguments)
      } else {
        None
      }
    }

  fun value(shortName: ShortName, longName: LongName): Kind<F, String> =
    argument(shortName, longName)
      .flatMap { (indexOfFirst, arguments) ->
        val arg = arguments.all[indexOfFirst]
        if (arg.startsWith("--")) {
          val split = arg.split('=')
          if (split.size == 2 && split[1].isNotBlank()) {
            Some(split[1])
          } else {
            None
          }
        } else if (arguments.all.size > (indexOfFirst + 1)) {
          val potentialValue = arguments.all[indexOfFirst + 1]
          if (potentialValue.startsWith("-")) {
            None
          } else {
            Some(potentialValue)
          }
        } else {
          None
        }
      }.fold({
        AE.raiseError(eProvider("Argument '${longName.name}' ('${shortName.name}') is missing."))
      }, {
        just(it)
      })

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
