package io.github.kartoffelsup.argparsing

import arrow.core.Either
import arrow.core.Nel
import arrow.core.NonEmptyList
import io.kotest.assertions.fail
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

internal class ArgParserTest : DescribeSpec({
    describe("parse value arguments") {
        val args: Array<String> =
            arrayOf(
                "--foo=bar",
                "-a",
                "b",
                "-b",
                "-c",
                "--emptyvalue=",
                "--foo-bar=asdf"
            )
        val argParser = ArgParser(args)

        it("parses long-name value argument") {
            val shortName = ShortName("f")
            val longName = LongName("foo")
            val value: Either<ArgParserError, String> = argParser.value(shortName, longName)
            value.fold({
                fail("A value should be found for ${shortName.name}, ${longName.name}")
            }, {
                it shouldBe "bar"
            })
        }

        it("parses long-name value argument with dashes") {
            val shortName = ShortName("fb")
            val longName = LongName("foo-bar")
            val value: Either<ArgParserError, String> = argParser.value(shortName, longName)
            value.fold({
                fail("A value should be found for ${shortName.name}, ${longName.name}")
            }, {
                it shouldBe "asdf"
            })
        }

        it("parses short-name value argument") {
            val shortName = ShortName("a")
            val longName = LongName("abc")
            val value: Either<ArgParserError, String> = argParser.value(shortName, longName)
            value.fold({
                fail("A value should be found for ${shortName.name}, ${longName.name}")
            }, {
                it shouldBe "b"
            })
        }

        it("errors if argument not found") {
            val shortName = ShortName("ne")
            val longName = LongName("nonexistent")
            val value: Either<ArgParserError, String> = argParser.value(shortName, longName)
            value.fold({
                it.message shouldBe "Argument '${longName.name}' ('${shortName.name}') is missing."
            }, {
                fail("Argument should not have been found")
            })
        }

        it("errors if argument has no value") {
            val shortName = ShortName("b")
            val longName = LongName("booyakasha")
            val value: Either<ArgParserError, String> = argParser.value(shortName, longName)
            value.fold({
                it.message shouldBe "Argument '${longName.name}' ('${shortName.name}') is missing."
            }, {
                fail("Argument should not have been found")
            })
        }

        it("errors if long argument has no value") {
            val shortName = ShortName("ev")
            val longName = LongName("emptyvalue")
            val value: Either<ArgParserError, String> = argParser.value(shortName, longName)
            value.fold({
                it.message shouldBe "Argument '${longName.name}' ('${shortName.name}') is missing."
            }, {
                fail("Argument should not have been found")
            })
        }
    }

    describe("parse list arguments") {
        val args: Array<String> =
            arrayOf(
                "--list=foo,bar,baz,",
                "-al",
                "b,c,d",
                "--listnovalue=",
                "-bl",
                "-a"
            )
        val argParser = ArgParser(args)

        it("parses long-name list argument") {
            val shortName = ShortName("l")
            val longName = LongName("list")
            val value: Either<ArgParserError, NonEmptyList<String>> =
                argParser.list(shortName, longName)
            value.fold({
                fail("A value should be found for ${shortName.name}, ${longName.name}")
            }, {
                it shouldBe Nel.fromListUnsafe(listOf("foo", "bar", "baz", ""))
            })
        }

        it("parses short-name list argument") {
            val shortName = ShortName("al")
            val longName = LongName("alist")
            val value: Either<ArgParserError, NonEmptyList<String>> = argParser.list(shortName, longName)
            value.fold({
                fail("A value should be found for ${shortName.name}, ${longName.name}")
            }, {
                it shouldBe Nel.fromListUnsafe(listOf("b", "c", "d"))
            })
        }

        it("errors if long list argument has no value") {
            val shortName = ShortName("lnv")
            val longName = LongName("listnovalue")
            val value: Either<ArgParserError, String> = argParser.value(shortName, longName)
            value.fold({
                it.message shouldBe "Argument '${longName.name}' ('${shortName.name}') is missing."
            }, {
                fail("Argument should not have been found")
            })
        }

        it("errors if short list argument has no value") {
            val shortName = ShortName("a")
            val longName = LongName("anemptylistargument")
            val value: Either<ArgParserError, String> = argParser.value(shortName, longName)
            value.fold({
                it.message shouldBe "Argument '${longName.name}' ('${shortName.name}') is missing."
            }, {
                fail("Argument should not have been found")
            })
        }

        it("errors if short list argument has no value (2)") {
            val shortName = ShortName("bl")
            val longName = LongName("bloodylist")
            val value: Either<ArgParserError, String> = argParser.value(shortName, longName)
            value.fold({
                it.message shouldBe "Argument '${longName.name}' ('${shortName.name}') is missing."
            }, {
                fail("Argument should not have been found")
            })
        }
    }

    describe("parse flags") {
        val args: Array<String> =
            arrayOf(
                "--longflag",
                "-sf"
            )
        val argParser = ArgParser(args)

        it("parse short style flag") {
            val flag = argParser.flag(ShortName("sf"), LongName("shortflag"))
            flag.fold({
                fail("Parsing flags should never fail.")
            }, {
                it shouldBe true
            })
        }

        it("parse long style flag") {
            val flag = argParser.flag(ShortName("lf"), LongName("longflag"))
            flag.fold({
                fail("Parsing flags should never fail.")
            }, {
                it shouldBe true
            })
        }

        it("parse missing flag") {
            val flag = argParser.flag(ShortName("mf"), LongName("missingflag"))
            flag.fold({
                fail("Parsing flags should never fail.")
            }, {
                it shouldBe false
            })
        }
    }
})
