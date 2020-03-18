package io.github.kartoffelsup.find

import arrow.Kind
import arrow.core.Either
import arrow.core.ListK
import arrow.core.NonEmptyList
import arrow.core.None
import arrow.core.Option
import arrow.core.Predicate
import arrow.core.Some
import arrow.core.extensions.either.monadError.monadError
import arrow.core.extensions.list.traverse.flatTraverse
import arrow.core.extensions.listk.monad.monad
import arrow.core.fix
import arrow.core.k
import arrow.core.toOption
import arrow.fx.IO
import arrow.fx.extensions.io.concurrent.concurrent
import arrow.fx.fix
import arrow.fx.typeclasses.Concurrent
import arrow.typeclasses.MonadError
import io.github.kartoffelsup.argparsing.ArgParser
import io.github.kartoffelsup.argparsing.ArgParserError
import io.github.kartoffelsup.argparsing.InvalidValue
import io.github.kartoffelsup.argparsing.longOption
import io.github.kartoffelsup.argparsing.shortOption
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private operator fun <T> Predicate<T>.plus(and: Predicate<T>): Predicate<T> =
    { this(it) && and(it) }

class Find<F>(private val MD: Concurrent<F>) : Concurrent<F> by MD {

    private fun performSearch(
        fileIo: Kind<F, File>,
        findArgs: FindArguments
    ): Kind<F, ListK<File>> =
        fileIo.flatMap { later { it.walkTopDown() } }
            .map { it.filter(filePredicate(findArgs)).toList().k() }

    private suspend fun safeGetFile(path: Kind<F, Path>): Kind<F, File> = fx.concurrent { path.bind().toFile() }

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

    private suspend fun safeCheckPathExists(pathToCheck: Path): Kind<F, Path> {
        return fx.concurrent {
            val exists = effect { Files.exists(pathToCheck) }.bind()
            if (exists) {
                pathToCheck
            } else {
                raiseError<Path>(IllegalArgumentException("File '$pathToCheck' does not exist.")).bind()
            }
        }
    }

    private suspend fun safeGetPath(it: String): Path = Paths.get(it)

    suspend fun program(findArgs: FindArguments): Kind<F, ListK<File>> {
        return findArgs.paths
            .map { path ->
                fx.concurrent {
                    val p: Path = effect { safeGetPath(path) }.bind()
                    val existingPath: Kind<F, Path> = effect { safeCheckPathExists(p) }.bind()
                    effect { safeGetFile(existingPath) }.bind().bind()
                }
            }
            .all
            .flatTraverse(ListK.monad(), MD) {
                performSearch(it, findArgs)
            }
            .map { it.fix() }
    }
}

suspend fun main(args: Array<String>) {
    val arguments: Either<ArgParserError, FindArguments> =
        parseArguments(args, Either.monadError()).fix()

    val find = Find(IO.concurrent())
    val search: IO<ListK<File>> =
        arguments.fold(
                { IO.raiseError<ListK<File>>(IllegalArgumentException(it.message)) },
                { find.program(it) })
            .fix()

    search
        .attempt()
        .flatMap {
            it.fold(
                { IO { System.err.println(it.message) } },
                { IO { println(it.joinToString(System.lineSeparator())) } }
            )
        }.unsafeRunSync()
}

private fun <F> parseArguments(
    args: Array<String>,
    ev: MonadError<F, ArgParserError>
): Kind<F, FindArguments> =
    ArgParser(ev, args).run {
        ev.run {
            list("p".shortOption(), "paths".longOption()).flatMap { p ->
                value("t".shortOption(), "type".longOption()).optional()
                    .flatMap { opt ->
                        parseToFindType(opt)
                    }.flatMap { t ->
                        value("n".shortOption(), "name".longOption()).optional().flatMap { n ->
                            value("in".shortOption(), "iname".longOption()).optional().map { i ->
                                FindArguments(p, t, n, i)
                            }
                        }
                    }
            }
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
                    raiseError(InvalidValue("Invalid value for argument (-t, --type): '$strValue'."))
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
    enumValues<T>().toList().find { name.toLowerCase() == it.name.toLowerCase() }.toOption()
