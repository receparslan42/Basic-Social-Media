package com.receparslan.basicsocialmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.receparslan.basicsocialmedia.databinding.RecyclerRowBinding
import com.receparslan.basicsocialmedia.models.Post
import com.squareup.picasso.Picasso

class RecyclerAdapter(private val postArrayList: ArrayList<Post>) : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.emailTextView.text = postArrayList[position].email
        holder.binding.dateTextView.text = postArrayList[position].date
        holder.binding.displayNameTextView.text = String.format("%s : ", postArrayList[position].displayName)
        val comment = holder.binding.displayNameTextView.text.toString() + postArrayList[position].comment
        holder.binding.commentTextView.text = comment
        postArrayList[position].imageUri?.let {
            Picasso.get().load(it).into(holder.binding.imageView)
        }
    }

    override fun getItemCount(): Int {
        return postArrayList.size
    }

    class ViewHolder(val binding: RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root)
}