package com.zhao.videolib.utils

import com.zhao.videolib.inf.BasePlayerI

class VideoPlayerManager private constructor() {
    var mVideoPlayer: BasePlayerI? = null
    companion object {
        private val instance by lazy { VideoPlayerManager() }
        @Synchronized
        fun get(): VideoPlayerManager {
            return instance
        }
    }

    fun setVideoPlayer(videoPlayer: BasePlayerI) {
        if (mVideoPlayer !== videoPlayer) {
            releaseVideoPlayer()
            mVideoPlayer = videoPlayer
        }
    }
    fun pauseVideoPlayer() {
        mVideoPlayer?.apply {
            if(isPlaying()||isBufferingPlaying()){
                pause()
            }
        }
    }

    fun resumeVideoPlayer() {
        mVideoPlayer?.apply {
            if(isPaused()||isBufferingPaused()){
                restart()
            }
        }
    }
    fun releaseVideoPlayer() {
        mVideoPlayer?.release()
        mVideoPlayer = null
    }
    fun onBackPressd(): Boolean {
        mVideoPlayer?.run {
            if (isFullScreen()) {
                return exitFullScreen()
            } else if (isTinyWindow()) {
                return exitTinyWindow()
            }
        }
        return false
    }
}