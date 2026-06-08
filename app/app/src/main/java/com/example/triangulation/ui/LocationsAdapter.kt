package com.example.triangulation.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.triangulation.R
import com.example.triangulation.data.LibraryLocation
import android.graphics.BitmapFactory
import android.util.Base64

class LocationsAdapter(
    private var locations: List<LibraryLocation>,
    private val onPointClick: (LibraryLocation) -> Unit,
    private val onShowClick: (LibraryLocation) -> Unit,
    private val onDeleteClick: (LibraryLocation) -> Unit
) : RecyclerView.Adapter<LocationsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumb: ImageView = view.findViewById(R.id.ivLocationThumb)
        val tvDesc: TextView = view.findViewById(R.id.tvLocationDesc)
        val tvCoords: TextView = view.findViewById(R.id.tvLocationCoords)
        val btnPoint: Button = view.findViewById(R.id.btnPoint)
        val btnShow: Button = view.findViewById(R.id.btnShow)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_library_location, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val loc = locations[position]
        holder.tvDesc.text = loc.desc ?: "No Description"
        holder.tvCoords.text = "${String.format("%.5f", loc.lat)}, ${String.format("%.5f", loc.lon)}"

        holder.ivThumb.setImageDrawable(null)
        loc.img?.let { imgStr ->
            if (imgStr.startsWith("data:image")) {
                try {
                    val base64Str = imgStr.substringAfter("base64,")
                    val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    holder.ivThumb.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // Here we'd normally use Glide/Picasso for URLs.
                // For now, if it's not a data URL, we just leave it grey or add a basic downloader if needed.
            }
        }

        holder.btnPoint.setOnClickListener { onPointClick(loc) }
        holder.btnShow.setOnClickListener { onShowClick(loc) }
        holder.btnDelete.setOnClickListener { onDeleteClick(loc) }
    }

    override fun getItemCount() = locations.size

    fun updateData(newLocations: List<LibraryLocation>) {
        locations = newLocations
        notifyDataSetChanged()
    }
}
