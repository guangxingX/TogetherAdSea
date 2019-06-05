package com.liangzemu.ad.sea.helper

import android.content.Context
import androidx.annotation.NonNull
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.liangzemu.ad.sea.AdBase
import com.liangzemu.ad.sea.R
import com.liangzemu.ad.sea.TogetherAdSea
import com.liangzemu.ad.sea.other.AdNameType
import com.liangzemu.ad.sea.other.AdRandomUtil
import com.liangzemu.ad.sea.other.logd
import com.liangzemu.ad.sea.other.loge


/*
 * (●ﾟωﾟ●) 激励视频广告
 * 
 * Created by Matthew_Chen on 2019-06-05.
 */
object TogetherAdSeaReward : AdBase {

    private var mRewardedVideoAdGoogle: RewardedVideoAd? = null
    private var mRewardedVideoAdFacebook: com.facebook.ads.RewardedVideoAd? = null

    fun requestAdReward(
        @NonNull context: Context,
        interConfigStr: String?,
        @NonNull adConstStr: String,
        @NonNull adListener: AdListenerReward
    ) {

        val randomAdName = AdRandomUtil.getRandomAdName(interConfigStr)
        when (randomAdName) {
            AdNameType.GOOGLE_ADMOB -> requestAdRewardGoogle(
                context.applicationContext,
                interConfigStr,
                adConstStr,
                0,
                adListener
            )
            AdNameType.FACEBOOK -> requestAdRewardFacebook(
                context.applicationContext,
                interConfigStr,
                adConstStr,
                0,
                adListener
            )
            else -> {
                adListener.onAdFailed(context.getString(com.liangzemu.ad.sea.R.string.all_ad_error))
                loge(context.getString(com.liangzemu.ad.sea.R.string.all_ad_error))
            }
        }
    }

    private fun requestAdRewardGoogle(
        @NonNull context: Context,
        rewardConfigStr: String?,
        @NonNull adConstStr: String,
        indexGoogle: Int,
        @NonNull adListener: AdListenerReward
    ) {
        val idList = TogetherAdSea.idListGoogleMap[adConstStr]

        if (indexGoogle >= idList?.size ?: 0) {
            //如果所有档位都请求失败了，就切换另外一种广告
            val newRewardConfig = rewardConfigStr?.replace(AdNameType.GOOGLE_ADMOB.type, AdNameType.NO.type)
            requestAdReward(context, newRewardConfig, adConstStr, adListener)
            return
        }

        if (idList.isNullOrEmpty()) {
            //如果在 Map 里面获取不到该广告位的 idList 意味着初始化的时候没有设置这个广告位
            loge("${AdNameType.GOOGLE_ADMOB.type}: ${context.getString(com.liangzemu.ad.sea.R.string.ad_id_no)}")
            adListener.onAdFailed(context.getString(com.liangzemu.ad.sea.R.string.ad_id_no))
            return
        }

        logd("${AdNameType.GOOGLE_ADMOB.type}: ${context.getString(com.liangzemu.ad.sea.R.string.start_request)}")
        adListener.onStartRequest(AdNameType.GOOGLE_ADMOB.type)

        mRewardedVideoAdGoogle = MobileAds.getRewardedVideoAdInstance(context)
        mRewardedVideoAdGoogle?.rewardedVideoAdListener = object : RewardedVideoAdListener {
            override fun onRewarded(reward: RewardItem) {
            }

            override fun onRewardedVideoAdLeftApplication() {
                logd("${AdNameType.GOOGLE_ADMOB.type}: ${context.getString(R.string.clicked)}")
                adListener.onAdClick(AdNameType.GOOGLE_ADMOB.type)
            }

            override fun onRewardedVideoAdClosed() {
            }

            override fun onRewardedVideoAdFailedToLoad(errorCode: Int) {
                loge("${AdNameType.GOOGLE_ADMOB.type}: indexGoogle:$indexGoogle, errorCode:$errorCode")
                val newIndexGoogle = indexGoogle + 1
                requestAdRewardGoogle(context, rewardConfigStr, adConstStr, newIndexGoogle, adListener)
            }

            override fun onRewardedVideoAdLoaded() {
                logd("${AdNameType.GOOGLE_ADMOB.type}: ${context.getString(R.string.prepared)}")
                adListener.onAdPrepared(AdNameType.GOOGLE_ADMOB.type)
            }

            override fun onRewardedVideoAdOpened() {
                logd("${AdNameType.GOOGLE_ADMOB.type}: ${context.getString(R.string.show)}")
                adListener.onAdShow(AdNameType.GOOGLE_ADMOB.type)
            }

            override fun onRewardedVideoStarted() {
            }

            override fun onRewardedVideoCompleted() {
                logd("${AdNameType.GOOGLE_ADMOB.type}: ${context.getString(R.string.complete)}")
                adListener.onAdComplete(AdNameType.GOOGLE_ADMOB.type)
            }
        }

        mRewardedVideoAdGoogle?.loadAd(idList[indexGoogle], AdRequest.Builder().build())
    }

