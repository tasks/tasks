package org.tasks.billing

import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.LocalBroadcastManager
import org.tasks.data.dao.CaldavDao
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.preferences.Preferences
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class InventoryTest : InjectingTestCase() {

    @Inject lateinit var preferences: Preferences
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var signatureVerifier: SignatureVerifier
    @Inject lateinit var caldavDao: CaldavDao

    lateinit var inventory: Inventory

    @Test
    fun monthlyIsPro() {
        withPurchases(monthly01)

        assertTrue(inventory.hasPro)
        assertEquals(1, inventory.subscription.value?.subscriptionPrice)
    }

    @Test
    fun testMonthlyUpgrade() {
        withPurchases(monthly01, monthly03)

        assertEquals(3, inventory.subscription.value?.subscriptionPrice)
    }

    @Test
    fun testMonthlyOverAnnual() {
        withPurchases(monthly01, annual03)

        assertTrue(inventory.subscription.value!!.isMonthly)
    }

    @Test
    fun isCancelled() {
        withPurchases(annual03Cancelled)

        assertTrue(inventory.subscription.value!!.isCanceled)
    }

    @Test
    fun cancelledIsStillPro() {
        withPurchases(annual03Cancelled)

        assertTrue(inventory.subscription.value!!.isProSubscription)
    }

    private fun withPurchases(vararg purchases: String) {
        val asPurchases =
            purchases
                .map(::JSONObject)
                .map {
                    com.android.billingclient.api.Purchase(
                        it.getString("zza"),
                        it.getString("zzb")
                    )
                }
                .map(::Purchase)
        preferences.setPurchases(asPurchases)
        runOnMainSync {
            inventory = Inventory(
                    context,
                    preferences,
                    signatureVerifier,
                    localBroadcastManager,
                    caldavDao
            )
        }
    }

    companion object {
        private const val annual03 = """{"zza":"{\"orderId\":\"GPA.3372-3222-8630-38485\",\"packageName\":\"org.tasks\",\"productId\":\"annual_03\",\"purchaseTime\":1603917413542,\"purchaseState\":0,\"purchaseToken\":\"ciogggalpbohflhmjamciehl.AO-J1OyjBpBOnwKCOBSN0Cil7yM65ZYnkn9nGqZO1n3nsHIF_LHv0ZuQqNnThB_JuCt-s9wBij1PyOCoP8axqVILXSiavcyPmg\",\"autoRenewing\":true}","mParsedJson":{"nameValuePairs":{"orderId":"GPA.3372-3222-8630-38485","packageName":"org.tasks","productId":"annual_03","purchaseTime":1603917413542,"purchaseState":0,"purchaseToken":"ciogggalpbohflhmjamciehl.AO-J1OyjBpBOnwKCOBSN0Cil7yM65ZYnkn9nGqZO1n3nsHIF_LHv0ZuQqNnThB_JuCt-s9wBij1PyOCoP8axqVILXSiavcyPmg","autoRenewing":true}},"zzb":"Od2ulMjFethYNdA1rTm7AvNMyfFgefCaZhtBeYuTHlMB/XbEd/m9noRlKWMnShFthnQyw97CfrB86aaB52OSWm9pGkPzaRtOJPyL8BJHP9LEjXHOQIQ2Nx9zRF30+EWgV4O0IyeL/o5eUvTQRNnfyUXFdJQRLiKTblQojO6mTCX2fA6lTAntjJpbTbYGuYZjg782gX5HvmwQN5CJu7ZVZCH9AmsnAqZgb7h+MXhquQjv0L4pDVDp3dyDDwgpCAvSRy3550ZANPfNGsQpPr9Iv9IGoK0/INZRrq63VEEAz2mBGkzJgyQUYVtT6AylvNrqdo0w17hs0MLfsj6dwvSlYw\u003d\u003d"}"""
        private const val monthly01 = """{"zza":"{\"orderId\":\"GPA.3369-0544-4429-52590\",\"packageName\":\"org.tasks\",\"productId\":\"monthly_01\",\"purchaseTime\":1603912474316,\"purchaseState\":0,\"purchaseToken\":\"iibbhlkglfjcgdebphiklajb.AO-J1OyJd2kCytLMfT8Vszibf_E99ffLha5cHgOM8o3gYPKy1kD8nIZh0hcEEyOPe7fsdFJrR1-gtvg8WKLFNJoCdqrerJ2Z6Q\",\"autoRenewing\":true}","mParsedJson":{"nameValuePairs":{"orderId":"GPA.3369-0544-4429-52590","packageName":"org.tasks","productId":"monthly_01","purchaseTime":1603912474316,"purchaseState":0,"purchaseToken":"iibbhlkglfjcgdebphiklajb.AO-J1OyJd2kCytLMfT8Vszibf_E99ffLha5cHgOM8o3gYPKy1kD8nIZh0hcEEyOPe7fsdFJrR1-gtvg8WKLFNJoCdqrerJ2Z6Q","autoRenewing":true}},"zzb":"UK7fdCY61QownZW8jDLB1myUKf1llFh9rj5I7P8V03AgdA6LGpEUiCvMvCqHfMGpY3VewmawezqiCUdGWGr+UgS+6QHEuFjpO8L+E36JUDqlU9uoGrTsXLI1gXQNQElGJ71DrKlFBbyyBHSeGWnzijcq4DyyHQzpmsqijxfs0KGjkta2TiOCtyxS+YA569xaGi6lcLGTyMEe7wS5bcjdfwFir0uVtCP+iqjoEd3kt4/03l9BEJYgf8eBxI0vrm4O+jYDJu8gGMTSQZiSqb0wN4sq8D9ksV+BcI4az6LVa1d6nuD+ob0Woe0/P2uoXG8nTEZJnrAZjkG6q8736HP6rw\u003d\u003d"}"""
        private const val monthly03 = """{"zza":"{\"orderId\":\"GPA.3348-6247-8527-38213\",\"packageName\":\"org.tasks\",\"productId\":\"monthly_03\",\"purchaseTime\":1603912730414,\"purchaseState\":0,\"purchaseToken\":\"cmomnojdllomadpoinoabbkd.AO-J1OypdY4iXbMrF21L6Evn3wZSccwiBq-d55G1BVcrkwuH69zOuqb35yZnVynEb9KEvnQvgYQsUpv1AD5749iU-eDo4TRV5A\",\"autoRenewing\":true}","mParsedJson":{"nameValuePairs":{"orderId":"GPA.3348-6247-8527-38213","packageName":"org.tasks","productId":"monthly_03","purchaseTime":1603912730414,"purchaseState":0,"purchaseToken":"cmomnojdllomadpoinoabbkd.AO-J1OypdY4iXbMrF21L6Evn3wZSccwiBq-d55G1BVcrkwuH69zOuqb35yZnVynEb9KEvnQvgYQsUpv1AD5749iU-eDo4TRV5A","autoRenewing":true}},"zzb":"FkkW5FPw2elWnenIoQT7U5BnL2prcuK0GJEaHKtObPujSGRfJWFfThe3yuQ0w9AuTO0EDbm7LbJI44AiVJmpva3Iz3U2np2eNBuUAJIw9eECvQjEvuYk6Vq7LIgJwEsTyA8xRwjLJm+R1mmMWOxURmvDVBgDTHCOJsdUI9s52CSTQf2Ek+XABHugrMJudO43LzDuV2sP9mCqXUnSLbBXe3zZKyhhuz7gD+/5yavkRsPOVcZnsJetdxEmnrip8JEvgtHAvciPkvSD/fYeXdAlY2HiQWK/S0/I+yRaCEK8V+Um78ibbYc4Ng5NcXDm44nTv3F6jQEzYy4qRv/ohmwEQg\u003d\u003d"}"""
        private const val annual03Cancelled = """{"zza":"{\"orderId\":\"GPA.3372-3222-8630-38485\",\"packageName\":\"org.tasks\",\"productId\":\"annual_03\",\"purchaseTime\":1603917413542,\"purchaseState\":0,\"purchaseToken\":\"ciogggalpbohflhmjamciehl.AO-J1OyjBpBOnwKCOBSN0Cil7yM65ZYnkn9nGqZO1n3nsHIF_LHv0ZuQqNnThB_JuCt-s9wBij1PyOCoP8axqVILXSiavcyPmg\",\"autoRenewing\":false}","mParsedJson":{"nameValuePairs":{"orderId":"GPA.3372-3222-8630-38485","packageName":"org.tasks","productId":"annual_03","purchaseTime":1603917413542,"purchaseState":0,"purchaseToken":"ciogggalpbohflhmjamciehl.AO-J1OyjBpBOnwKCOBSN0Cil7yM65ZYnkn9nGqZO1n3nsHIF_LHv0ZuQqNnThB_JuCt-s9wBij1PyOCoP8axqVILXSiavcyPmg","autoRenewing":false}},"zzb":"jL+2qRv0LtCutoJ86NWaInbx/9/kIWbxXRKYkou74TBjwu9KZ89EpJY632ImEy2xfLd8DHuVuWOcZY646I29Ny2E4HYNAsQEg2du4NRXEHZvu+py4Mi212KF8S2EPNdZCor1wiOJ0zRVBiRAtiCfqxHjQdfKn7FpDiHFrUhMu1huEAxJ0Xrnvxcmkouizw3wzKnAvI+O75LIWWZHCy+1o7s285cSKtQoztVY/nHInJLxV6dk93lAivOlEox+VCLU978lUvv45Rue50fMzS2CRsVFmRt9/yTP8RCiQKzGC/pyHtqNj/ceCrDi4VV8JPhsPd4NUaKk82Oq1xmGXtzEcQ\u003d\u003d"}"""
    }
}