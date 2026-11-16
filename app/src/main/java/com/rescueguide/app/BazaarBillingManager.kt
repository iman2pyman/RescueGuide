package com.rescueguide.app

import androidx.activity.result.ActivityResultRegistry
import android.content.Context
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.callback.ConnectionCallback
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.request.PurchaseRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cafe Bazaar one-time, non-consumable purchase manager.
 * Emergency BLS pathways and the 115 emergency call remain available without payment.
 */
class BazaarBillingManager(context: Context) {
    companion object {
        const val FULL_ACCESS_PRODUCT_ID = "rescueguide_full_access"
        private const val RSA_PUBLIC_KEY = "MIHNMA0GCSqGSIb3DQEBAQUAA4G7ADCBtwKBrwCWLmNva1YnwJjFd8aQS0xaA/L37lBMgR+6BTYLNIdeLmlMMNp06kVlPn4mwYqXBxpUcJ+C8d7NZhcYRzf1Ddi4gkOCeEokpcHQ8t7LCjuzPgyhAZYA2Wst/vSdJlebskBuVKcK08DSmpxV0d+whPpq9INidUtvGIAXL1n0ZZNc8YXljQNuOqqhMEsfAiw01AipQeyEBZln4regem+NPYUpBAlHFQVwUqJPTXqntjkCAwEAAQ=="
    }

    private val payment = Payment(
        context = context.applicationContext,
        config = PaymentConfiguration(
            localSecurityCheck = SecurityCheck.Enable(rsaPublicKey = RSA_PUBLIC_KEY),
            shouldSupportSubscription = false
        )
    )

    private var connection: Connection? = null
    @Volatile private var connected = false

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _status = MutableStateFlow("در حال آماده‌سازی پرداخت کافه‌بازار…")
    val status: StateFlow<String> = _status.asStateFlow()

    fun connect() {
        if (connection != null) return
        connection = payment.connect {
            connectionSucceed {
                connected = true
                _status.value = "اتصال به کافه‌بازار برقرار شد."
                restorePurchases()
            }
            connectionFailed { error ->
                connected = false
                _status.value = "اتصال به کافه‌بازار برقرار نشد: ${error.message ?: "خطای نامشخص"}"
            }
            disconnected {
                connected = false
                _status.value = "اتصال به کافه‌بازار قطع شد."
            }
        }
    }

    fun purchase(registry: ActivityResultRegistry) {
        if (!connected) {
            _status.value = "برای خرید، ابتدا اتصال کافه‌بازار را بررسی کنید و برنامه بازار را باز داشته باشید."
            return
        }

        payment.purchaseProduct(
            registry = registry,
            request = PurchaseRequest(
                productId = FULL_ACCESS_PRODUCT_ID,
                payload = "rescueguide_full_access_v1"
            )
        ) {
            purchaseFlowBegan {
                _status.value = "در حال انجام خرید در کافه‌بازار…"
            }
            failedToBeginFlow { error ->
                _status.value = "شروع فرایند خرید ناموفق بود: ${error.message ?: "خطای نامشخص"}"
            }
            purchaseSucceed { purchaseInfo ->
                if (purchaseInfo.productId == FULL_ACCESS_PRODUCT_ID) {
                    _isPremium.value = true
                    _status.value = "خرید با موفقیت تأیید شد؛ دسترسی کامل فعال است."
                } else {
                    _status.value = "شناسه محصول خریداری‌شده با محصول RescueGuide مطابقت ندارد."
                }
            }
            purchaseCanceled {
                _status.value = "خرید لغو شد؛ هیچ مبلغی به‌عنوان خرید موفق ثبت نشد."
            }
            purchaseFailed { error ->
                _status.value = "خرید تأیید نشد: ${error.message ?: "خطای نامشخص"}"
            }
        }
    }

    /** Restore permanent purchases from Bazaar; this product is intentionally never consumed. */
    fun restorePurchases() {
        if (!connected) {
            _status.value = "برای بازیابی خرید، اتصال به کافه‌بازار لازم است."
            return
        }
        payment.getPurchasedProducts {
            querySucceed { purchases ->
                val owned = purchases.any { it.productId == FULL_ACCESS_PRODUCT_ID }
                _isPremium.value = owned
                _status.value = if (owned) {
                    "خرید قبلی تأیید شد؛ دسترسی کامل فعال است."
                } else {
                    "خریدی برای این حساب کافه‌بازار پیدا نشد."
                }
            }
            queryFailed { error ->
                _status.value = "بازیابی خریدها ناموفق بود: ${error.message ?: "خطای نامشخص"}"
            }
        }
    }

    fun disconnect() {
        connected = false
        connection?.disconnect()
        connection = null
    }
}
