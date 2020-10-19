package com.mohammad.kk.faos.webView.views

import android.app.Activity
import android.view.View
import com.google.android.material.snackbar.Snackbar


class RootSnackBar (private var activity: Activity,private var text:String){
    fun show() {
        val rootView:View = activity.window.decorView.findViewById(android.R.id.content)
        Snackbar.make(rootView,text,Snackbar.LENGTH_LONG).show()
    }
}
