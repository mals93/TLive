package com.testexam.charlie.tlive.main.live


import android.content.Context
import android.content.Intent
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso

import com.testexam.charlie.tlive.R
import com.testexam.charlie.tlive.main.live.webrtc.viewer.ViewerActivity
import com.testexam.charlie.tlive.main.live.webrtc.vod.VodActivity

/**
 *
 * Created by charlie on 2018. 5. 24
 */

class BroadcastAdapter(private var broadcastList : ArrayList<Broadcast>,val context : Context) : RecyclerView.Adapter<BroadcastAdapter.ViewHolder>()  {
    private val serverUrl = "http://13.125.64.135"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_broadcast_card,parent,false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val broadcast = broadcastList[position]
        holder.broadHostNameTv.text = broadcast.hostName
        holder.broadRoomNameTv.text = broadcast.roomName
        holder.broadRoomTagTv.text = broadcast.roomTag
        holder.broadHostIv.setImageDrawable(context.getDrawable(R.drawable.ic_profile_ex1))

        if(broadcast.isLive == 1){
            holder.broadLiveTv.visibility = View.VISIBLE
            holder.broadVodTv.visibility = View.GONE
            holder.broadCardView.setOnClickListener( {
                val intent = Intent(context,ViewerActivity::class.java)
                intent.putExtra("presenterSessionId",broadcast.roomSessionNo)
                context.startActivity(intent)
            })
            holder.broadLiveNumTv.visibility = View.VISIBLE
            holder.broadLiveNumTv.text = "시청자"+broadcast.viewerNum.toString()+"명"
            holder.broadLiveCircle.visibility = View.VISIBLE

            Picasso.get().load(serverUrl+"/livePreview/"+broadcast.previewUrl).into(holder.broadPreviewIv)
        }else{
            holder.broadLiveTv.visibility = View.GONE
            holder.broadVodTv.visibility = View.VISIBLE
            holder.broadCardView.setOnClickListener( {
                val intent = Intent(context,VodActivity::class.java)
                intent.putExtra("vodUrl",broadcast.vodUrl)
                context.startActivity(intent)
            })
            if(broadcast.previewUrl.endsWith("png")){
                Glide.with(context)
                        .load(serverUrl+"/preview/"+broadcast.previewUrl)
                        .into(holder.broadPreviewIv)
                Log.d("preview","png")
            }
            Log.d("vodUrl",broadcast.vodUrl);
            holder.broadLiveNumTv.visibility = View.GONE
            holder.broadLiveCircle.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return broadcastList.size
    }

    fun setData(list : ArrayList<Broadcast>){
        broadcastList = list
    }

    class ViewHolder (broadView : View) : RecyclerView.ViewHolder(broadView){
        val broadCardView = broadView.findViewById<CardView>(R.id.broadCardView)!!

        val broadLiveCircle = broadView.findViewById<View>(R.id.broadLiveCircle)!!

        val broadPreviewIv = broadView.findViewById<ImageView>(R.id.broadPreviewIv)!!
        val broadHostIv = broadView.findViewById<ImageView>(R.id.broadHostIv)!!
        val broadMoreInfoIv = broadView.findViewById<ImageView>(R.id.broadMoreInfoIv)!!


        val broadHostNameTv = broadView.findViewById<TextView>(R.id.broadHostNameTv)!!
        val broadRoomNameTv = broadView.findViewById<TextView>(R.id.broadRoomNameTv)!!
        val broadRoomTagTv = broadView.findViewById<TextView>(R.id.broadRoomTagTv)!!
        val broadLiveTv = broadView.findViewById<TextView>(R.id.broadLiveTv)!!
        val broadVodTv = broadView.findViewById<TextView>(R.id.broadVodTv)!!
        val broadLiveNumTv = broadView.findViewById<TextView>(R.id.broadLiveNumTv)!!
    }
}