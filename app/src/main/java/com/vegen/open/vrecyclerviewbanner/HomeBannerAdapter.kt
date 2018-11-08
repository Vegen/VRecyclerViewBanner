package com.vegen.open.vrecyclerviewbanner

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.vegen.open.library.VRecyclerViewBanner

import java.util.*

/**
 * @author Vegen
 * @creation_time 2018/11/1
 * @description 首页轮播图的 Adapter
 */

class HomeBannerAdapter(var bannerList: ArrayList<String>) : RecyclerView.Adapter<HomeBannerAdapter.ViewHolder>() {

    private var onBannerItemClickListener: VRecyclerViewBanner.OnBannerItemClickListener? = null

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        var imgUrl = bannerList[p1 % bannerList.size]
        Glide.with(p0.ivBg.context).load(imgUrl).into(p0.ivBg)
        p0.itemView.setOnClickListener {
            onBannerItemClickListener?.onItemClick(p1 % bannerList.size)
        }
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.item_home_bannner, p0, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return if (bannerList.size < 2) bannerList.size else Integer.MAX_VALUE
    }

    fun setOnBannerItemClickListener(onBannerItemClickListener: VRecyclerViewBanner.OnBannerItemClickListener) {
        this.onBannerItemClickListener = onBannerItemClickListener
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ivBg: ImageView
        var tvName: TextView

        init {
            tvName = itemView.findViewById(R.id.tv_name)
            ivBg = itemView.findViewById(R.id.iv_bg)
        }
    }
}
