package com.example.orbblaze.ui.game

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

class AdsManager(private val context: Context) {

    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    // ✅ ID REAL obtenido de tu consola AdMob
    private val adUnitId = "ca-app-pub-3395921255182308/8441248018" 

    init {
        MobileAds.initialize(context) { loadRewardedInterstitialAd() }
    }

    private fun loadRewardedInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedInterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedInterstitialAd = null
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: (Int) -> Unit) {
        rewardedInterstitialAd?.let { ad ->
            ad.show(activity) { rewardItem ->
                onRewardEarned(rewardItem.amount)
                loadRewardedInterstitialAd() // Cargar el siguiente
            }
        } ?: run {
            // Si no hay anuncio cargado, intentamos cargar uno para la próxima
            loadRewardedInterstitialAd()
        }
    }
}
