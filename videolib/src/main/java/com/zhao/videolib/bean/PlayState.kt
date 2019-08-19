package com.zhao.videolib.bean

enum class PlayState(state:String) {
    ERROR("播放错误"),
    IDLE("播放未开始"),
    PREPARING("播放准备中"),
    PREPARED("播放准备就绪"),
    PLAYING("正在播放"),
    PAUSED("暂停播放"),
    BUFFERING_PLAYING("正在缓冲(播放器正在播放时，缓冲区数据不足，进行缓冲，缓冲区数据足够后恢复播放)"),
    BUFFERING_PAUSED("正在缓冲(播放器正在播放时，缓冲区数据不足，进行缓冲，此时暂停播放器，继续缓冲，缓冲区数据足够后恢复暂停"),
    COMPLETED("播放完成")
}