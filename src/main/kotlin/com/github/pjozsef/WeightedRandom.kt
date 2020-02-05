package com.github.pjozsef

import java.util.Random

class WeightedCoin(val trueProbability: Double, val random: Random = Random()) {

    init {
        require(trueProbability in 0.0..1.0) {
            "trueProbability must be between 0.0 and 1.0, but was: $trueProbability"
        }
    }

    fun flip(): Boolean {
        return random.nextDouble() <= trueProbability
    }
}

class WeightedDie<T>(probabilities: Map<T, Double>, val random: Random = Random()) {
    constructor(values: List<T>, probabilities: List<Double>, random: Random = Random()) : this(values.zip(probabilities), random)
    constructor(probabilities: List<Pair<T, Double>>, random: Random = Random()) : this(probabilities.toMap(), random)

    private val n = probabilities.size
    private val alias = IntArray(n)
    private val prob = DoubleArray(n)
    private val values: List<T>

    init {
        val probList = probabilities.toList()
        probList.map { it.second }.let {
            require(Math.abs(1.0 - it.sum()) <= 0.0001) { "Probabilities must add up to 1.0" }
            require(it.none { it < 0.0 }) { "Probabilities must not be negative" }
        }
        values = probList.unzip().first

        val small = mutableListOf<Int>()
        val large = mutableListOf<Int>()

        val scaledProbs = probList.zip(0 until n) { (_, percent), index ->
            index to percent
        }.map {
            it.first to it.second * n
        }.onEach { (i, value) ->
            if (value < 1.0) small.add(i) else large.add(i)
        }.map {
            it.second
        }.toTypedArray()

        while (small.isNotEmpty() && large.isNotEmpty()) {
            val l = small.removeAt(0)
            val g = large.removeAt(0)
            prob[l] = scaledProbs[l]
            alias[l] = g
            val pgtemp = scaledProbs[g] + scaledProbs[l] - 1
            scaledProbs[g] = pgtemp
            if (pgtemp < 1) small.add(g) else large.add(g)
        }

        while (large.isNotEmpty()) {
            val g = large.removeAt(0)
            prob[g] = 1.0
        }

        while (small.isNotEmpty()) {
            val l = small.removeAt(0)
            prob[l] = 1.0
        }
    }

    fun roll(): T = random.nextInt(n).let { i ->
        if (flipCoin(prob[i], random)) values[i] else values[alias[i]]
    }
}

fun flipCoin(trueProbability: Double, random: Random = Random()): Boolean {
    return WeightedCoin(trueProbability, random).flip()
}

fun intWeightedDice(probabilities: List<Double>, random: Random = Random()) =
    WeightedDie(probabilities.indices.toList().zip(probabilities).toMap(), random)
