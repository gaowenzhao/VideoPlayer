package com.zhao.videolib

import android.content.Context
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.media.AudioManager
import android.net.Uri
import android.util.AttributeSet
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import com.zhao.videolib.bean.PlayMode
import com.zhao.videolib.bean.PlayState
import com.zhao.videolib.bean.PlayerType
import com.zhao.videolib.controller.VideoPlayerController
import com.zhao.videolib.inf.BasePlayerI
import com.zhao.videolib.utils.LogUtil
import com.zhao.videolib.utils.VideoUtils
import com.zhao.videolib.utils.VideoPlayerManager
import tv.danmaku.ijk.media.player.AndroidMediaPlayer
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.IOException

/**
 * 播放器的基本信息
 */
 abstract class BaseMediaPlayer : FrameLayout, BasePlayerI, TextureView.SurfaceTextureListener {
    var mController:VideoPlayerController? = null
    var mBufferPercentage: Int = 0
    var skipToPosition: Long= 0
    var mCurrentMode = PlayMode.NORMAL
    var mPlayerType = PlayerType.IJK
    private var mediaPlayer:IMediaPlayer? = null
    var mCurrentState = PlayState.IDLE
    private var mAudioManager:AudioManager? = null
    val mTextureView by lazy { NiceTextureView(context)}
    private var mSurfaceTexture: SurfaceTexture? = null
    private var mSurface: Surface? = null
    lateinit var mUrl: String
    var continueFromLastPosition = true

    lateinit var mContainer: FrameLayout
    private var mHeaders: Map<String, String>? = null
    constructor(context: Context) : super(context){ initView() }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs){ initView() }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr){ initView() }

    override fun setUp(url: String, headers: Map<String, String>?) {
        mUrl = url
        mHeaders = headers
    }
    fun setController(controller: VideoPlayerController) {
        mContainer.removeView(mController)
        mController = controller
        mController?.reset()
        mController?.setNiceVideoPlayer(this)
        val params = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        mContainer.addView(mController, params)
    }
    override fun start() {
        if (mCurrentState == PlayState.IDLE) {
            VideoPlayerManager.get().setVideoPlayer(this)
            if(mAudioManager==null){
                mAudioManager =  context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                mAudioManager?.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            }
            if(mediaPlayer == null){
                mediaPlayer = if(mPlayerType==PlayerType.NATIVE) AndroidMediaPlayer() else IjkMediaPlayer()
            }
            mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mTextureView.surfaceTextureListener = this
            addTextureView()
        } else {
            LogUtil.d("NiceVideoPlayer只有在mCurrentState == STATE_IDLE时才能调用start方法.")
        }
    }

    override fun start(position: Long) {
        skipToPosition = position
        start()
    }

    override fun restart() {
        if (mCurrentState == PlayState.PAUSED) {
            mediaPlayer?.start()
            mCurrentState = PlayState.PLAYING
            mController?.onPlayStateChanged(mCurrentState)
            LogUtil.d("STATE_PLAYING")
        } else if (mCurrentState == PlayState.BUFFERING_PAUSED) {
            mediaPlayer?.start()
            mCurrentState = PlayState.BUFFERING_PLAYING
            mController?.onPlayStateChanged(mCurrentState)
            LogUtil.d("STATE_BUFFERING_PLAYING")
        } else if (mCurrentState == PlayState.COMPLETED || mCurrentState == PlayState.ERROR) {
            mediaPlayer?.reset()
            openMediaPlayer()
        } else {
            LogUtil.d("NiceVideoPlayer在mCurrentState == " + mCurrentState + "时不能调用restart()方法.")
        }
    }

    override fun pause() {
        if (mCurrentState == PlayState.PLAYING) {
            mediaPlayer?.pause()
            mCurrentState = PlayState.PAUSED
            mController?.onPlayStateChanged(mCurrentState)
            LogUtil.d("STATE_PAUSED")
        }
        if (mCurrentState == PlayState.BUFFERING_PLAYING) {
            mediaPlayer?.pause()
            mCurrentState = PlayState.BUFFERING_PAUSED
            mController?.onPlayStateChanged(mCurrentState)
            LogUtil.d("STATE_BUFFERING_PAUSED")
        }
    }

    override fun seekTo(pos: Long) {
        mediaPlayer?.seekTo(pos)
    }

    override fun setVolume(volume: Int) {
        mAudioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
    }

    override fun setSpeed(speed: Float) {
        if (mediaPlayer is IjkMediaPlayer) {
            (mediaPlayer as IjkMediaPlayer).setSpeed(speed)
        } else {
            LogUtil.d("只有IjkPlayer才能设置播放速度")
        }
    }
    override fun getMaxVolume(): Int {
        return if(mAudioManager==null) 0 else mAudioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }
    override fun getVolume(): Int {
        return if(mAudioManager==null) 0 else mAudioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
    }
    override fun getDuration(): Long {
         return if(mediaPlayer==null) 0 else mediaPlayer!!.duration
    }

    override fun getCurrentPosition(): Long {
        return if(mediaPlayer==null) 0 else mediaPlayer!!.currentPosition
    }

    override fun getBufferPercentage(): Int {
        return mBufferPercentage
    }
    override fun getSpeed(speed: Float): Float {
        return if (mediaPlayer is IjkMediaPlayer) {
            (mediaPlayer as IjkMediaPlayer).getSpeed(speed)
        } else 0f
    }

    override fun getTcpSpeed(): Long {
        return if (mediaPlayer is IjkMediaPlayer) {
            (mediaPlayer as IjkMediaPlayer).tcpSpeed
        } else 0
    }
    override fun releasePlayer() {
        mAudioManager?.abandonAudioFocus(null)
        mAudioManager = null
        mediaPlayer?.release()
        mediaPlayer = null
        mContainer.removeView(mTextureView)
        mSurface?.release()
        mSurface = null
        mSurfaceTexture?.release()
        mSurfaceTexture = null
        mCurrentState = PlayState.IDLE
    }

    override fun release() {
        // 保存播放位置
        if (isPlaying() || isBufferingPlaying() || isBufferingPaused() || isPaused()) {
            VideoUtils.savePlayPosition(context, mUrl, getCurrentPosition())
        } else if (isCompleted()) {
            VideoUtils.savePlayPosition(context, mUrl, 0)
        }
        // 退出全屏或小窗口
        if (isFullScreen()) {
            exitFullScreen()
        }
        if (isTinyWindow()) {
            exitTinyWindow()
        }
        mCurrentMode = PlayMode.NORMAL

        // 释放播放器
        releasePlayer()

      // 恢复控制器
       mController?.reset()
       Runtime.getRuntime().gc()
    }
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {return mSurfaceTexture == null}
    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
        if (mSurfaceTexture == null) {
            mSurfaceTexture = surfaceTexture
            openMediaPlayer()
        } else {
            mTextureView.surfaceTexture = mSurfaceTexture
        }
    }
    private fun initView() {
        mContainer = FrameLayout(context)
        mContainer.setBackgroundColor(Color.BLACK)
        val params = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        this.addView(mContainer, params)
    }
    abstract fun addPlayListener(mediaPlayer: IMediaPlayer)
    private fun openMediaPlayer() {
       // 屏幕常亮
        mContainer.keepScreenOn = true
        // 设置监听
        mediaPlayer?.let {
            addPlayListener(it)
            try {
                it.setDataSource(context, Uri.parse(mUrl), mHeaders)
                if (mSurface == null) {
                    mSurface = Surface(mSurfaceTexture)
                }
                it.setSurface(mSurface)
                it.prepareAsync()
                mCurrentState = PlayState.PREPARING
               mController?.onPlayStateChanged(mCurrentState)
                LogUtil.d("STATE_PREPARING")
            } catch (e: IOException) {
                e.printStackTrace()
                LogUtil.e("打开播放器发生错误", e)
            }
        }
    }
    private fun addTextureView(){
        mContainer.removeView(mTextureView)
        val params = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
        mContainer.addView(mTextureView, 0, params)
    }
}