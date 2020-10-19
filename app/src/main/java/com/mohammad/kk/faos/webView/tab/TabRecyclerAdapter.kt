package com.mohammad.kk.faos.webView.tab

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.alpha
import androidx.recyclerview.widget.RecyclerView
import com.mohammad.kk.faos.webView.R

class TabRecyclerAdapter(private var context: Context,private var itemTab:ArrayList<Tab>,private var switchTab:(Int) -> Unit) : RecyclerView.Adapter<TabRecyclerAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTab: TextView = itemView.findViewById(R.id.titleTab)
        val iconTab: ImageView = itemView.findViewById(R.id.iconTab)
        val tabClick: CardView = itemView.findViewById(R.id.tabClick)
        fun switcherTab(index:Int,switchTab: (Int) -> Unit){
            tabClick.setOnClickListener {
                switchTab(index)
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tab_recycler,parent,false))
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val list = itemTab[position]
        holder.iconTab.setImageBitmap(list.webView.favicon)
        holder.titleTab.text = list.webView.title
        holder.switcherTab(position,switchTab)
        if (sendIndex == position)
            holder.tabClick.setCardBackgroundColor(ContextCompat.getColor(context,R.color.amber300))
        else
            holder.tabClick.setCardBackgroundColor(ContextCompat.getColor(context,R.color.white))
    }
    override fun getItemCount(): Int {
        return itemTab.size
    }
    var sendIndex = 0
}