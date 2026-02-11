package com.example.orbblaze.ui.game

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

class AdsManager(private val context: Context) {

    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var interstitialAd: InterstitialAd? = null

    private val rewardedId = "ca-app-pub-3395921255182308/8441248018"
    private val bannerId = "ca-app-pub-3395921255182308/2751460140"
    private val interstitialId = "ca-app-pub-3395921255182308/1188517915"

    init {
        MobileAds.initialize(context) {
            loadRewardedInterstitialAd()
            loadInterstitialAd()
        }
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: (Int) -> Unit) {
        rewardedInterstitialAd?.let { ad ->
            ad.show(activity) { rewardItem ->
                onRewardEarned(rewardItem.amount)
                loadRewardedInterstitialAd()
            }
        } ?: loadRewardedInterstitialAd()
    }

    private fun loadRewardedInterstitialAd() {
        RewardedInterstitialAd.load(
            context, rewardedId, AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) { rewardedInterstitialAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { rewardedInterstitialAd = null }
            }
        )
    }

    private fun loadInterstitialAd() {
        InterstitialAd.load(
            context, interstitialId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null }
            }
        )
    }

    fun showInterstitialAd(activity: Activity) {
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                }
            }
            ad.show(activity)
        } ?: loadInterstitialAd()
    }

    @Composable
    fun BannerAd(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp), 
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        adUnitId = bannerId
                        val displayMetrics: DisplayMetrics = ctx.resources.displayMetrics
                        val density = displayMetrics.density
                        
                        // ✅ Ancho al 100% para llegar a los bordes de cualquier dispositivo
                        val adWidthPixels = displayMetrics.widthPixels.toFloat()
                        val adWidth = (adWidthPixels / density).toInt()
                        
                        setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth))
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}
