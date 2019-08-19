package com.zhao.videolib

import android.content.Context
import android.util.AttributeSet
import com.zhao.videolib.bean.PlayMode
import com.zhao.videolib.bean.PlayState

/**
 * 各种状态的判断
 */
 abstract class BaseStatePlayer : BaseModePlayer {
     constructor(context: Context) : super(context)
     constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
     constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
     override fun isIdle(): Boolean {
         return mCurrentState == PlayState.IDLE
     }

     override fun isPreparing(): Boolean {
         return mCurrentState == PlayState.PREPARING
     }

     override fun isPrepared(): Boolean {
         return mCurrentState == PlayState.PREPARED
     }

     override fun isBufferingPlaying(): Boolean {
         return mCurrentState == PlayState.BUFFERING_PLAYING
     }

     override fun isBufferingPaused(): Boolean {
         return mCurrentState == PlayState.BUFFERING_PAUSED
     }

     override fun isPlaying(): Boolean {
         return mCurrentState == PlayState.PLAYING
     }

     override fun isPaused(): Boolean {
         return mCurrentState == PlayState.PAUSED
     }

     override fun isError(): Boolean {
         return mCurrentState == PlayState.ERROR
     }

     override fun isCompleted(): Boolean {
         return mCurrentState == PlayState.COMPLETED
     }
     override fun isFullScreen(): Boolean {
         return mCurrentMode == PlayMode.FULL_SCREEN
     }

     override fun isTinyWindow(): Boolean {
         return mCurrentMode == PlayMode.TINY_WINDOW
     }

     override fun isNormal(): Boolean {
         return mCurrentMode == PlayMode.NORMAL
     }
}