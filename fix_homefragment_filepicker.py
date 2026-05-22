import re

with open('/tmp/HomeFragment.kt', 'r') as f:
    content = f.read()

new_show_save_dialog = """
    private var pendingImgUrlView: EditText? = null

    private val filePickerLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                    pendingImgUrlView?.setText("data:image/jpeg;base64," + base64)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Failed to read image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSaveDialog(lat: Double, lon: Double) {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Save Location to Library")

        val view = layoutInflater.inflate(R.layout.dialog_save_location, null)
        val etDesc = view.findViewById<EditText>(R.id.etDialogDesc)
        val etImgUrl = view.findViewById<EditText>(R.id.etDialogImgUrl)
        val btnChooseFile = view.findViewById<Button>(R.id.btnChooseFile)

        pendingImgUrlView = etImgUrl

        btnChooseFile.setOnClickListener {
            filePickerLauncher.launch("image/*")
        }

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

content = re.sub(r'    private fun showSaveDialog\([^)]+\) \{.*?(?=\n    private fun calculateCenterOfGravity)', new_show_save_dialog, content, flags=re.DOTALL)

with open('/tmp/HomeFragment.kt', 'w') as f:
    f.write(content)
