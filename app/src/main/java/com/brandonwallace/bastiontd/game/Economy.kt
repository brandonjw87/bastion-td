package com.brandonwallace.bastiontd.game

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow
import kotlin.math.roundToLong

object Economy {
    /**
     * Hidden compound interest rate on unspent credits per wave clear (16.769%).
     * Folded silently into wave performance payouts; never shown as a separate line in UI or docs.
     */
    val INTEREST_RATE: BigDecimal = BigDecimal("0.16769")
    private val HUNDRED_THOUSAND = BigDecimal(100_000)
    const val MAX_CREDITS: Long = 9_000_000_000_000_000L

    data class InterestResult(
        val wholePaid: Long,
        val newCarryScaled: Int // 0..99_999 (hundred-thousandths of a credit)
    )

    /**
     * Computes exact compound interest on [unspentCredits] using BigDecimal with fractional carry
     * [carryScaled] (in units of 1e-5 credits) so 0.16769 compounds with zero rounding loss.
     */
    fun computeWaveInterest(unspentCredits: Long, carryScaled: Int): InterestResult {
        if (unspentCredits <= 0L) return InterestResult(0L, carryScaled.coerceIn(0, 99_999))
        val principal = BigDecimal.valueOf(unspentCredits)
        val carry = BigDecimal.valueOf(carryScaled.coerceIn(0, 99_999).toLong())
            .divide(HUNDRED_THOUSAND, 5, RoundingMode.UNNECESSARY)
        val totalExact = principal.multiply(INTEREST_RATE).add(carry)
        val whole = totalExact.setScale(0, RoundingMode.FLOOR)
        val frac = totalExact.subtract(whole)
        val nextCarry = frac.multiply(HUNDRED_THOUSAND).setScale(0, RoundingMode.HALF_UP).toInt()
            .coerceIn(0, 99_999)
        val paid = whole.toLong().coerceAtLeast(0L)
        return InterestResult(paid, nextCarry)
    }

    /** Base enemy HP for a given wave (smooth exponential + polynomial curve for endless scaling). */
    fun baseEnemyHpForWave(wave: Int): Float {
        val w = wave.coerceAtLeast(1)
        val poly = 52f + (w - 1) * 24f + (w - 1) * (w - 1) * 2.8f
        val exp = 1.075f.pow((w - 1).coerceAtMost(160))
        return poly * exp
    }

    /** Base credit reward for killing a standard enemy on [wave]. */
    fun baseKillRewardForWave(wave: Int): Long {
        val w = wave.coerceAtLeast(1)
        return (12.0 + w * 2.6 + (w * w) * 0.08).roundToLong()
    }

    /** Base wave-clear performance bonus before tech multipliers and hidden interest. */
    fun baseWaveClearBonus(wave: Int): Long {
        val w = wave.coerceAtLeast(1)
        return (55.0 + w * 22.0 + (w * w) * 1.15).roundToLong()
    }

    /** Safe addition capped at [MAX_CREDITS] to prevent Long overflow in deep endless runs. */
    fun addCreditsSafe(current: Long, delta: Long): Long {
        if (delta <= 0L) return (current + delta).coerceAtLeast(0L)
        val room = MAX_CREDITS - current
        return if (delta >= room) MAX_CREDITS else current + delta
    }

    fun formatCompact(value: Long): String {
        val v = kotlin.math.abs(value)
        val sign = if (value < 0) "-" else ""
        return when {
            v >= 1_000_000_000_000L -> "%s%.2fT".format(sign, v / 1_000_000_000_000.0)
            v >= 1_000_000_000L -> "%s%.2fB".format(sign, v / 1_000_000_000.0)
            v >= 1_000_000L -> "%s%.2fM".format(sign, v / 1_000_000.0)
            v >= 10_000L -> "%s%.1fK".format(sign, v / 1_000.0)
            else -> "%,d".format(value)
        }
    }
}
