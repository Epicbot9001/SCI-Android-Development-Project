package edu.gatech.ce.allgather.extend

import android.view.View

/**
 * 显示效果相关
 * @author Justin Lee
 * @date 2020/5/18
 */
fun View.visible() {
    this.visibility = View.VISIBLE
}


fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}