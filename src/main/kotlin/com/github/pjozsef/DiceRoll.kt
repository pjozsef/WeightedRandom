package com.github.pjozsef

import com.github.pjozsef.DiceRoll.*
import java.util.Random

fun rollD4(random: Random = Random()): Int = roll(4, random)

fun rollD6(random: Random = Random()): Int = roll(6, random)

fun rollD8(random: Random = Random()): Int = roll(8, random)

fun rollD10(random: Random = Random()): Int = roll(10, random)

fun rollD12(random: Random = Random()): Int = roll(12, random)

fun rollD20(random: Random = Random()): Int = roll(20, random)

fun roll(max: Int, random: Random = Random()): Int {
    if (max <= 0) {
        return 0
    }

    var roll = random.nextInt(max) + 1
    var result = roll
    while (roll == max) {
        roll = random.nextInt(max) + 1
        result += roll
    }
    return result
}

sealed class DiceRoll {
    abstract fun roll(): Int

    data class BaseDiceRoll(val count: Int, val base: Int, val random: Random = Random()) : DiceRoll() {
        override fun roll() = (0 until count).map { roll(base, random) }.sum()
    }

    data class ConstantRoll(val value: Int) : DiceRoll() {
        override fun roll() = value
    }

    data class CompositeSumDiceRoll(val diceRolls: List<DiceRoll>) : DiceRoll() {
        override fun roll() = diceRolls.asSequence().map { it.roll() }.sum()
    }

    data class NegatedDiceRoll(val diceRoll: DiceRoll) : DiceRoll() {
        override fun roll() = -1 * diceRoll.roll()
    }

    data class BestDiceRoll(val diceRolls: List<DiceRoll>) : DiceRoll() {
        override fun roll() = diceRolls.asSequence().map { it.roll() }.maxOrNull() ?: 0
    }
}

infix fun Int.d(i: Int) = BaseDiceRoll(this, i)

operator fun DiceRoll.plus(that: DiceRoll): DiceRoll = CompositeSumDiceRoll(listOf(this, that))
operator fun DiceRoll.plus(that: Int): DiceRoll = CompositeSumDiceRoll(listOf(this, ConstantRoll(that)))
operator fun DiceRoll.minus(that: Int): DiceRoll = CompositeSumDiceRoll(listOf(this, NegatedDiceRoll(ConstantRoll(that))))
operator fun DiceRoll.minus(that: DiceRoll): DiceRoll = CompositeSumDiceRoll(listOf(this, NegatedDiceRoll(that)))
infix fun DiceRoll.or(that: DiceRoll): DiceRoll = BestDiceRoll(listOf(this, that))
