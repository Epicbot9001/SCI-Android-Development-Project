package com.facelab.iface.extension

import android.view.View

/**
 * Forbid Debounce
 * @author Justin Lee
 * @date 2020/4/14
 */
class DebounceOnClickListener(
        private val interval: Long = 400L,
        private val listener: (View) -> Unit
) : View.OnClickListener {

    private var lastClickTime = 0L

    override fun onClick(v: View) {
        val time = System.currentTimeMillis()
        if (time - lastClickTime >= interval) {
            lastClickTime = time
            listener(v)
        }
    }
}