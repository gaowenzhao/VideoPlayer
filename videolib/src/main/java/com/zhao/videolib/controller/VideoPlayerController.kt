package com.zhao.videolib.controller

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.zhao.videolib.bean.PlayMode
import com.zhao.videolib.bean.PlayState
import com.zhao.videolib.bean.VideoBean
import com.zhao.videolib.inf.BasePlayerI
import com.zhao.videolib.utils.VideoUtils

import java.util.Timer
import java.util.TimerTask

/**
 * Created by XiaoJianjun on 2017/6/21.
 * 控制器抽象类
 */
abstract class VideoPlayerController(private val mContext: Context) : FrameLayout(mContext), View.OnTouchListener {
    protected lateinit var videoPlayer: BasePlayerI

    private var mUpdateProgressTimer: Timer? = null
    private var mUpdateProgressTimerTask: TimerTask? = null

    private var mDownX: Float = 0f
    private var mDownY: Float = 0f
    private var mNeedChangePosition: Boolean = false
    private var mNeedChangeVolume: Boolean = false
    private var mNeedChangeBrightness: Boolean = false
    private var mGestureDownPosition: Long = 0
    private var mGestureDownBrightness: Float = 0.toFloat()
    private var mGestureDownVolume: Int = 0
    private var mNewPosition: Long = 0

    init {
        this.setOnTouchListener(this)
    }

    open fun setNiceVideoPlayer(videoPlayer: BasePlayerI) {
        this.videoPlayer = videoPlayer
    }
    abstract fun setVideoInfo(info:VideoBean)


    abstract fun onPlayStateChanged(playState: PlayState)


    abstract fun onPlayModeChanged(playMode: PlayMode)

    /**
     * 重置控制器，将控制器恢复到初始状态。
     */
    abstract fun reset()

    /**
     * 开启更新进度的计时器。
     */
    fun startUpdateProgressTimer() {
        cancelUpdateProgressTimer()
        if (mUpdateProgressTimer == null) {
            mUpdateProgressTimer = Timer()
        }
        if (mUpdateProgressTimerTask == null) {
            mUpdateProgressTimerTask = object : TimerTask() {
                override fun run() {
                    this@VideoPlayerController.post { updateProgress() }
                }
            }
        }
        mUpdateProgressTimer!!.schedule(mUpdateProgressTimerTask, 0, 1000)
    }

    /**
     * 取消更新进度的计时器。
     */
    fun cancelUpdateProgressTimer() {
        if (mUpdateProgressTimer != null) {
            mUpdateProgressTimer!!.cancel()
            mUpdateProgressTimer = null
        }
        if (mUpdateProgressTimerTask != null) {
            mUpdateProgressTimerTask!!.cancel()
            mUpdateProgressTimerTask = null
        }
    }

