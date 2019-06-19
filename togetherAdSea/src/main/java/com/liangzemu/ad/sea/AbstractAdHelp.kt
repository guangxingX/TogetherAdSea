package com.liangzemu.ad.sea

import android.os.CountDownTimer
import androidx.annotation.NonNull
import com.liangzemu.ad.sea.TogetherAdSea.context
import com.liangzemu.ad.sea.TogetherAdSea.timeoutMillsecond
import com.liangzemu.ad.sea.other.*
import java.lang.ref.WeakReference

/**
 * 广告开始请求时监听器与广告实体(or loader)对应存储
 * 广告正在请求时替换最新一个的监听器
 *
 * 是否每次只能请求一个
 * @property adConstStr String
 * @constructor
 */
abstract class AbstractAdHelp(val adConstStr: String):AdBase,IAdListener {
    val levelHorizontal:Int = Math.max(
        TogetherAdSea.idListGoogleMap[adConstStr]?.size ?: 0,
        TogetherAdSea.idListFacebookMap[adConstStr]?.size ?: 0
    ) - 1
    
    fun requestAd(splashConfigStr: String?,
                  @NonNull userListener: IAdListener,
                  direction: Direction = Direction.HORIZONTAL,
                  onlyOnce:Boolean=false){

        if(onlyOnce){
            //正在加载中
            if(loadingAdType.contains(adConstStr)){
                unUseListenerList.add(0,userListener)
                loge("正在加载中")
                return
            }
        }
        //查看是否有缓存
        val adFromCache = getAdFromCache()
        adFromCache?.let {
            loge("已经有缓存了")
            unUseListenerList.add(0,userListener)
            bindListener(adFromCache.key)
            //回调加载完成
            listenerMap[adFromCache.key]?.get()?.onAdPrepared("adCache",adFromCache)
            return
        }
        loge("开始请求")
        unUseListenerList.add(userListener)
        loadingAdType.add(adConstStr)
        if (direction == Direction.HORIZONTAL) {
            requestAdHorizontal(splashConfigStr,this)
        } else {
            requestAdVertical(splashConfigStr, this)
        }
    }

    /**
     * 横向切换
     */
    private fun requestAdHorizontal(
        splashConfigStr: String?,
        iAdListener: IAdListener
    ) {

        //取最高等级

        var level = 0
        loge("total level:$levelHorizontal level:$level start")
        //循环等级请求
        fun requestAdRewardByLevel() {
            //这里虽然是竖向请求  但是因为有level的原因  每一个竖向只有一个  相当于就是横向了
            requestAdVertical(splashConfigStr, object : IAdListener {
                override fun onAdShow(channel: String,key:String) {
                    iAdListener.onAdShow(channel,key)
                }

                override fun onAdClose(channel: String, key:String,other:Any) {
                    iAdListener.onAdClose(channel, key,other)
                }

                override fun onAdPrepared(channel: String,adWrapper: AdWrapper) {
                    loge("AbstractAdHelp: level:$level success:$channel")
                    iAdListener.onAdPrepared(channel,adWrapper)
                }

                override fun onStartRequest(channel: String,key:String) {
                    iAdListener.onStartRequest(channel,key)
                }

                override fun onAdClick(channel: String,key:String) {
                    iAdListener.onAdClick(channel,key)
                }

                override fun onAdFailed(failedMsg: String?,key:String) {
                    loge("AbstractAdHelp: level:$level failed:$failedMsg")
                    if (level >= levelHorizontal) {
                        iAdListener.onAdFailed(failedMsg,key)
                    } else {
                        level++
                        requestAdRewardByLevel()
                    }

                }

            }, level)

        }
        //开始请求
        requestAdRewardByLevel()
    }

    /**
     * 无论横竖向都会走到这开始进行广告随机
     * @param configStr String?
     * @param adListener IAdListener
     * @param level Int 横向请求时区分第几级，竖向为-1
     * @return Unit
     */
    protected fun requestAdVertical(
        configStr: String?,
        @NonNull adListener:IAdListener,
        level: Int = -1
    ) {
        val randomAdName = AdRandomUtil.getRandomAdName(configStr)
        when (randomAdName) {
            AdNameType.NO -> {
                adListener.onAdFailed(TogetherAdSea.context.getString(R.string.all_ad_error),"ALL")
            }
            else->{
                dispatchAdRequest(randomAdName,level,configStr,0,adListener)
            }
        }
    }

    /**
     * 内部调用  添加到缓存
     * @param adWrapper AdWrapper
     * @return Unit
     */
    internal fun addtoAdCache(adWrapper: AdWrapper){
        val list = adCacheMap[adConstStr]
        if(list==null)
            adCacheMap[adConstStr]= mutableListOf(adWrapper)
        else
            list.add(adWrapper)
    }

    fun getAdFromCache():AdWrapper?{
        val list = adCacheMap[adConstStr]
        return  if(list.isNullOrEmpty())
                    null
                else
                    list[0]
    }

