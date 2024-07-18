package edu.gatech.ce.allgather.extend

import android.view.View
import com.facelab.iface.extension.DebounceOnClickListener

/**
 * Click Extend
 * @author Justin Lee
 * @date 2020/6/10
 */

/**
 * forbid debounce chain
 *
 * @param debounceInterval debounce interval, normal 400ms
 * @param listener
 * Created by Justin Lee
 */
fun View.setOnClickDebounceListener(debounceInterval: Long = 400L, listener: (View) -> Unit) =
        setOnClickListener(DebounceOnClickListener(debounceInterval, listener))
