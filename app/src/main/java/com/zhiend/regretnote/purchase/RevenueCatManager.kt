package com.zhiend.regretnote.purchase

import android.content.Context
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.zhiend.regretnote.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thin wrapper around the RevenueCat SDK.
 *
 * Everything is gated on [isConfigured]: until an API key is provided through
 * `local.properties` (see README → "RevenueCat setup") the app runs fully local
 * and premium features stay locked. In debug builds a "simulate premium" override
 * (see the settings sheet) lets you preview premium features without any store.
 *
 * Debug builds use a Test Store key, so the whole purchase flow — offerings,
 * purchase, entitlement, restore — works before a store listing exists.
 */
object RevenueCatManager {

    const val ENTITLEMENT_ID = "premium"

    private const val TEST_STORE_KEY_PREFIX = "test_"

    val isConfigured: Boolean = BuildConfig.REVENUECAT_API_KEY.isNotBlank()

    /** True when this build runs against RevenueCat's Test Store (no real money). */
    val isTestStore: Boolean = BuildConfig.REVENUECAT_API_KEY.startsWith(TEST_STORE_KEY_PREFIX)

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    @Volatile
    private var debugOverride = false

    fun configure(context: Context) {
        if (!isConfigured) return
        Purchases.debugLogsEnabled = BuildConfig.DEBUG
        val configuration = PurchasesConfiguration.Builder(context, BuildConfig.REVENUECAT_API_KEY).build()
        Purchases.configure(configuration)
        refreshPremium()
    }

    /** Re-reads customer info (call after a purchase/restore or on app start). */
    fun refreshPremium() {
        if (!isConfigured) {
            _isPremium.value = debugOverride
            return
        }
        Purchases.sharedInstance.getCustomerInfo(
            CacheFetchPolicy.CACHED_OR_FETCHED,
            object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    _isPremium.value = debugOverride || isEntitled(customerInfo)
                }

                override fun onError(error: PurchasesError) {
                    // Keep the previous value; the next refresh will retry.
                }
            },
        )
    }

    /** Called from the paywall when a purchase or restore completes. */
    fun onCustomerInfoChanged(customerInfo: CustomerInfo?) {
        _isPremium.value = debugOverride || isEntitled(customerInfo)
    }

    /** Debug-only override so premium features can be previewed without a store. */
    fun setDebugPremiumOverride(value: Boolean) {
        debugOverride = value
        if (isConfigured) {
            refreshPremium()
        } else {
            _isPremium.value = value
        }
    }

    private fun isEntitled(customerInfo: CustomerInfo?): Boolean =
        customerInfo?.entitlements?.get(ENTITLEMENT_ID)?.isActive == true
}
