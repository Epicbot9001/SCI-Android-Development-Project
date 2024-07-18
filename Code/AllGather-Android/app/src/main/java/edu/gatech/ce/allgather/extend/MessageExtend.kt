package edu.gatech.ce.allgather.extend

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

/**
 * This Extend Is Used For Message Hint
 * @author Justin Lee
 * @date 2020/5/14
 */
/**
 * toast extend
 */
fun Fragment.toast(message: String?) {
    Toast.makeText(this.context, message, Toast.LENGTH_SHORT).show()
}

fun Activity.toast(message: String?) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Fragment.toast(@StringRes resId: Int) {
    Toast.makeText(this.context, resId, Toast.LENGTH_SHORT).show()
}

fun Activity.toast(@StringRes resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
}

fun Fragment.toastLong(message: String?) {
    Toast.makeText(this.context, message, Toast.LENGTH_LONG).show()
}

fun Activity.toastLong(message: String?) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Fragment.toastLong(@StringRes resId: Int) {
    Toast.makeText(this.context, resId, Toast.LENGTH_LONG).show()
}

fun Activity.toastLong(@StringRes resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
}

/**
 * snack bar extend
 */
fun View.snackBar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG).show()
}

fun View.snackBar(@StringRes stringId: Int) {
    Snackbar.make(this, stringId, Snackbar.LENGTH_LONG).show()
}