    private fun requestAdRewardFacebook(
        @NonNull context: Context,
        rewardConfigStr: String?,
        @NonNull adConstStr: String,
        indexFacebook: Int,
        @NonNull adListener: AdListenerReward
    ) {

        val idList = TogetherAdSea.idListFacebookMap[adConstStr]

        if (indexFacebook >= idList?.size ?: 0) {
            //如果所有档位都请求失败了，就切换另外一种广告
            val newRewardConfig = rewardConfigStr?.replace(AdNameType.FACEBOOK.type, AdNameType.NO.type)
            requestAdReward(context, newRewardConfig, adConstStr, adListener)
            return
        }

        if (idList.isNullOrEmpty()) {
            //如果在 Map 里面获取不到该广告位的 idList 意味着初始化的时候没有设置这个广告位
            loge("${AdNameType.FACEBOOK.type}: ${context.getString(com.liangzemu.ad.sea.R.string.ad_id_no)}")
            adListener.onAdFailed(context.getString(com.liangzemu.ad.sea.R.string.ad_id_no))
            return
        }

        logd("${AdNameType.FACEBOOK.type}: ${context.getString(com.liangzemu.ad.sea.R.string.start_request)}")
        adListener.onStartRequest(AdNameType.FACEBOOK.type)

        mRewardedVideoAdFacebook = com.facebook.ads.RewardedVideoAd(context, idList[indexFacebook])
        mRewardedVideoAdFacebook?.setAdListener(object : com.facebook.ads.RewardedVideoAdListener {
            override fun onRewardedVideoClosed() {
            }

            override fun onAdClicked(p0: Ad?) {
                logd("${AdNameType.FACEBOOK.type}: ${context.getString(R.string.clicked)}")
                adListener.onAdClick(AdNameType.FACEBOOK.type)
            }

            override fun onRewardedVideoCompleted() {
                logd("${AdNameType.FACEBOOK.type}: ${context.getString(R.string.complete)}")
                adListener.onAdComplete(AdNameType.FACEBOOK.type)
            }

            override fun onError(p0: Ad?, adError: AdError?) {
                loge("${AdNameType.FACEBOOK.type}: indexFacebook:$indexFacebook, errorCode:${adError?.errorCode} ${adError?.errorMessage}")
                val newIndexFacebook = indexFacebook + 1
                requestAdRewardFacebook(context, rewardConfigStr, adConstStr, newIndexFacebook, adListener)
            }

            override fun onAdLoaded(p0: Ad?) {
                logd("${AdNameType.FACEBOOK.type}: ${context.getString(R.string.prepared)}")
                adListener.onAdPrepared(AdNameType.FACEBOOK.type)
            }

            override fun onLoggingImpression(p0: Ad?) {
                logd("${AdNameType.FACEBOOK.type}: ${context.getString(R.string.show)}")
                adListener.onAdShow(AdNameType.FACEBOOK.type)
            }
        })
        mRewardedVideoAdFacebook?.loadAd()

    }

    fun isLoaded(): Boolean {
        return mRewardedVideoAdGoogle?.isLoaded == true || mRewardedVideoAdFacebook?.isAdLoaded == true
    }

    fun showAdReward() {
        if (mRewardedVideoAdGoogle?.isLoaded == true) {
            mRewardedVideoAdGoogle?.show()
        } else {
            if (mRewardedVideoAdFacebook?.isAdLoaded == true) {
                mRewardedVideoAdFacebook?.show()
            }
        }
    }

    /**
     * 监听器
     */
    interface AdListenerReward {

        //开始请求
        fun onStartRequest(channel: String)

        //点击了
        fun onAdClick(channel: String)

        //失败了
        fun onAdFailed(failedMsg: String?)

        //展示了
        fun onAdShow(channel: String)

        //播放完成了
        fun onAdComplete(channel: String)

        //准备好了
        fun onAdPrepared(channel: String)
    }
}