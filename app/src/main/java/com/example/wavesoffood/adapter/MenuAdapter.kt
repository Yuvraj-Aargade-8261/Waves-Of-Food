package com.example.wavesoffood.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.wavesoffood.DetailsActivity
import com.example.wavesoffood.databinding.MenuitemBinding
import com.example.wavesoffood.model.MenuItem

class MenuAdapter(
    private val menuItems: List<MenuItem>,
    private val context: Context,
    private val userLat: Double,
    private val userLng: Double
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = MenuitemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = menuItems.size

    inner class MenuViewHolder(private val binding: MenuitemBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val menuItem = menuItems[position]
                    val intent = Intent(context, DetailsActivity::class.java).apply {
                        putExtra("MenuItemName", menuItem.foodName)
                        putExtra("MenuItemPrice", menuItem.foodPrice)
                        putExtra("MenuItemImage", menuItem.foodImage)
                        putExtra("MenuItemDescription", menuItem.foodDescription)
                        putExtra("MenuItemIngredients", menuItem.foodIngredients)
                        putExtra("HotelUserId", menuItem.hotelUserId)
                        putExtra("HotelName", menuItem.hotelName)
                    }
                    context.startActivity(intent)
                }
            }
        }

        fun bind(position: Int) {
            val menuItem = menuItems[position]

            // Set basic info
            binding.menuFoodNameid.text = menuItem.foodName ?: "Unnamed Food"
            binding.menuPriceid.text = menuItem.foodPrice ?: "₹0"
            binding.menuHotelNameid.text = menuItem.hotelName ?: "Unknown Hotel"

            // Load food image
            Glide.with(context)
                .load(Uri.parse(menuItem.foodImage ?: ""))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(binding.menuImageid)

            // Use pre-calculated distance and time
            val distance = menuItem.distanceInKm
            val time = menuItem.estimatedTimeMin

            if (distance != null && time != null) {
                binding.menuDistanceTimeText.text = String.format("%.1f km • %d min", distance, time)
            } else {
                binding.menuDistanceTimeText.text = "Distance Unavailable"
            }
        }
    }
}
