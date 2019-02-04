package io.github.kartoffelsup.find

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.either.applicativeError.applicativeError
import arrow.core.extensions.either.monad.binding
import arrow.data.ListK
import arrow.data.NonEmptyList
import arrow.data.extensions.list.foldable.find
import arrow.data.extensions.list.traverse.flatTraverse
import arrow.data.extensions.listk.monad.monad
import arrow.data.fix
import arrow.data.k
import arrow.effects.IO
import arrow.effects.extensions.io.applicative.applicative
import arrow.effects.extensions.io.applicative.map
import arrow.effects.fix
import io.github.kartoffelsup.argparsing.ArgParser
import io.github.kartoffelsup.argparsing.LongName
import io.github.kartoffelsup.argparsing.ShortName
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main(args: Array<String>) {
  val arguments: Either<String, FindArguments> =
    ArgParser(Either.applicativeError(), { it }, args).run {
      binding {
        val paths: NonEmptyList<String> = this@run.list(ShortName("p"), LongName("paths")).bind()
        val type: Option<FindType> =
          this@run.value(ShortName("t"), LongName("type")) { optEnumValueOf<FindType>(it) }.bind()
        val name: Option<String> =
          this@run.value(ShortName("n"), LongName("name")).optional().bind()
        val iname: Option<String> =
          this@run.value(ShortName("in"), LongName("iname")).optional().bind()

        FindArguments(paths, type, name, iname)
      }
    }

  val search: IO<ListK<File>> =
    arguments.fold(
      { IO.raiseError(IllegalArgumentException(it)) },
      { findArgs ->
        findArgs.paths
          .map(::safeGetPath)
          .map(::safeCheckPathExists)
          .map { it.flatMap(::safeGetFile) }
          .all
          .flatTraverse(ListK.monad(), IO.applicative()) { performSearch(it, findArgs) }
          .map { it.fix() }
          .fix()
      })

  search
    .attempt()
    .flatMap {
      it.fold(
        { IO { System.err.println(it.message) } },
        { IO { println("Results: $it") } }
      )
    }.unsafeRunSync()
}

private fun performSearch(fileIo: IO<File>, findArgs: FindArguments): IO<ListK<File>> =
  fileIo.flatMap { IO { it.walkTopDown() } }
    .map { it.filter(filePredicate(findArgs)).toList().k() }

private fun safeGetFile(path: Path) = IO { path.toFile() }

private fun filePredicate(findArgs: FindArguments): (File) -> Boolean = { file ->
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

  iNamePredicate(file) && namePredicate(file) && typePredicate(file)
}

private fun safeCheckPathExists(pathIo: IO<Path>): IO<Path> =
  pathIo.flatMap { path ->
    IO.defer {
      if (Files.exists(path)) {
        IO.just(path)
      } else {
        IO.raiseError(IllegalArgumentException("File '$path' does not exist."))
      }
    }
  }

private fun safeGetPath(it: String) = IO { Paths.get(it) }

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
