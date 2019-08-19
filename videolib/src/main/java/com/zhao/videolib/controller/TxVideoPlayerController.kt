package com.zhao.videolib.controller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.BatteryManager
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.zhao.videolib.R
import com.zhao.videolib.bean.PlayMode
import com.zhao.videolib.bean.PlayState
import com.zhao.videolib.bean.VideoBean
import com.zhao.videolib.databinding.TxVideoPalyerControllerBinding
import com.zhao.videolib.inf.BasePlayerI
import com.zhao.videolib.utils.VideoUtils
import kotlinx.android.synthetic.main.tx_video_palyer_controller.view.*

import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

/**
 * Created by XiaoJianjun on 2017/6/21.
 * 仿腾讯视频热点列表页播放器控制器.
 */
class TxVideoPlayerController(private val mContext: Context) : VideoPlayerController(mContext),
    View.OnClickListener, SeekBar.OnSeekBarChangeListener, ChangeClarityDialog.OnClarityChangedListener {
    private var topBottomVisible: Boolean = false
    private var mDismissTopBottomCountDownTimer: CountDownTimer? = null
    private var clarities: List<Clarity>? = null
    private var defaultClarityIndex: Int = 0
    private var clarityDialog: ChangeClarityDialog? = null
    private val ui by lazy { DataBindingUtil.inflate<TxVideoPalyerControllerBinding>(LayoutInflater.from(mContext),R.layout.tx_video_palyer_controller,this,true) }
    private val vm by lazy{ ControllerVM()}
    init {
        init()
    }
    private fun init() {
        ui.vm = vm
        center_start.setOnClickListener(this)
        back.setOnClickListener(this)
        restart_or_pause.setOnClickListener(this)
        full_screen.setOnClickListener(this)
        clarity.setOnClickListener(this)
        retry.setOnClickListener(this)
        replay.setOnClickListener(this)
        share.setOnClickListener(this)
        seek.setOnSeekBarChangeListener(this)
        this.setOnClickListener(this)

    }
    override fun setVideoInfo(info: VideoBean) {
        vm.videoInfo = info
    }

    override fun setNiceVideoPlayer(videoPlayer: BasePlayerI) {
        super.setNiceVideoPlayer(videoPlayer)
        // 给播放器配置视频链接地址
        if (clarities != null && clarities!!.size > 1) {
            this.videoPlayer.setUp(clarities!![defaultClarityIndex].videoUrl, null!!)
        }
    }

    /**
     * 设置清晰度
     *
     * @param clarities 清晰度及链接
     */
    fun setClarity(clarities: List<Clarity>?, defaultClarityIndex: Int) {
        if (clarities != null && clarities.size > 1) {
            this.clarities = clarities
            this.defaultClarityIndex = defaultClarityIndex

            val clarityGrades = ArrayList<String>()
            for (clarity in clarities) {
                clarityGrades.add(clarity.grade + " " + clarity.p)
            }
            clarity.text = clarities[defaultClarityIndex].grade
            // 初始化切换清晰度对话框
            clarityDialog = ChangeClarityDialog(mContext)
            clarityDialog?.setClarityGrade(clarityGrades, defaultClarityIndex)
            clarityDialog?.setOnClarityCheckedListener(this)
            // 给播放器配置视频链接地址
            if (videoPlayer != null) {
                videoPlayer.setUp(clarities[defaultClarityIndex].videoUrl, null!!)
            }
        }
    }

    override fun onPlayStateChanged(playState: PlayState) {
        when (playState) {
            PlayState.IDLE -> {
            }
            PlayState.PREPARING -> {
                image.visibility = View.GONE
                loading.visibility = View.VISIBLE
                tvLoadText.text = "正在准备..."
                error.visibility = View.GONE
                completed.visibility = View.GONE
                lL_top.visibility = View.GONE
                lLbottom.visibility = View.GONE
                center_start.visibility = View.GONE
                length.visibility = View.GONE
            }
            PlayState.PREPARED -> {
                startUpdateProgressTimer()
            }
            PlayState.PLAYING -> {
                loading.visibility = View.GONE
                restart_or_pause.setImageResource(R.drawable.ic_player_pause)
                startDismissTopBottotvTimer()
            }

            PlayState.PAUSED -> {
                loading.visibility = View.GONE
                restart_or_pause.setImageResource(R.drawable.ic_player_start)
                cancelDismissTopBottotvTimer()
            }

            PlayState.BUFFERING_PLAYING -> {
                loading.visibility = View.VISIBLE
                restart_or_pause.setImageResource(R.drawable.ic_player_pause)
                tvLoadText.text = "正在缓冲..."
                startDismissTopBottotvTimer()
            }

            PlayState.BUFFERING_PAUSED -> {
                loading.visibility = View.VISIBLE
                restart_or_pause.setImageResource(R.drawable.ic_player_start)
                tvLoadText.text = "正在缓冲..."
                cancelDismissTopBottotvTimer()
            }
            PlayState.ERROR -> {
                cancelUpdateProgressTimer()
                setTopBottomVisible(false)
                lL_top.visibility = View.VISIBLE
                error.visibility = View.VISIBLE
            }
            PlayState.COMPLETED -> {
                cancelUpdateProgressTimer()
                setTopBottomVisible(false)
                image.visibility = View.VISIBLE
                completed.visibility = View.VISIBLE
            }

        }
    }

    override fun onPlayModeChanged(playMode: PlayMode) {
        when (playMode) {
            PlayMode.NORMAL -> {
                back.visibility = View.GONE
                full_screen.setImageResource(R.drawable.ic_player_enlarge)
                full_screen.visibility = View.VISIBLE
                clarity.visibility = View.GONE
            }

            PlayMode.FULL_SCREEN -> {
                back.visibility = View.VISIBLE
                full_screen.visibility = View.GONE
                full_screen.setImageResource(R.drawable.ic_player_shrink)
                if (clarities != null && clarities!!.size > 1) {
                    clarity.visibility = View.VISIBLE
                }
            }

            PlayMode.TINY_WINDOW -> {
                back.visibility = View.VISIBLE
                clarity.visibility = View.GONE
            }
        }
    }

    override fun reset() {
        topBottomVisible = false
        cancelUpdateProgressTimer()
        cancelDismissTopBottotvTimer()
        seek.progress = 0
        seek.secondaryProgress = 0

        center_start.visibility = View.VISIBLE
        image.visibility = View.VISIBLE

        lLbottom.visibility = View.GONE
        full_screen.setImageResource(R.drawable.ic_player_enlarge)

        length.visibility = View.VISIBLE

        lL_top.visibility = View.VISIBLE
        back.visibility = View.GONE

        loading.visibility = View.GONE
        error.visibility = View.GONE
        completed.visibility = View.GONE
    }

    /**
     * 尽量不要在onClick中直接处理控件的隐藏、显示及各种UI逻辑。
     * UI相关的逻辑都尽量到[.onPlayStateChanged]和[.onPlayModeChanged]中处理.
     */
    override fun onClick(v: View) {
        when (v) {
            center_start -> if (videoPlayer.isIdle()) {
                videoPlayer.start()
            }
            back -> if (videoPlayer.isFullScreen()) {
                videoPlayer.exitFullScreen()
            } else if (videoPlayer.isTinyWindow()) {
                videoPlayer.exitTinyWindow()
            }
            restart_or_pause -> if (videoPlayer.isPlaying() || videoPlayer.isBufferingPlaying()) {
                videoPlayer.pause()
            } else if (videoPlayer.isPaused() || videoPlayer.isBufferingPaused()) {
                videoPlayer.restart()
            }
            full_screen -> if (videoPlayer.isNormal() || videoPlayer.isTinyWindow()) {
                videoPlayer.enterFullScreen()
            } else if (videoPlayer.isFullScreen()) {
                videoPlayer.exitFullScreen()
            }
            clarity -> {
                setTopBottomVisible(false) // 隐藏top、bottom
                clarityDialog!!.show()     // 显示清晰度对话框
            }
            retry -> videoPlayer.restart()
            replay -> retry.performClick()
            share -> Toast.makeText(mContext, "分享", Toast.LENGTH_SHORT).show()
            this -> if (videoPlayer.isPlaying()
                || videoPlayer.isPaused()
                || videoPlayer.isBufferingPlaying()
                || videoPlayer.isBufferingPaused()
            ) {
                setTopBottomVisible(!topBottomVisible)
            }
        }
    }

    override fun onClarityChanged(clarityIndex: Int) {
        // 根据切换后的清晰度索引值，设置对应的视频链接地址，并从当前播放位置接着播放
        val clarity = clarities!![clarityIndex]
        this.clarity.text = clarity.grade
        val currentPosition = videoPlayer.getCurrentPosition()
        videoPlayer.releasePlayer()
        videoPlayer.setUp(clarity.videoUrl, null!!)
        videoPlayer.start(currentPosition)
    }

    override fun onClarityNotChanged() {
        // 清晰度没有变化，对话框消失后，需要重新显示出top、bottom
        setTopBottomVisible(true)
    }

    /**
     * 设置top、bottom的显示和隐藏
     *
     * @param visible true显示，false隐藏.
     */
    private fun setTopBottomVisible(visible: Boolean) {
        lL_top.visibility = if (visible) View.VISIBLE else View.GONE
        lLbottom.visibility = if (visible) View.VISIBLE else View.GONE
        topBottomVisible = visible
        if (visible) {
            if (!videoPlayer.isPaused() && !videoPlayer.isBufferingPaused()) {
                startDismissTopBottotvTimer()
            }
        } else {
            cancelDismissTopBottotvTimer()
        }
    }
    /**
     * 开启top、bottom自动消失的timer
     */
    private fun startDismissTopBottotvTimer() {
        cancelDismissTopBottotvTimer()
        if (mDismissTopBottomCountDownTimer == null) {
            mDismissTopBottomCountDownTimer = object : CountDownTimer(8000, 8000) {
                override fun onTick(millisUntilFinished: Long) {
                }
                override fun onFinish() {
                    setTopBottomVisible(false)
                }
            }
        }
        mDismissTopBottomCountDownTimer!!.start()
    }

    /**
     * 取消top、bottom自动消失的timer
     */
    private fun cancelDismissTopBottotvTimer() {
        if (mDismissTopBottomCountDownTimer != null) {
            mDismissTopBottomCountDownTimer!!.cancel()
        }
    }
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (videoPlayer.isBufferingPaused() || videoPlayer.isPaused()) {
            videoPlayer.restart()
        }
        val position = (videoPlayer.getDuration() * seekBar.progress / 100f).toLong()
        videoPlayer.seekTo(position)
        startDismissTopBottotvTimer()
    }

    override fun updateProgress() {
        val position = videoPlayer.getCurrentPosition()
        val duration = videoPlayer.getDuration()
        val bufferPercentage = videoPlayer.getBufferPercentage()
        seek.secondaryProgress = bufferPercentage
        val progress = (100f * position / duration).toInt()
        seek.progress = progress
        tvPosition.text = VideoUtils.formatTime(position)
        tvDuration.text = VideoUtils.formatTime(duration)
    }

    override fun showChangePosition(duration: Long, newPositionProgress: Int) {
        change_position.visibility = View.VISIBLE
        val newPosition = (duration * newPositionProgress / 100f).toLong()
        change_position_current.text = VideoUtils.formatTime(newPosition)
        change_position_progress.progress = newPositionProgress
        seek.progress = newPositionProgress
        tvPosition.text = VideoUtils.formatTime(newPosition)
    }

    override fun hideChangePosition() {
        change_position.visibility = View.GONE
    }

    override fun showChangeVolume(newVolumeProgress: Int) {
        change_volume.visibility = View.VISIBLE
        change_volume_progress.progress = newVolumeProgress
    }

    override fun hideChangeVolume() {
        change_volume.visibility = View.GONE
    }

    override fun showChangeBrightness(newBrightnessProgress: Int) {
        change_brightness.visibility = View.VISIBLE
        change_brightness_progress.progress = newBrightnessProgress
    }

    override fun hideChangeBrightness() {
        change_brightness.visibility = View.GONE
    }
}
