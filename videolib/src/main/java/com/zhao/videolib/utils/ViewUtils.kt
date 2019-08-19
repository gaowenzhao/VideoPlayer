package com.zhao.videolib.utils

import android.databinding.BindingAdapter
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
object ViewUtils {
  @BindingAdapter("app:res")
  @JvmStatic
  fun setSrc(v: ImageView, resId: Int) {
    v.setImageResource(resId)
  }

  @BindingAdapter("app:url")
  @JvmStatic
  fun setSrc(v: ImageView, url: String?) {
    Glide.with(v.context).load(url ?: "").into(v)
  }

  @BindingAdapter("app:imageUrl", "app:placeHolder", "app:error")
  @JvmStatic
  fun setSrc(v: ImageView, url: String?, holderDrawable: Int,errorDrawable:Int) {
    Glide.with(v.context)
      .load(url)
      .placeholder(holderDrawable)
      .error(errorDrawable)
      .into(v)
  }

  @BindingAdapter("app:bgcolor")
  @JvmStatic
  fun loadColor(v: View, resId: Int) {
    v.setBackgroundColor(resId)
  }

  @BindingAdapter("app:bgdrawable")
  @JvmStatic
  fun load(v: View, resId: Int) {
    v.setBackgroundResource(resId)
  }

  //=================== text =========================
  @BindingAdapter("app:textStyle")
  @JvmStatic
  fun textStyle(v: TextView, type: Int) {
    v.setTypeface(v.typeface, type)

  }

  //============================================
  @BindingAdapter("app:gone")
  @JvmStatic
  fun visGone(v: View, flag: Boolean) {
    var isVis = View.VISIBLE
    if (flag) {
      isVis = View.GONE
    }
    v.visibility = isVis
  }

  @BindingAdapter("app:invisible")
  @JvmStatic
  fun visInvisible(v: View, flag: Boolean) {
    var isVis = View.VISIBLE
    if (flag) {
      isVis = View.INVISIBLE
    }
    v.visibility = isVis
  }
}