package com.zhao.videoplayer
import com.squareup.leakcanary.LeakCanary
import com.zhao.base.app.BaseApplication

class MyApplication : BaseApplication(){
    override fun onCreate() {
        super.onCreate()
        initLeakCanary()
    }
    private fun initLeakCanary(){
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }
        LeakCanary.install(this)
    }
}
