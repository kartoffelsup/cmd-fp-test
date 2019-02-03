package io.github.kartoffelsup.find

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.either.applicativeError.applicativeError
import arrow.core.extensions.either.monad.binding
import arrow.core.extensions.either.traverse.sequence
import arrow.core.fix
import arrow.data.NonEmptyList
import arrow.data.extensions.list.foldable.find
import arrow.data.extensions.list.traverse.sequence
import arrow.data.fix
import arrow.effects.IO
import arrow.effects.extensions.io.applicative.applicative
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
        // TODO kartoffelsup: name and iname should be mutually exclusive
        FindArguments(paths, type, name, iname)
      }
    }

  arguments.map { findArgs ->
    val pathsIo: IO<NonEmptyList<Path>> = findArgs.paths
      .map(::safeGetPath)
      .traverse(IO.applicative(), safeCheckPathExists()).fix()

    pathsIo.flatMap { paths: NonEmptyList<Path> ->
      paths.all.map { path ->
        IO { path.toFile() }
          .flatMap { IO { it.walkTopDown() } }
          .map { it.filter(predicates(findArgs)).toList() }
      }.sequence(IO.applicative()).fix()
        .map { it.fix().flatten() }
    }
  }.sequence(IO.applicative()).fix().map { it.fix() }
    .flatMap {
      it.fold({ IO { println(it) } }, { IO { println("Results: $it") } })
    }.attempt().unsafeRunSync()
    .fold({ println(it.message) }, { Unit })
}

private fun predicates(findArgs: FindArguments): (File) -> Boolean = { file ->
  val alwaysTrue: (File) -> Boolean = { true }

  // TODO kartoffelsup: glob matching (i.e. 'fileName*' etc.)
  val iNamePredicate: (File) -> Boolean = findArgs.iname
    .fold(
      { alwaysTrue },
      { { f: File -> f.name.equals(it, ignoreCase = true) } })

  val namePredicate: (File) -> Boolean =
    findArgs.name.fold({ alwaysTrue }, { { f: File -> f.name == it } })

  val typePredicate: (File) -> Boolean =
    findArgs.type.fold(
      { alwaysTrue },
      {
        { f: File ->
          when (it) {
            FindType.DIRECTORY -> f.isDirectory
            FindType.FILE -> f.isFile
          }
        }
      })

  iNamePredicate(file) && namePredicate(file) && typePredicate(file)
}

private fun safeCheckPathExists(): (IO<Path>) -> IO<Path> = { pathIo: IO<Path> ->
  pathIo.flatMap { path ->
    IO.defer {
      if (Files.exists(path)) {
        IO.just(path)
      } else {
        IO.raiseError(IllegalArgumentException("File $path does not exist."))
      }
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
