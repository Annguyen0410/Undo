package com.zhiend.regretnote.ui.paywall

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitOfferingsResult
import com.revenuecat.purchases.awaitPurchaseResult
import com.revenuecat.purchases.awaitRestoreResult
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.StoreProduct
import com.zhiend.regretnote.purchase.RevenueCatManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State of the paywall screen: what's being shown and how the purchase went.
 */
sealed interface PaywallUiState {
    data object Loading : PaywallUiState
    data class Ready(
        val plans: List<PaywallPlan>,
        val selectedId: String,
        val isDemo: Boolean,
    ) : PaywallUiState

    data class Error(val message: String) : PaywallUiState
}

/**
 * A single row in the paywall. [rcPackage] is the RevenueCat [Package] used to
 * purchase; it is null in the offline demo mode.
 */
data class PaywallPlan(
    val id: String,
    val title: String,
    val price: String,
    val period: String,
    val bestValue: Boolean,
    val freeTrialDays: Int?,
    val rcPackage: Package?,
)

sealed interface PurchaseState {
    data object Idle : PurchaseState
    data object Purchasing : PurchaseState
    data object Success : PurchaseState
    data class Error(val message: String) : PurchaseState
}

/** Drives the paywall: loads offerings, tracks selection, purchases & restores. */
class PaywallViewModel(application: Application) : AndroidViewModel(application) {

    val isPremium: StateFlow<Boolean> = RevenueCatManager.isPremium

    private val _uiState = MutableStateFlow<PaywallUiState>(PaywallUiState.Loading)
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private var selectedId: String? = null

    val isConfigured: Boolean get() = RevenueCatManager.isConfigured

    private val demoPlans: List<PaywallPlan> = listOf(
        PaywallPlan("demo-weekly", "Weekly", "$1.99", "per week", bestValue = false, freeTrialDays = null, rcPackage = null),
        PaywallPlan("demo-monthly", "Monthly", "$4.99", "per month", bestValue = false, freeTrialDays = null, rcPackage = null),
        PaywallPlan("demo-annual", "Annual", "$24.99", "per year", bestValue = true, freeTrialDays = 7, rcPackage = null),
    )

    init {
        load()
    }

