package com.zhao.videolib

import android.content.Context
import android.view.TextureView

/**
 * Created by XiaoJianjun on 2017/6/21.
 * 重写TextureView，适配视频的宽高和旋转.
 * （参考自节操播放器 https://github.com/lipangit/JieCaoVideoPlayer）
 */
class NiceTextureView(context: Context) : TextureView(context) {

  private var videoHeight: Int = 0
  private var videoWidth: Int = 0

  fun adaptVideoSize(videoWidth: Int, videoHeight: Int) {
    if (this.videoWidth != videoWidth && this.videoHeight != videoHeight) {
      this.videoWidth = videoWidth
      this.videoHeight = videoHeight
      requestLayout()
    }
  }

  override fun setRotation(rotation: Float) {
    if (rotation != getRotation()) {
      super.setRotation(rotation)
      requestLayout()
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    var widthMeasureSpec = widthMeasureSpec
    var heightMeasureSpec = heightMeasureSpec

    val viewRotation = rotation

    // 如果判断成立，则说明显示的TextureView和本身的位置是有90度的旋转的，所以需要交换宽高参数。
    if (viewRotation == 90f || viewRotation == 270f) {
      val tempMeasureSpec = widthMeasureSpec
      widthMeasureSpec = heightMeasureSpec
      heightMeasureSpec = tempMeasureSpec
    }

    var width = getDefaultSize(videoWidth, widthMeasureSpec)
    var height = getDefaultSize(videoHeight, heightMeasureSpec)
    if (videoWidth > 0 && videoHeight > 0) {

      val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
      val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
      val heightSpecMode =MeasureSpec.getMode(heightMeasureSpec)
      val heightSpecSize =MeasureSpec.getSize(heightMeasureSpec)

      if (widthSpecMode ==MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
        // the size is fixed
        width = widthSpecSize
        height = heightSpecSize
        // for compatibility, we adjust size based on aspect ratio
        if (videoWidth * height < width * videoHeight) {
          width = height * videoWidth / videoHeight
        } else if (videoWidth * height > width * videoHeight) {
          height = width * videoHeight / videoWidth
        }
      } else if (widthSpecMode == MeasureSpec.EXACTLY) {
        // only the width is fixed, adjust the height to match aspect ratio if possible
        width = widthSpecSize
        height = width * videoHeight / videoWidth
        if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
          // couldn't match aspect ratio within the constraints
          height = heightSpecSize
          width = height * videoWidth / videoHeight
        }
      } else if (heightSpecMode == MeasureSpec.EXACTLY) {
        // only the height is fixed, adjust the width to match aspect ratio if possible
        height = heightSpecSize
        width = height * videoWidth / videoHeight
        if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
          // couldn't match aspect ratio within the constraints
          width = widthSpecSize
          height = width * videoHeight / videoWidth
        }
      } else {
        // neither the width nor the height are fixed, try to use actual video size
        width = videoWidth
        height = videoHeight
        if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
          // too tall, decrease both width and height
          height = heightSpecSize
          width = height * videoWidth / videoHeight
        }
        if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
          // too wide, decrease both width and height
          width = widthSpecSize
          height = width * videoHeight / videoWidth
        }
      }
    } else {
      // no size yet, just adopt the given spec sizes
    }
    setMeasuredDimension(width, height)
  }
}
