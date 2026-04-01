package com.example.wallpapers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class WallpaperAdapter(
    private val onItemClick: (WallpaperImage) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_ITEM = 0
        const val VIEW_TYPE_LOADING = 1
    }

    private var isLoaderVisible = false

    private var wallpaperList = mutableListOf<WallpaperImage?>()

    override fun getItemViewType(position: Int): Int {
        return if (position == wallpaperList.size - 1 && isLoaderVisible) VIEW_TYPE_LOADING else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_ITEM) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_wallpaper, parent, false)
            return WallpaperViewHolder(view)
        }
        else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_loading, parent, false)
            return LoadingViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is WallpaperViewHolder) {
            val currentWallpaper = wallpaperList[position] ?: return

            holder.imageView.layoutParams.height = currentWallpaper.height

            Glide.with(holder.itemView.context)
                .load(currentWallpaper.webformatURL)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.color.light_gray)
                .centerCrop()
                .into(holder.imageView)

            holder.itemView.setOnClickListener {
                onItemClick(currentWallpaper)
            }
        }
        else if (holder is LoadingViewHolder) {
            val layoutParams = holder.itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams
            layoutParams.isFullSpan = true
        }
    }

    override fun getItemCount(): Int = wallpaperList.size

    fun addLoadingView() {
        isLoaderVisible = true
        wallpaperList.add(null)
        notifyItemInserted(wallpaperList.size - 1)
    }

    fun removeLoadingView() {
        isLoaderVisible = false
        val position = wallpaperList.size - 1
        if (position >= 0 && wallpaperList[position] == null) {
            wallpaperList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun setList(newList: List<WallpaperImage>) {
        wallpaperList.clear()
        newList.forEach { it.height = (600..1300).random() }
        wallpaperList.addAll(newList)
        notifyDataSetChanged()
    }

    fun addToList(additionalList: List<WallpaperImage>) {
        val startPosition = wallpaperList.size
        additionalList.forEach { it.height = (600..1300).random() }
        wallpaperList.addAll(additionalList)
        notifyItemRangeInserted(startPosition, additionalList.size)
    }

    class WallpaperViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivWallpaper)
    }

    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}