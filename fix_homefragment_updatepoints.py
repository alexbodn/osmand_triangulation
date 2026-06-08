import re

with open('/tmp/HomeFragment.kt', 'r') as f:
    content = f.read()

# Add libraryManager instance variable
content = content.replace('private var isUserEditing = false', 'private var isUserEditing = false\n    private lateinit var libraryManager: com.example.triangulation.data.LocationLibraryManager')

# Initialize libraryManager in onViewCreated
content = content.replace('loadState()', 'libraryManager = com.example.triangulation.data.LocationLibraryManager(requireContext())\n            loadState()')


# Update updatePointsList
new_update_points_list = """
    private fun updatePointsList() {
        btnIntersection.isEnabled = selectedLocations.size >= 2
        tvListHeader.text = "Saved Points (${selectedLocations.size})"
        llPointsContainer.removeAllViews()

        for (i in selectedLocations.indices) {
            val reading = selectedLocations[i]
            val itemView = layoutInflater.inflate(R.layout.item_point, llPointsContainer, false)

            val tvAzimuth = itemView.findViewById<TextView>(R.id.tvPointAzimuth)
            val tvDesc = itemView.findViewById<TextView>(R.id.tvPointDesc)
            val btnSave = itemView.findViewById<Button>(R.id.btnSave)
            val btnView = itemView.findViewById<Button>(R.id.btnView)
            val btnDelete = itemView.findViewById<Button>(R.id.btnDelete)

            tvAzimuth.text = "${String.format("%.1f", reading.azimuth)}°"

            val libraryLoc = libraryManager.isLocationInLibrary(reading.lat, reading.lon)
            if (libraryLoc != null) {
                btnSave.visibility = View.GONE
                tvDesc.visibility = View.VISIBLE
                tvDesc.text = libraryLoc.desc ?: ""
            } else {
                btnSave.visibility = View.VISIBLE
                tvDesc.visibility = View.GONE
            }

            btnSave.setOnClickListener {
                showSaveDialog(reading.lat, reading.lon)
            }

            btnView.setOnClickListener {
                val launchIntent = requireActivity().packageManager.getLaunchIntentForPackage("net.osmand.plus")
                    ?: requireActivity().packageManager.getLaunchIntentForPackage("net.osmand")
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(launchIntent)
                }

                Thread {
                    Thread.sleep(300)
                    if (!osmandHelper.setMapLocation(reading.lat, reading.lon, 15)) {
                        requireActivity().runOnUiThread { Toast.makeText(requireContext(), "Failed to set OsmAnd location", Toast.LENGTH_SHORT).show() }
                    }
                }.start()
            }

            btnDelete.setOnClickListener {
                selectedLocations.removeAt(i)
                saveState()
                updatePointsList()
                Thread {
                    drawTriangulationPointsOnMap()
                }.start()
                Toast.makeText(requireContext(), "Point removed from active list", Toast.LENGTH_SHORT).show()
            }

            llPointsContainer.addView(itemView)
        }
    }

    private fun showSaveDialog(lat: Double, lon: Double) {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Save Location to Library")

        val view = layoutInflater.inflate(R.layout.dialog_save_location, null)
        val etDesc = view.findViewById<EditText>(R.id.etDialogDesc)
        val etImgUrl = view.findViewById<EditText>(R.id.etDialogImgUrl)

        builder.setView(view)
        builder.setPositiveButton("Save") { dialog, _ ->
            val desc = etDesc.text.toString().takeIf { it.isNotBlank() }
            val img = etImgUrl.text.toString().takeIf { it.isNotBlank() }

            val newLoc = com.example.triangulation.data.LibraryLocation(lat, lon, desc, img)
            libraryManager.addLocation(newLoc)

            updatePointsList() // Refresh the active list to show desc instead of save button
            Toast.makeText(requireContext(), "Location saved to library", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
"""

content = re.sub(r'    private fun updatePointsList\(\) \{.*?(?=    private fun calculateCenterOfGravity)', new_update_points_list, content, flags=re.DOTALL)

with open('/tmp/HomeFragment.kt', 'w') as f:
    f.write(content)
