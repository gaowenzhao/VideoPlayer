package com.zhao.videolib

import android.content.Context
import android.util.AttributeSet
import com.zhao.videolib.bean.PlayState
import com.zhao.videolib.utils.LogUtil
import com.zhao.videolib.utils.VideoUtils
import tv.danmaku.ijk.media.player.IMediaPlayer

/**
 * 播放状态的监听
 */
class BasePlayListenerPlayer : BaseStatePlayer{
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun addPlayListener(mediaPlayer: IMediaPlayer) {
        mediaPlayer.setOnVideoSizeChangedListener { p0, p1, p2, p3, p4 ->
            mTextureView.adaptVideoSize(width, height)
            LogUtil.d("onVideoSizeChanged ——> width：$width， height：$height")
        }
        mediaPlayer.setOnPreparedListener { p0 ->
            mCurrentState = PlayState.PREPARED
            mController?.onPlayStateChanged(mCurrentState)
            LogUtil.d("onPrepared ——> STATE_PREPARED")
            p0?.start()
            // 从上次的保存位置播放
            if (continueFromLastPosition) {
                val savedPlayPosition = VideoUtils.getSavedPlayPosition(context, mUrl)
                p0?.seekTo(savedPlayPosition)
            }
            // 跳到指定位置播放
            if (skipToPosition != 0L) {
                p0?.seekTo(skipToPosition)
            }
        }
        mediaPlayer.setOnCompletionListener {
            mCurrentState = PlayState.COMPLETED
            mController?.onPlayStateChanged(mCurrentState)
            LogUtil.d("onCompletion ——> STATE_COMPLETED")
            // 清除屏幕常亮
            mContainer.keepScreenOn = false
        }
        mediaPlayer.setOnErrorListener { p0, p1, p2 ->
            // 直播流播放时去调用mediaPlayer.getDuration会导致-38和-2147483648错误，忽略该错误
            if (p1 != -38 && p1 != -2147483648 && p2 != -38 && p2 != -2147483648) {
                mCurrentState = PlayState.ERROR
                mController?.onPlayStateChanged(mCurrentState)
                LogUtil.d("onError ——> STATE_ERROR ———— what：$p1, extra: $p2")
            }
            true
        }
        mediaPlayer.setOnInfoListener { p0, what, extra ->
            if (what == IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                // 播放器开始渲染
                mCurrentState = PlayState.PLAYING
                mController?.onPlayStateChanged(mCurrentState)
                LogUtil.d("onInfo ——> MEDIA_INFO_VIDEO_RENDERING_START：STATE_PLAYING")
            } else if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_START) {
                // MediaPlayer暂时不播放，以缓冲更多的数据
                if (mCurrentState == PlayState.PAUSED || mCurrentState == PlayState.BUFFERING_PAUSED) {
                    mCurrentState = PlayState.BUFFERING_PAUSED
                    LogUtil.d("onInfo ——> MEDIA_INFO_BUFFERING_START：STATE_BUFFERING_PAUSED")
                } else {
                    mCurrentState = PlayState.BUFFERING_PLAYING
                    LogUtil.d("onInfo ——> MEDIA_INFO_BUFFERING_START：STATE_BUFFERING_PLAYING")
                }
                mController?.onPlayStateChanged(mCurrentState)
            } else if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
                // 填充缓冲区后，MediaPlayer恢复播放/暂停
                if (mCurrentState == PlayState.BUFFERING_PLAYING) {
                    mCurrentState = PlayState.PLAYING
                    mController?.onPlayStateChanged(mCurrentState)
                    LogUtil.d("onInfo ——> MEDIA_INFO_BUFFERING_END： STATE_PLAYING")
                }
                if (mCurrentState == PlayState.BUFFERING_PAUSED) {
                    mCurrentState = PlayState.PAUSED
                    mController?.onPlayStateChanged(mCurrentState)
                    LogUtil.d("onInfo ——> MEDIA_INFO_BUFFERING_END： STATE_PAUSED")
                }
            } else if (what == IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED) {
                // 视频旋转了extra度，需要恢复
                if (mTextureView != null) {
                    mTextureView.rotation = extra.toFloat()
                    LogUtil.d("视频旋转角度：$extra")
                }
            } else if (what == IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE) {
                LogUtil.d("视频不能seekTo，为直播视频")
            } else {
                LogUtil.d("onInfo ——> what：$what")
            }
            true
        }
        mediaPlayer.setOnBufferingUpdateListener { p0, p1 -> mBufferPercentage = p1 }
    }
}