    fun load() {
        if (!RevenueCatManager.isConfigured) {
            val plans = demoPlans.withBestValue()
            selectedId = plans.firstOrNull { it.bestValue }?.id ?: plans.first().id
            _uiState.value = PaywallUiState.Ready(
                plans = plans,
                selectedId = selectedId.orEmpty(),
                isDemo = true,
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = PaywallUiState.Loading
            val result = runCatching {
                Purchases.sharedInstance.awaitOfferingsResult()
            }.getOrNull()

            val offerings = result?.getOrNull()
            val packages = offerings?.current?.availablePackages.orEmpty()
            if (packages.isEmpty()) {
                // Distinguish a failed request (bad key, no network) from a project
                // that simply has no offering yet — otherwise a wrong key looks
                // like a missing product, and you chase the wrong problem.
                val reason = result?.exceptionOrNull()?.message
                _uiState.value = PaywallUiState.Error(
                    if (reason != null) {
                        "Couldn't reach RevenueCat: $reason"
                    } else {
                        "No products found. Set up an offering in the RevenueCat dashboard."
                    },
                )
                return@launch
            }
            val plans = packages.map { it.toPaywallPlan() }.withBestValue()
            val featured = plans.firstOrNull { it.bestValue }?.id ?: plans.firstOrNull()?.id
            selectedId = featured
            _uiState.value = PaywallUiState.Ready(
                plans = plans,
                selectedId = featured.orEmpty(),
                isDemo = false,
            )
        }
    }

    fun select(id: String) {
        selectedId = id
        val state = _uiState.value
        if (state is PaywallUiState.Ready) {
            _uiState.value = state.copy(selectedId = id)
        }
    }

    /** Purchases the currently selected package, or unlocks the demo override. */
    fun purchase(activity: Activity) {
        val plan = selectedPlan() ?: return
        viewModelScope.launch {
            _purchaseState.value = PurchaseState.Purchasing

            if (!RevenueCatManager.isConfigured) {
                // Offline demo: unlock premium locally so the flow is testable.
                RevenueCatManager.setDebugPremiumOverride(true)
                _purchaseState.value = PurchaseState.Success
                return@launch
            }

            val pkg = plan.rcPackage
            if (pkg == null) {
                _purchaseState.value = PurchaseState.Error("This plan isn't available.")
                return@launch
            }

            val params = PurchaseParams.Builder(activity, pkg).build()
            val result = Purchases.sharedInstance.awaitPurchaseResult(params)
            result.fold(
                onSuccess = { purchaseResult ->
                    RevenueCatManager.onCustomerInfoChanged(purchaseResult.customerInfo)
                    _purchaseState.value = PurchaseState.Success
                },
                onFailure = { throwable ->
                    val cancelled = throwable is com.revenuecat.purchases.PurchasesTransactionException &&
                        throwable.userCancelled
                    _purchaseState.value = if (cancelled) {
                        PurchaseState.Idle
                    } else {
                        PurchaseState.Error(throwable.message ?: "Purchase failed. Try again.")
                    }
                },
            )
        }
    }

    /** Restores an existing purchase (no-op for the offline demo). */
    fun restore() {
        if (!RevenueCatManager.isConfigured) return
        viewModelScope.launch {
            _purchaseState.value = PurchaseState.Purchasing
            val result = Purchases.sharedInstance.awaitRestoreResult()
            result.fold(
                onSuccess = { customerInfo ->
                    RevenueCatManager.onCustomerInfoChanged(customerInfo)
                    _purchaseState.value = PurchaseState.Success
                },
                onFailure = {
                    _purchaseState.value = PurchaseState.Error(
                        "Nothing to restore — no previous purchase found.",
                    )
                },
            )
        }
    }

    private fun selectedPlan(): PaywallPlan? {
        val state = _uiState.value
        if (state !is PaywallUiState.Ready) return null
        return state.plans.firstOrNull { it.id == selectedId } ?: state.plans.firstOrNull()
    }
}

private fun Package.toPaywallPlan(): PaywallPlan {
    val product = product
    val title = packageType.displayTitle(product)
    val periodText = packageType.periodText(product)
    val trialDays = product.defaultOption?.freePhase?.billingPeriod?.days
        ?: product.subscriptionOptions?.basePlan?.freePhase?.billingPeriod?.days
    return PaywallPlan(
        id = identifier,
        title = title,
        price = product.price.formatted,
        period = periodText,
        bestValue = false,
        freeTrialDays = trialDays,
        rcPackage = this,
    )
}

/** Marks the single most attractive subscription (longest billing cycle) as best value. */
private fun List<PaywallPlan>.withBestValue(): List<PaywallPlan> {
    val ranked = map { plan -> plan to plan.cycleRank() }
    val best = ranked.maxByOrNull { it.second }
    if (best == null || best.second <= 0) {
        // Nothing came from a store (offline demo): keep the preset flags.
        return this
    }
    return map { plan -> plan.copy(bestValue = plan.id == best.first.id) }
}

/**
 * Ranks a plan by its real billing cycle. The Test Store reports packages whose
 * [PackageType] is frequently unknown, so the product's own billing period is the
 * authoritative signal; [PackageType.rank] only backstops a missing period.
 */
private fun PaywallPlan.cycleRank(): Int {
    val pkg = rcPackage ?: return 0
    if (pkg.packageType == PackageType.LIFETIME) return Int.MAX_VALUE
    return pkg.product.period?.days ?: pkg.packageType.rank()
}

/** Higher rank = longer billing cycle = the plan we feature by default. */
private fun PackageType.rank(): Int = when (this) {
    PackageType.ANNUAL -> 40
    PackageType.SIX_MONTH -> 35
    PackageType.THREE_MONTH -> 30
    PackageType.TWO_MONTH -> 25
    PackageType.MONTHLY -> 20
    PackageType.WEEKLY -> 10
    else -> 0
}

private fun PackageType.displayTitle(product: StoreProduct): String = when (this) {
    PackageType.LIFETIME -> "Lifetime"
    PackageType.ANNUAL -> "Annual"
    PackageType.SIX_MONTH -> "6 months"
    PackageType.THREE_MONTH -> "3 months"
    PackageType.TWO_MONTH -> "2 months"
    PackageType.MONTHLY -> "Monthly"
    PackageType.WEEKLY -> "Weekly"
    else -> product.period?.let { it.planLabel() } ?: "Premium"
}

/** Best-effort label when the store only reports a billing period, not a package type. */
private fun Period.planLabel(): String = when (unit) {
    Period.Unit.YEAR -> if (value == 1) "Annual" else "$value years"
    Period.Unit.MONTH -> if (value == 1) "Monthly" else "$value months"
    Period.Unit.WEEK -> if (value == 1) "Weekly" else "$value weeks"
    Period.Unit.DAY -> if (value == 1) "Daily" else "$value days"
    else -> "Premium"
}

private fun PackageType.periodText(product: StoreProduct): String = when (this) {
    PackageType.LIFETIME -> "one-time"
    PackageType.ANNUAL -> "per year"
    PackageType.SIX_MONTH -> "per 6 months"
    PackageType.THREE_MONTH -> "per 3 months"
    PackageType.TWO_MONTH -> "per 2 months"
    PackageType.MONTHLY -> "per month"
    PackageType.WEEKLY -> "per week"
    else -> product.period?.let { "per ${it.describe()}" } ?: "per plan"
}

private fun Period.describe(): String {
    val unitText = when (unit) {
        Period.Unit.DAY -> if (value == 1) "day" else "days"
        Period.Unit.WEEK -> if (value == 1) "week" else "weeks"
        Period.Unit.MONTH -> if (value == 1) "month" else "months"
        Period.Unit.YEAR -> if (value == 1) "year" else "years"
        else -> "plan"
    }
    return if (value == 1) unitText else "$value $unitText"
}

private val Period.days: Int?
    get() = when (unit) {
        Period.Unit.DAY -> value
        Period.Unit.WEEK -> value * 7
        Period.Unit.MONTH -> value * 30
        Period.Unit.YEAR -> value * 365
        else -> null
    }
