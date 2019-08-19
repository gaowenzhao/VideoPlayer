package com.zhao.videoplayer
import android.content.Context
import android.view.View
import android.widget.Toast
import com.zhao.base.inf.BaseSimpleActivity
import com.zhao.videolib.bean.VideoBean
import com.zhao.videolib.controller.TxVideoPlayerController
import com.zhao.videolib.utils.VideoPlayerManager
import com.zhao.videoplayer.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Context.AUDIO_SERVICE
import android.support.annotation.NonNull


class MainActivity : BaseSimpleActivity<ActivityMainBinding>() {
    override var layoutId: Int = R.layout.activity_main
    override fun initData() {
    }
    override fun initView() {
        val videoUrl = "http://tanzi27niu.cdsb.mobi/wps/wp-content/uploads/2017/05/2017-05-17_17-33-30.mp4"
        video_player.setUp(videoUrl,null)
        val controller = TxVideoPlayerController(this)
        controller.setVideoInfo(VideoBean().apply {
            title="办公室小野开番外了哈哈哈哈"
            imgUrl = "http://tanzi27niu.cdsb.mobi/wps/wp-content/uploads/2017/05/2017-05-17_17-30-43.jpg"
            playLength = 98000
        })
        video_player.setController(controller)
    }
    fun enterTinyWindow(view: View) {
        if (video_player.isIdle()) {
            Toast.makeText(this, "要点击播放后才能进入小窗口", Toast.LENGTH_SHORT).show()
        } else {
            video_player.enterTinyWindow()
        }
    }
    override fun onStop() {
        super.onStop()
        VideoPlayerManager.get().releaseVideoPlayer()
    }

    override fun onBackPressed() {
        if (VideoPlayerManager.get().onBackPressd()) return
        super.onBackPressed()
    }
    //Android系统 6.0以下AudioManager内存泄漏问题
    override fun getSystemService(name: String): Any {
        return if (Context.AUDIO_SERVICE == name) applicationContext.getSystemService(name) else super.getSystemService(name)
    }
}
