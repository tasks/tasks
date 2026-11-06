package org.tasks.billing

import co.touchlab.kermit.Logger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlayPurchases(
    private val store: PlayPurchaseStore,
    private val verifier: PlayPurchaseVerifier,
) {
    private val logger = Logger.withTag("PlayPurchases")

    data class State(val purchases: Map<String, PlayPurchase> = emptyMap()) {
        val subscription: PlayPurchase? = purchases.values
            .filter { it.isProSubscription }
            .minWithOrNull(PRECEDENCE)

        val hasPro: Boolean = subscription != null || purchases.containsKey(PlayPurchase.SKU_VIP)

        val purchasedThemes: Boolean = hasPro || purchases.containsKey(PlayPurchase.SKU_THEMES)
    }

    private val mutations = Mutex()

    private var storeRead = false

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    val updates: Flow<Unit> = _state.map { }

    private val _purchased = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val purchased: Flow<Unit> = _purchased

    val subscription: Flow<PlayPurchase?> =
        _state.map { it.subscription }.distinctUntilChanged()

    val hasPro: Boolean get() = _state.value.hasPro

    val purchasedThemes: Boolean get() = _state.value.purchasedThemes

    val needingAcknowledgement: List<PlayPurchase>
        get() = _state.value.purchases.values.filter { it.needsAcknowledgement }

    fun get(sku: String): PlayPurchase? = _state.value.purchases[sku]

    suspend fun load() = mutations.withLock {
        if (storeRead) {
            return@withLock
        }
        storeRead = true
        _state.value = State(store.load().verified())
    }

    suspend fun replaceAll(items: List<PlayPurchase>) = mutations.withLock {
        update(items.verified())
    }

    suspend fun add(items: List<PlayPurchase>): List<PlayPurchase> = mutations.withLock {
        val verified = items.verified()
        val known = _state.value.purchases
        val new = verified.filter { (sku, purchase) ->
            known[sku]?.purchaseToken != purchase.purchaseToken
        }
        update(known + verified)
        if (new.isNotEmpty()) {
            _purchased.emit(Unit)
        }
        verified.values.toList()
    }

    private suspend fun update(purchases: Map<String, PlayPurchase>) {
        storeRead = true
        _state.value = State(purchases)
        store.save(purchases.values.toList())
    }

    private fun List<PlayPurchase>.verified(): Map<String, PlayPurchase> =
        filter { verifier.verify(it) }
            .also { verified ->
                if (verified.size != size) {
                    logger.w { "Discarded ${size - verified.size} unverified purchase(s)" }
                }
            }
            .associateBy { it.sku }

    companion object {
        private val PRECEDENCE = compareByDescending<PlayPurchase> { it.isMonthly }
            .thenBy { it.isCanceled }
            .thenByDescending { it.subscriptionPrice ?: 0 }
    }
}
