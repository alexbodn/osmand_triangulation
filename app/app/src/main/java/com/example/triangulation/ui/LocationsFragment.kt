package com.example.triangulation.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.triangulation.MainActivity
import com.example.triangulation.OsmAndAidlHelper
import com.example.triangulation.R
import com.example.triangulation.data.LibraryLocation
import com.example.triangulation.data.LocationLibraryManager

class LocationsFragment : Fragment(), OsmAndAidlHelper.OsmAndAidlListener {

    private lateinit var rvLocations: RecyclerView
    private lateinit var adapter: LocationsAdapter
    private lateinit var libraryManager: LocationLibraryManager
    private lateinit var osmandHelper: OsmAndAidlHelper

    // Optional bounding box filter
    private var bboxFilter: DoubleArray? = null // [minLon, minLat, maxLon, maxLat]

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_locations, container, false)
    }


    private val updateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == "com.example.triangulation.BBOX_FILTER") {
                val bbox = intent.getDoubleArrayExtra("bbox")
                setBboxFilter(bbox)
            } else if (intent?.action == "com.example.triangulation.REFRESH_LIBRARY") {
                loadData()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        libraryManager = LocationLibraryManager(requireContext())
        osmandHelper = OsmAndAidlHelper(requireActivity().application, this)

        rvLocations = view.findViewById(R.id.rvLocations)
        rvLocations.layoutManager = LinearLayoutManager(requireContext())

        adapter = LocationsAdapter(
            emptyList(),
            onPointClick = { loc ->
                // Switch to home tab and set as current
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra("lat", loc.lat)
                intent.putExtra("lon", loc.lon)
                intent.putExtra("desc", loc.desc)
                startActivity(intent)

                // If using ViewPager, we could also just change the current item.
                (activity as? MainActivity)?.let { mainActivity ->
                    val viewPager = mainActivity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
                    viewPager?.currentItem = 0
                }
            },
            onShowClick = { loc ->
                val launchIntent = requireActivity().packageManager.getLaunchIntentForPackage("net.osmand.plus")
                    ?: requireActivity().packageManager.getLaunchIntentForPackage("net.osmand")
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(launchIntent)
                }

                Thread {
                    Thread.sleep(300)
                    if (!osmandHelper.setMapLocation(loc.lat, loc.lon, 15)) {
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Failed to set OsmAnd location", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            },
            onDeleteClick = { loc ->
                libraryManager.removeLocation(loc.lat, loc.lon)
                loadData()
                // Update Home tab active list if needed? It pulls automatically on next resume/refresh if we use shared prefs or DB, but here they are disjoint.
                // The requirements say: "Deleting an active location from the Home tab should remove it from the active list and map, but NOT delete it from the library."
                // "deleting a location from the lib should not remove it from active. just set the save button in active for the location."
            }
        )

        rvLocations.adapter = adapter
    }

    override fun onResume() {

        super.onResume()
        val filter = android.content.IntentFilter()
        filter.addAction("com.example.triangulation.BBOX_FILTER")
        filter.addAction("com.example.triangulation.REFRESH_LIBRARY")
        androidx.core.content.ContextCompat.registerReceiver(requireContext(), updateReceiver, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)

        osmandHelper.bindService()

        loadData()
    }

    override fun onPause() {

        super.onPause()
        try {
            requireContext().unregisterReceiver(updateReceiver)
        } catch (e: Exception) {}
        osmandHelper.unbindService()

    }

    fun setBboxFilter(bbox: DoubleArray?) {
        this.bboxFilter = bbox
        loadData()
    }


    fun loadData() {
        var locations = libraryManager.getLocations()

        // Check shared prefs first for cold start bbox filtering
        val sharedPrefs = requireActivity().getSharedPreferences("triangulation_prefs", android.content.Context.MODE_PRIVATE)
        val savedBboxStr = sharedPrefs.getString("bbox_filter", null)
        if (savedBboxStr != null) {
            val parts = savedBboxStr.split(",")
            if (parts.size == 4) {
                bboxFilter = doubleArrayOf(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())
                // clear it so it only applies once
                sharedPrefs.edit().remove("bbox_filter").apply()
            }
        }

        bboxFilter?.let { bbox ->
            val minLon = bbox[0]
            val minLat = bbox[1]
            val maxLon = bbox[2]
            val maxLat = bbox[3]

            locations = locations.filter {
                it.lon in minLon..maxLon && it.lat in minLat..maxLat
            }
        }

        adapter.updateData(locations)
    }
    override fun onOsmAndServiceConnected() {}
    override fun onOsmAndServiceDisconnected() {}
    override fun onContextMenuButtonClicked(buttonId: Int, pointId: String?, layerId: String?) {}
}
