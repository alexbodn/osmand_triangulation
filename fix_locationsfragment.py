import re

with open('app/app/src/main/java/com/example/triangulation/ui/LocationsFragment.kt', 'r') as f:
    content = f.read()

new_broadcast_receiver = """
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
"""

content = content.replace('    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {', new_broadcast_receiver)

new_register = """
        super.onResume()
        val filter = android.content.IntentFilter()
        filter.addAction("com.example.triangulation.BBOX_FILTER")
        filter.addAction("com.example.triangulation.REFRESH_LIBRARY")
        requireContext().registerReceiver(updateReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)

        osmandHelper.bindService()
"""
content = content.replace('        super.onResume()\n        osmandHelper.bindService()', new_register)

new_unregister = """
        super.onPause()
        try {
            requireContext().unregisterReceiver(updateReceiver)
        } catch (e: Exception) {}
        osmandHelper.unbindService()
"""
content = content.replace('        super.onPause()\n        osmandHelper.unbindService()', new_unregister)

with open('app/app/src/main/java/com/example/triangulation/ui/LocationsFragment.kt', 'w') as f:
    f.write(content)