    /**
     * 更新进度，包括进度条进度，展示的当前播放位置时长，总时长等。
     */
    abstract fun updateProgress()

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        // 只有全屏的时候才能拖动位置、亮度、声音
        if (!videoPlayer.isFullScreen()) {
            return false
        }
        // 只有在播放、暂停、缓冲的时候能够拖动改变位置、亮度和声音
        if (videoPlayer.isIdle()
            || videoPlayer.isError()
            || videoPlayer.isPreparing()
            || videoPlayer.isPrepared()
            || videoPlayer.isCompleted()
        ) {
            hideChangePosition()
            hideChangeBrightness()
            hideChangeVolume()
            return false
        }
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mDownX = x
                mDownY = y
                mNeedChangePosition = false
                mNeedChangeVolume = false
                mNeedChangeBrightness = false
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - mDownX
                var deltaY = y - mDownY
                val absDeltaX = Math.abs(deltaX)
                val absDeltaY = Math.abs(deltaY)
                if (!mNeedChangePosition && !mNeedChangeVolume && !mNeedChangeBrightness) {
                    // 只有在播放、暂停、缓冲的时候能够拖动改变位置、亮度和声音
                    if (absDeltaX >= THRESHOLD) {
                        cancelUpdateProgressTimer()
                        mNeedChangePosition = true
                        mGestureDownPosition = videoPlayer.getCurrentPosition()
                    } else if (absDeltaY >= THRESHOLD) {
                        if (mDownX < width * 0.5f) {
                            // 左侧改变亮度
                            mNeedChangeBrightness = true
                            mGestureDownBrightness = VideoUtils.scanForActivity(mContext)
                                ?.window?.attributes?.screenBrightness!!
                        } else {
                            // 右侧改变声音
                            mNeedChangeVolume = true
                            mGestureDownVolume = videoPlayer.getVolume()
                        }
                    }
                }
                if (mNeedChangePosition) {
                    val duration = videoPlayer.getDuration()
                    val toPosition = (mGestureDownPosition + duration * deltaX / width).toLong()
                    mNewPosition = Math.max(0, Math.min(duration, toPosition))
                    val newPositionProgress = (100f * mNewPosition / duration).toInt()
                    showChangePosition(duration, newPositionProgress)
                }
                if (mNeedChangeBrightness) {
                    deltaY = -deltaY
                    val deltaBrightness = deltaY * 3 / height
                    var newBrightness = mGestureDownBrightness + deltaBrightness
                    newBrightness = Math.max(0f, Math.min(newBrightness, 1f))
                    val newBrightnessPercentage = newBrightness
                    val params = VideoUtils.scanForActivity(mContext)
                        ?.window?.attributes
                    params?.screenBrightness = newBrightnessPercentage
                    VideoUtils.scanForActivity(mContext)?.window?.attributes = params
                    val newBrightnessProgress = (100f * newBrightnessPercentage).toInt()
                    showChangeBrightness(newBrightnessProgress)
                }
                if (mNeedChangeVolume) {
                    deltaY = -deltaY
                    val maxVolume = videoPlayer.getMaxVolume()
                    val deltaVolume = (maxVolume.toFloat() * deltaY * 3f / height).toInt()
                    var newVolume = mGestureDownVolume + deltaVolume
                    newVolume = Math.max(0, Math.min(maxVolume, newVolume))
                    videoPlayer.setVolume(newVolume)
                    val newVolumeProgress = (100f * newVolume / maxVolume).toInt()
                    showChangeVolume(newVolumeProgress)
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (mNeedChangePosition) {
                    videoPlayer.seekTo(mNewPosition)
                    hideChangePosition()
                    startUpdateProgressTimer()
                    return true
                }
                if (mNeedChangeBrightness) {
                    hideChangeBrightness()
                    return true
                }
                if (mNeedChangeVolume) {
                    hideChangeVolume()
                    return true
                }
            }
        }
        return false
    }

    /**
     * 手势左右滑动改变播放位置时，显示控制器中间的播放位置变化视图，
     * 在手势滑动ACTION_MOVE的过程中，会不断调用此方法。
     *
     * @param duration            视频总时长ms
     * @param newPositionProgress 新的位置进度，取值0到100。
     */
    abstract fun showChangePosition(duration: Long, newPositionProgress: Int)

    /**
     * 手势左右滑动改变播放位置后，手势up或者cancel时，隐藏控制器中间的播放位置变化视图，
     * 在手势ACTION_UP或ACTION_CANCEL时调用。
     */
    abstract fun hideChangePosition()

    /**
     * 手势在右侧上下滑动改变音量时，显示控制器中间的音量变化视图，
     * 在手势滑动ACTION_MOVE的过程中，会不断调用此方法。
     *
     * @param newVolumeProgress 新的音量进度，取值1到100。
     */
    abstract fun showChangeVolume(newVolumeProgress: Int)

    /**
     * 手势在左侧上下滑动改变音量后，手势up或者cancel时，隐藏控制器中间的音量变化视图，
     * 在手势ACTION_UP或ACTION_CANCEL时调用。
     */
    abstract fun hideChangeVolume()

    /**
     * 手势在左侧上下滑动改变亮度时，显示控制器中间的亮度变化视图，
     * 在手势滑动ACTION_MOVE的过程中，会不断调用此方法。
     *
     * @param newBrightnessProgress 新的亮度进度，取值1到100。
     */
    abstract fun showChangeBrightness(newBrightnessProgress: Int)

    /**
     * 手势在左侧上下滑动改变亮度后，手势up或者cancel时，隐藏控制器中间的亮度变化视图，
     * 在手势ACTION_UP或ACTION_CANCEL时调用。
     */
    abstract fun hideChangeBrightness()

    companion object {
        private val THRESHOLD = 80
    }
}
