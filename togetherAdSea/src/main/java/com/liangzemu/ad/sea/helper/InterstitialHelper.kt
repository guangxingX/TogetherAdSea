package com.liangzemu.ad.sea.helper

import android.os.CountDownTimer
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.InterstitialAdListener
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.liangzemu.ad.sea.*
import com.liangzemu.ad.sea.TogetherAdSea.context
import com.liangzemu.ad.sea.other.AdNameType
import com.liangzemu.ad.sea.other.logi

/* 
 * (●ﾟωﾟ●) 插页广告
 * 
 * Created by Matthew_Chen on 2019-06-25.
 */
class InterstitialHelper(
    adConstStr: String,
    timeOutMillsecond: Long = TogetherAdSea.timeoutMillsecond,
    owner: String = adConstStr
) : BaseAdHelp(adConstStr, timeOutMillsecond, owner) {

    @Throws(IllegalArgumentException::class)
    override fun initAD(id: String, adNameType: AdNameType): Pair<Any, String> {
        return when (adNameType) {
            AdNameType.GOOGLE_ADMOB -> {
                val interGoogle = InterstitialAd(context)
                Pair(interGoogle, interGoogle.hashCode().toString())

            }
            AdNameType.FACEBOOK -> {
                val interFacebook = com.facebook.ads.InterstitialAd(context, id)
                Pair(interFacebook, interFacebook.hashCode().toString())
            }
            else -> {
                throw IllegalArgumentException("没有此广告类型:${adNameType.type}")
            }
        }
    }

    override fun setGoogleAdListenerAndStart(
        id: String,
        adOrBuilder: Any,
        adListener: IAdListener,
        timer: CountDownTimer,
        errorCallback: (String?) -> Unit
    ) {
        adOrBuilder as InterstitialAd
        adOrBuilder.adUnitId = id
        adOrBuilder.loadAd(AdRequest.Builder().apply {
            if (TogetherAdSea.testDeviceID != null) addTestDevice(
                TogetherAdSea.testDeviceID
            )
        }.build())
        adOrBuilder.adListener = object : AdListener() {
            override fun onAdLoaded() {
                if (adOrBuilder.isLoaded) {
                    timer.cancel()
                    logi("${AdNameType.GOOGLE_ADMOB.type}:$adConstStr ${context.getString(R.string.prepared)}")
                    adListener.onAdPrepared(AdNameType.GOOGLE_ADMOB.type, AdWrapper(adOrBuilder))
                } else {
                    errorCallback("Google InterstitialAd is not loaded")
                }
            }

            override fun onAdClosed() {
                logi("${AdNameType.GOOGLE_ADMOB.type}:$adConstStr ${context.getString(R.string.dismiss)}")
                adListener.onAdClose(AdNameType.GOOGLE_ADMOB.type, adOrBuilder.hashCode().toString(), adOrBuilder)
            }

            override fun onAdFailedToLoad(errorCode: Int) {
                errorCallback("errorCode: $errorCode")
            }

            override fun onAdClicked() {
                logi("${AdNameType.GOOGLE_ADMOB.type}:$adConstStr ${context.getString(R.string.clicked)}")
                adListener.onAdClick(AdNameType.GOOGLE_ADMOB.type, adOrBuilder.hashCode().toString())
            }

            override fun onAdImpression() {
                logi("${AdNameType.GOOGLE_ADMOB.type}:$adConstStr ${context.getString(R.string.exposure)}")
            }

            override fun onAdOpened() {
                logi("${AdNameType.GOOGLE_ADMOB.type}:$adConstStr ${context.getString(R.string.opened)}")
                adListener.onAdShow(AdNameType.GOOGLE_ADMOB.type, adOrBuilder.hashCode().toString())
            }
        }
    }

    override fun setFaceBookAdListenerAndStart(
        adOrBuilder: Any,
        adListener: IAdListener,
        timer: CountDownTimer,
        errorCallback: (String?) -> Unit
    ) {
        adOrBuilder as com.facebook.ads.InterstitialAd
        adOrBuilder.setAdListener(object : InterstitialAdListener {
            override fun onInterstitialDisplayed(p0: Ad?) {
            }

            override fun onAdClicked(p0: Ad) {
                logi("${AdNameType.FACEBOOK.type}:$adConstStr ${context.getString(R.string.clicked)}")
                adListener.onAdClick(AdNameType.FACEBOOK.type, p0.hashCode().toString())
            }

            override fun onInterstitialDismissed(p0: Ad) {
                logi("${AdNameType.FACEBOOK.type}:$adConstStr ${context.getString(R.string.dismiss)}")
                adListener.onAdClose(AdNameType.FACEBOOK.type, p0.hashCode().toString(), p0)
            }

            override fun onError(p0: Ad?, adError: AdError?) {
                errorCallback("${adError?.errorCode}:${adError?.errorMessage}")
            }

            override fun onAdLoaded(p0: Ad) {
                logi("${AdNameType.FACEBOOK.type}:$adConstStr ${context.getString(R.string.prepared)}")
                timer.cancel()
                adListener.onAdPrepared(AdNameType.FACEBOOK.type, AdWrapper(p0))
            }

            override fun onLoggingImpression(p0: Ad?) {
                logi("${AdNameType.FACEBOOK.type}:$adConstStr ${context.getString(R.string.show)}")
                adListener.onAdShow(AdNameType.FACEBOOK.type, p0.hashCode().toString())
            }
        })
        adOrBuilder.loadAd()
    }

}