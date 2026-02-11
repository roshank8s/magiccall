package com.magiccall.voicechanger.model

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Manages the local credits system using SharedPreferences.
 * Credits are consumed when applying voice effects.
 * Users start with free credits and can "purchase" more (simulated locally).
 */
class CreditManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "magiccall_credits"
        private const val KEY_CREDITS = "credits_balance"
        private const val KEY_TOTAL_EARNED = "total_earned"
        private const val KEY_TOTAL_SPENT = "total_spent"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_DAILY_REWARD_DATE = "daily_reward_date"
        const val INITIAL_CREDITS = 50
        const val DAILY_REWARD = 5
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _credits = MutableLiveData<Int>()
    val credits: LiveData<Int> = _credits

    init {
        if (prefs.getBoolean(KEY_FIRST_LAUNCH, true)) {
            prefs.edit()
                .putInt(KEY_CREDITS, INITIAL_CREDITS)
                .putInt(KEY_TOTAL_EARNED, INITIAL_CREDITS)
                .putInt(KEY_TOTAL_SPENT, 0)
                .putBoolean(KEY_FIRST_LAUNCH, false)
                .apply()
        }
        _credits.value = getBalance()
        checkDailyReward()
    }

    fun getBalance(): Int = prefs.getInt(KEY_CREDITS, INITIAL_CREDITS)

    fun canAfford(cost: Int): Boolean = getBalance() >= cost

    fun spend(cost: Int): Boolean {
        val current = getBalance()
        if (current < cost) return false

        val newBalance = current - cost
        prefs.edit()
            .putInt(KEY_CREDITS, newBalance)
            .putInt(KEY_TOTAL_SPENT, prefs.getInt(KEY_TOTAL_SPENT, 0) + cost)
            .apply()
        _credits.value = newBalance
        return true
    }

    fun addCredits(amount: Int) {
        val newBalance = getBalance() + amount
        prefs.edit()
            .putInt(KEY_CREDITS, newBalance)
            .putInt(KEY_TOTAL_EARNED, prefs.getInt(KEY_TOTAL_EARNED, 0) + amount)
            .apply()
        _credits.value = newBalance
    }

    fun getTotalEarned(): Int = prefs.getInt(KEY_TOTAL_EARNED, INITIAL_CREDITS)
    fun getTotalSpent(): Int = prefs.getInt(KEY_TOTAL_SPENT, 0)

    private fun checkDailyReward() {
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
            .format(java.util.Date())
        val lastRewardDate = prefs.getString(KEY_DAILY_REWARD_DATE, "") ?: ""

        if (today != lastRewardDate) {
            addCredits(DAILY_REWARD)
            prefs.edit().putString(KEY_DAILY_REWARD_DATE, today).apply()
        }
    }

    /** Simulated credit packages for "purchase" */
    data class CreditPackage(
        val id: String,
        val credits: Int,
        val displayPrice: String,
        val description: String
    )

    fun getAvailablePackages(): List<CreditPackage> = listOf(
        CreditPackage("starter", 20, "$0.99", "Starter Pack"),
        CreditPackage("popular", 60, "$1.99", "Popular Pack"),
        CreditPackage("premium", 150, "$3.99", "Premium Pack"),
        CreditPackage("ultimate", 500, "$7.99", "Ultimate Pack")
    )

    /** Simulate a purchase (no real payment, just add credits locally) */
    fun simulatePurchase(packageId: String): Boolean {
        val pkg = getAvailablePackages().find { it.id == packageId } ?: return false
        addCredits(pkg.credits)
        return true
    }
}
