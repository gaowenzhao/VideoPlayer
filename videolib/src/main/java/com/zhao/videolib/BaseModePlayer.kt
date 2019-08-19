package com.zhao.videolib

import android.content.Context
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import com.zhao.videolib.bean.PlayMode
import com.zhao.videolib.utils.LogUtil
import com.zhao.videolib.utils.VideoUtils

/**
 * 全屏/小窗口/普通 切换
 */
 abstract class BaseModePlayer : BaseMediaPlayer {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun enterFullScreen() {
        if (mCurrentMode == PlayMode.FULL_SCREEN) return
        // 隐藏ActionBar、状态栏，并横屏
        VideoUtils.hideActionBar(context)
        VideoUtils.scanForActivity(context)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val contentView = VideoUtils.scanForActivity(context)
            ?.findViewById(android.R.id.content) as ViewGroup
        if (mCurrentMode == PlayMode.TINY_WINDOW) {
            contentView.removeView(mContainer)
        } else {
            this.removeView(mContainer)
        }
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        contentView.addView(mContainer, params)
        mCurrentMode = PlayMode.FULL_SCREEN
         mController?.onPlayModeChanged(mCurrentMode)
         LogUtil.d("MODE_FULL_SCREEN")
    }

    override fun exitFullScreen(): Boolean {
        if (mCurrentMode == PlayMode.FULL_SCREEN) {
            VideoUtils.showActionBar(context)
            VideoUtils.scanForActivity(context)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            val contentView = VideoUtils.scanForActivity(context)
                ?.findViewById(android.R.id.content) as ViewGroup
            contentView.removeView(mContainer)
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            this.addView(mContainer, params)

            mCurrentMode = PlayMode.NORMAL
            mController?.onPlayModeChanged(mCurrentMode)
//            LogUtil.d("MODE_NORMAL")
            return true
        }
        return false
    }

    override fun enterTinyWindow() {
        if (mCurrentMode == PlayMode.TINY_WINDOW) return
        this.removeView(mContainer)

        val contentView = VideoUtils.scanForActivity(context)
            ?.findViewById(android.R.id.content) as ViewGroup
        // 小窗口的宽度为屏幕宽度的60%，长宽比默认为16:9，右边距、下边距为8dp。
        val params = LayoutParams(
            (VideoUtils.getScreenWidth(context) * 0.6f).toInt(),
            (VideoUtils.getScreenWidth(context) * 0.6f * 9f / 16f).toInt()
        )
        params.gravity = Gravity.BOTTOM or Gravity.END
        params.rightMargin = VideoUtils.dp2px(context, 8f)
        params.bottomMargin = VideoUtils.dp2px(context, 8f)

        contentView.addView(mContainer, params)

        mCurrentMode = PlayMode.TINY_WINDOW
        mController?.onPlayModeChanged(mCurrentMode)
//        LogUtil.d("MODE_TINY_WINDOW")
    }

    override fun exitTinyWindow(): Boolean {
        if (mCurrentMode == PlayMode.TINY_WINDOW) {
            val contentView = VideoUtils.scanForActivity(context)
                ?.findViewById(android.R.id.content) as ViewGroup
            contentView.removeView(mContainer)
            val params = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            this.addView(mContainer, params)

            mCurrentMode = PlayMode.NORMAL
            mController?.onPlayModeChanged(mCurrentMode)
//            LogUtil.d("MODE_NORMAL")
            return true
        }
        return false
    }
}