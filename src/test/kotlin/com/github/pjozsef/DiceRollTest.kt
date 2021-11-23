package com.github.pjozsef

import com.github.pjozsef.DiceRoll.*
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.kotlintest.assertSoftly
import io.kotlintest.data.suspend.forall
import io.kotlintest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotlintest.matchers.types.shouldBeTypeOf
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import io.kotlintest.tables.row
import java.util.*

class DiceUtilsKtTest : FreeSpec({
    fun getRandom(vararg boundaries: Int): Random = mock {
        val (head, tail) = boundaries.map { it - 1 }.run { first() to drop(1).toTypedArray() }

        on { nextInt(any()) }.doReturn(head, *tail)
    }

    "rolls values via Random" {
        val max = 4
        val random = getRandom(1, 2, 3)

        1 shouldBe roll(max, random)
        2 shouldBe roll(max, random)
        3 shouldBe roll(max, random)
    }

    "rolls" - {
        forall(
            row("with acing", 4, intArrayOf(4, 2), 6),
            row("with multiple acing", 8, intArrayOf(8, 8, 8, 3), 27),
            row("with 0 result", 0, intArrayOf(0), 0)
        ) { test, max, rolls, expected ->
            test {
                roll(max, getRandom(*rolls)) shouldBe expected
            }
        }
    }

    "shorthand rolls" - {

        val random = {
            Random(100)
        }

        forall(
            row("1d4", ::rollD4, 4),
            row("1d6", ::rollD6, 6),
            row("1d8", ::rollD8, 8),
            row("1d10", ::rollD10, 10),
            row("1d12", ::rollD12, 12),
            row("1d20", ::rollD20, 20),
        ) { test, rollFunction, max ->
            test {
                rollFunction(random()) shouldBe roll(max, random())
            }
        }
    }

    "DiceRoll" - {
        forall(
            row("base", BaseDiceRoll(3, 6, getRandom(2, 4, 3)), 9),
            row("constant", ConstantRoll(8), 8),
            row("negated", NegatedDiceRoll(ConstantRoll(3)), -3),
            row(
                "best", BestDiceRoll(
                    listOf(
                        BaseDiceRoll(2, 4, getRandom(3, 2)),
                        NegatedDiceRoll(ConstantRoll(30)),
                        ConstantRoll(8)
                    )
                ), 8
            ),
            row(
                "composite sum", CompositeSumDiceRoll(
                    listOf(
                        BaseDiceRoll(1, 10, getRandom(9)),
                        ConstantRoll(3),
                        NegatedDiceRoll(ConstantRoll(10)),
                        CompositeSumDiceRoll(
                            listOf(
                                ConstantRoll(3),
                                ConstantRoll(4),
                            )
                        )
                    )
                ), 9
            ),
        ) { test, dice, expected ->
            test {
                dice.roll() shouldBe expected
            }
        }
    }

    "DiceRoll DSL" - {

        "1 d 4" {
            (1 d 4).shouldBeEqualToIgnoringFields(BaseDiceRoll(1, 4), BaseDiceRoll::random)
        }

        "1 d 4 + 2" {
            val roll = ((1 d 4) + 2)
            roll.shouldBeTypeOf<CompositeSumDiceRoll> {
                assertSoftly {
                    it.diceRolls[0].shouldBeEqualToIgnoringFields(BaseDiceRoll(1, 4), BaseDiceRoll::random)
                    it.diceRolls[1] shouldBe ConstantRoll(2)
                }
            }
        }

        "1 d 4 - 2" {
            val roll = ((1 d 4) - 2)
            roll.shouldBeTypeOf<CompositeSumDiceRoll> {
                assertSoftly {
                    it.diceRolls[0].shouldBeEqualToIgnoringFields(BaseDiceRoll(1, 4), BaseDiceRoll::random)
                    it.diceRolls[1] shouldBe NegatedDiceRoll(ConstantRoll(2))
                }
            }
        }

        "1 d 4 + 2 d 6" {
            val roll = (1 d 4) + (2 d 6)
            roll.shouldBeTypeOf<CompositeSumDiceRoll> {
                assertSoftly {
                    it.diceRolls[0].shouldBeEqualToIgnoringFields(BaseDiceRoll(1, 4), BaseDiceRoll::random)
                    it.diceRolls[1].shouldBeEqualToIgnoringFields(BaseDiceRoll(2, 6), BaseDiceRoll::random)
                }
            }
        }

        "1 d 4 - 2 d 6" {
            val roll = (1 d 4) - (2 d 6)
            roll.shouldBeTypeOf<CompositeSumDiceRoll> {
                assertSoftly {
                    it.diceRolls[0].shouldBeEqualToIgnoringFields(BaseDiceRoll(1, 4), BaseDiceRoll::random)
                    it.diceRolls[1].shouldBeTypeOf<NegatedDiceRoll> {
                        it.diceRoll.shouldBeEqualToIgnoringFields(BaseDiceRoll(2, 6), BaseDiceRoll::random)
                    }
                }
            }
        }

        "1 d 4 or 2 d 6" {
            val roll = (1 d 4) or (2 d 6)
            roll.shouldBeTypeOf<BestDiceRoll> {
                assertSoftly {
                    it.diceRolls[0].shouldBeEqualToIgnoringFields(BaseDiceRoll(1, 4), BaseDiceRoll::random)
                    it.diceRolls[1].shouldBeEqualToIgnoringFields(BaseDiceRoll(2, 6), BaseDiceRoll::random)
                }
            }
        }

    }
})
