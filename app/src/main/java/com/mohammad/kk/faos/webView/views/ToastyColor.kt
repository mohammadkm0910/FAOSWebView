package com.mohammad.kk.faos.webView.views

import android.content.Context
import es.dmoral.toasty.Toasty

@Suppress("DEPRECATION")
class ToastyColor(private var context: Context, private var text: String, private var mode:Int = 1){
    fun show(){
        var toasty = Toasty.normal(context,text,Toasty.LENGTH_SHORT)
        when(mode){
            1-> toasty = Toasty.info(context,text,Toasty.LENGTH_SHORT)
            2 -> toasty = Toasty.success(context,text,Toasty.LENGTH_SHORT)
            3 -> toasty = Toasty.warning(context,text,Toasty.LENGTH_SHORT)
            else -> Toasty.normal(context,text,Toasty.LENGTH_SHORT)
        }
        toasty.show()
    }
}