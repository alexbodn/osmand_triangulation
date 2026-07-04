package com.example.triangulation

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver

object KeyboardUtils {
    fun addKeyboardVisibilityListener(rootView: View, onVisibilityChanged: (Boolean) -> Unit) {
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            private var isKeyboardShowing = false
            private val defaultKeyboardHeightDP = 100

            override fun onGlobalLayout() {
                val r = Rect()
                rootView.getWindowVisibleDisplayFrame(r)
                val screenHeight = rootView.rootView.height

                // Calculate difference between screen height and visible display frame
                val keypadHeight = screenHeight - r.bottom

                // If keypad height is significant (more than a specific threshold), keyboard is probably showing
                val isShowing = keypadHeight > screenHeight * 0.15

                if (isShowing != isKeyboardShowing) {
                    isKeyboardShowing = isShowing
                    onVisibilityChanged(isKeyboardShowing)
                }
            }
        })
    }
}
