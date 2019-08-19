package com.zhao.videolib.utils

import android.util.Log

/**
 * Created by XiaoJianjun on 2017/5/4.
 * log工具.
 */
object LogUtil {

    private val TAG = "NiceVideoPlayer"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun e(message: String, throwable: Throwable) {
        Log.e(TAG, message, throwable)
    }
}
