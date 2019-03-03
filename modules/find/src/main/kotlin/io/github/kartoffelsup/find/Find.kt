package io.github.kartoffelsup.find

import arrow.Kind
import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Predicate
import arrow.core.Some
import arrow.core.extensions.either.applicativeError.applicativeError
import arrow.core.extensions.either.monad.binding
import arrow.core.fix
import arrow.data.ListK
import arrow.data.NonEmptyList
import arrow.data.extensions.list.foldable.find
import arrow.data.extensions.list.traverse.flatTraverse
import arrow.data.extensions.listk.monad.monad
import arrow.data.fix
import arrow.data.k
import arrow.effects.IO
import arrow.effects.extensions.io.monadDefer.monadDefer
import arrow.effects.fix
import arrow.effects.typeclasses.MonadDefer
import io.github.kartoffelsup.argparsing.ArgParser
import io.github.kartoffelsup.argparsing.ArgParserError
import io.github.kartoffelsup.argparsing.ln
import io.github.kartoffelsup.argparsing.sn
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private operator fun <T> Predicate<T>.plus(and: Predicate<T>): Predicate<T> =
  { this(it) && and(it) }

class Find<F>(private val MD: MonadDefer<F>) : MonadDefer<F> by MD {

  private fun performSearch(
    fileIo: Kind<F, File>,
    findArgs: FindArguments
  ): Kind<F, ListK<File>> =
    fileIo.flatMap { delay { it.walkTopDown() } }
      .map { it.filter(filePredicate(findArgs)).toList().k() }

  private fun safeGetFile(path: Path): Kind<F, File> = delay { path.toFile() }

  private fun filePredicate(findArgs: FindArguments): Predicate<File> = { file ->
    val alwaysTrue: (File) -> Boolean = { true }

    // TODO kartoffelsup: glob matching (i.e. 'fileName*' etc.)
    val iNamePredicate: (File) -> Boolean = findArgs.iname
      .fold(
        { alwaysTrue },
        { { f: File -> f.name.equals(it, ignoreCase = true) } }
      )

    val namePredicate: (File) -> Boolean =
      findArgs.name.fold(
        { alwaysTrue },
        { { f: File -> f.name == it } }
      )

    val typePredicate: (File) -> Boolean =
      findArgs.type.fold(
        { alwaysTrue },
        { type: FindType ->
          { f: File ->
            when (type) {
              FindType.DIRECTORY -> f.isDirectory
              FindType.FILE -> f.isFile
            }
          }
        })

    val combinedPredicate = iNamePredicate + namePredicate + typePredicate
    combinedPredicate(file)
  }

  private fun safeCheckPathExists(pathToCheck: Kind<F, Path>): Kind<F, Path> =
    pathToCheck.flatMap { path ->
      delay { Files.exists(path) }.flatMap { exists ->
        if (exists) {
          just(path)
        } else {
          raiseError(IllegalArgumentException("File '$path' does not exist."))
        }
      }
    }

  private fun safeGetPath(it: String): Kind<F, Path> =
    delay { Paths.get(it) }

  fun program(findArgs: FindArguments): Kind<F, ListK<File>> = findArgs.paths
    .map { safeGetPath(it) }
    .map { pathIo -> safeCheckPathExists(pathIo) }
    .map { it.flatMap { path -> safeGetFile(path) } }
    .all
    .flatTraverse(ListK.monad(), MD) {
      performSearch(
        it,
        findArgs
      )
    }
    .map { it.fix() }
}

fun main(args: Array<String>) {
  val arguments: Either<ArgParserError, FindArguments> = parseArguments(args)

  val find = Find(IO.monadDefer())
  val search: IO<ListK<File>> =
    arguments.fold(
      { IO.raiseError(IllegalArgumentException(it.message)) },
      { findArgs ->
        find.program(findArgs).fix()
      })

  search
    .attempt()
    .flatMap {
      it.fold(
        { IO { System.err.println(it.message) } },
        { IO { println(it.joinToString(System.lineSeparator())) } }
      )
    }.unsafeRunSync()
}

private fun parseArguments(args: Array<String>): Either<ArgParserError, FindArguments> =
  ArgParser(Either.applicativeError(), args).run {
    binding {
      val paths: NonEmptyList<String> = list(sn("p"), ln("paths")).bind()
      val type: Option<FindType> =
        value(sn("t"), ln("type")).optional()
          .flatMap { opt ->
            parseToFindType(opt)
          }.fix().bind()

      val (name: Option<String>) = value(sn("n"), ln("name")).optional()
      val (iname: Option<String>) = value(sn("in"), ln("iname")).optional()

      FindArguments(paths, type, name, iname)
    }
  }

private fun <F> ArgParser<F>.parseToFindType(
  opt: Option<String>
): Kind<F, Option<FindType>> = opt.fold(
  { just(None) },
  { strValue ->
    optEnumValueOf<FindType>(strValue)
      .fold(
        {
          raiseError(object : ArgParserError() {
            override val message: String = "Invalid value for argument (-t, --type): '$strValue'."
          })
        },
        { just(Some(it)) }
      )
  }
)

class FindArguments(
  val paths: NonEmptyList<String>,
  val type: Option<FindType>,
  val name: Option<String>,
  val iname: Option<String>
)

enum class FindType {
  DIRECTORY, FILE
}

inline fun <reified T : Enum<T>> optEnumValueOf(name: String): Option<T> =
  enumValues<T>().toList().find { name.toLowerCase() == it.name.toLowerCase() }