    /**
     * 从没有使用的监听器列表中取出一个  和传进来的key进行绑定
     * @param key String
     * @return Unit
     */
    internal fun bindListener(key:String){
        if(unUseListenerList.isNullOrEmpty())
            return
        val listener = unUseListenerList.removeAt(0)

        useListenerList.add(listener)
        listenerMap[key] = WeakReference(listener)
    }
    internal fun removeListener(key:String){
        listenerMap[key]?.get()?.let {
            useListenerList.remove(it)
        }
        listenerMap.remove(key)
    }
    /**
     * 外部广告使用后需要销毁时调用
     * @param adWrapper AdWrapper
     * @return Unit
     */
    fun destoryAd(adWrapper: AdWrapper){
        adCacheMap[adConstStr]?.remove(adWrapper)
        listenerMap[adWrapper.key]?.get()?.let {
            useListenerList.remove(it)
        }
        listenerMap.remove(adWrapper.key)
        adWrapper.destory()
    }
    fun removeAd(key: String,destroy:Boolean=false){

        listenerMap[key]?.get()?.let {
            useListenerList.remove(it)
        }
        adCacheMap[adConstStr]?.find {
            it.key == key
        }?.apply {
                if (destroy) {
                    destory()
                }
            adCacheMap[adConstStr]?.remove(this)
            }
        listenerMap.remove(key)

    }
    /**
     * 退出Activity时调用
     * @receiver AdWrapper
     * @return Unit
     */
    fun onDestory(){
        unUseListenerList.clear()
        useListenerList.clear()
    }

    internal abstract fun dispatchAdRequest(type:AdNameType,level: Int, configStr: String?, requestIndex: Int, adListener: IAdListener)
    companion object{
        /**
         * 已加载广告缓存
         */
        internal val adCacheMap=HashMap<String,MutableList<AdWrapper>>()
        /**
         * 正在加载的广告类型 主要用于某些时候每次只能加载一个的时候   防止重复加载
         */
        internal val loadingAdType=HashSet<String>()

        internal val listenerMap=HashMap<String,WeakReference<IAdListener>>() //HashMap<ADhash or loaderhash，监听器的弱引用>
        //TODO 改成队列或者链表
        /**
         * 没有使用过的监听器集合
         */
        internal val unUseListenerList=ArrayList<IAdListener>()
        /**
         * 使用过的监听器集合  是为了避免弱引用被清理
         */
        internal val useListenerList=ArrayList<IAdListener>()
        /**
         * 超时key
         */
        internal val timeOutSet=HashSet<String>()//HashSet<Key>
    }

    /**
     * 添加超时的key
     * @param key String
     * @return Unit
     */
    internal fun addTimeOut(key:String){
        timeOutSet.add(key)
    }
    internal fun isTimeOut(key:String):Boolean{
        return timeOutSet.contains(key)
    }
    /**
     *  ======================== 这堆监听器回调是为了方便子类扩展 =======================
     *  key 一般是 ADhash or loaderhash 区别在于能不能再加载前拿到ad
     *  这是仅次于userListener的回调
     */
    override fun onStartRequest(channel: String,key:String) {
        //这个是否有必要回调
        //listenerMap[key]?.onStartRequest(channel,key)
    }

    override fun onAdClick(channel: String,key:String) {
        listenerMap[key]?.get()?.onAdClick(channel,key)
    }

    override fun onAdFailed(failedMsg: String?,key:String) {
        loadingAdType.remove(adConstStr)
        //移出超时
        timeOutSet.remove(key)
        //绑定监听器
        bindListener(key)
        listenerMap[key]?.get()?.onAdFailed(failedMsg,key)
        removeListener(key)
    }

    override fun onAdShow(channel: String,key:String) {
        listenerMap[key]?.get()?.onAdShow(channel,key)
    }

    override fun onAdClose(channel: String,key:String,other:Any) {
        listenerMap[key]?.get()?.onAdClose(channel,key,other)
    }

    override fun onAdPrepared(channel: String,adWrapper: AdWrapper) {
        //移出加载中
        loadingAdType.remove(adConstStr)
        //移出超时
        timeOutSet.remove(adWrapper.key)
        //绑定监听器
        bindListener(adWrapper.key)
        //加入缓存
        addtoAdCache(adWrapper)
        //回调
        listenerMap[adWrapper.key]?.get()?.onAdPrepared(channel,adWrapper)
    }

    /**
     * 创建超时倒计时
     * @param callback ()->Unit
     * @return CountDownTimer
     */
    protected fun creatTimer(callback:()->Unit):CountDownTimer{
        return object : CountDownTimer(timeoutMillsecond, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                logd(" 倒计时: $millisUntilFinished")
            }

            override fun onFinish() {
                callback()
                logd("倒计时: ${context.getString(R.string.dismiss)}")
            }
        }
    }
}
