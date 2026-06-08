import re

with open('/tmp/HomeFragment.kt', 'r') as f:
    content = f.read()

# Fix application reference
content = content.replace('OsmAndAidlHelper(application, requireContext())', 'OsmAndAidlHelper(requireActivity().application, this)')

# Fix sensor listener type error
content = content.replace('sensorManager.registerListener(requireContext(), it, SensorManager.SENSOR_DELAY_UI)', 'sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)')
content = content.replace('sensorManager.unregisterListener(requireContext())', 'sensorManager.unregisterListener(this)')

# Fix multiple return statements syntax error
content = content.replace('if (isPluginDialogShowing) return@().runOnUiThread', 'if (isPluginDialogShowing) return@runOnUiThread')
content = content.replace('if (isInstallDialogShowing) return@().runOnUiThread', 'if (isInstallDialogShowing) return@runOnUiThread')

# Fix var vs val and requireActivity().intent
content = content.replace('val requireActivity().intent = Intent(requireContext(), MainActivity::class.java)', 'val launchIntent = Intent(requireContext(), com.example.triangulation.MainActivity::class.java)')
content = content.replace('requireActivity().intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP', 'launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP')
content = content.replace('requireActivity().intent.putExtra("lat", lat)', 'launchIntent.putExtra("lat", lat)')
content = content.replace('requireActivity().intent.putExtra("lon", lon)', 'launchIntent.putExtra("lon", lon)')
content = content.replace('requireActivity().intent.putExtra("pointId", pointId)', 'launchIntent.putExtra("pointId", pointId)')
content = content.replace('startActivity(requireActivity().intent)', 'startActivity(launchIntent)')

content = content.replace('fun onNewIntent(requireActivity().intent: Intent?) {', 'fun onNewIntent(intent: Intent?) {')
content = content.replace('println(requireActivity().intent)', 'println(intent)')
content = content.replace('handleIntent(requireActivity().intent)', 'handleIntent(intent)')

content = content.replace('private fun handleIntent(requireActivity().intent: Intent?) {', 'private fun handleIntent(intent: Intent?) {')
content = content.replace('val latExtra = requireActivity().intent?.getDoubleExtra("lat", Double.NaN) ?: Double.NaN', 'val latExtra = intent?.getDoubleExtra("lat", Double.NaN) ?: Double.NaN')
content = content.replace('val lonExtra = requireActivity().intent?.getDoubleExtra("lon", Double.NaN) ?: Double.NaN', 'val lonExtra = intent?.getDoubleExtra("lon", Double.NaN) ?: Double.NaN')
content = content.replace('intent?.data?.let', 'intent?.data?.let')
content = content.replace('intent?.action == Intent.ACTION_SEND', 'intent?.action == Intent.ACTION_SEND')
content = content.replace('intent.type == "text/plain"', 'intent?.type == "text/plain"')
content = content.replace('val sharedText = requireActivity().intent.getStringExtra(Intent.EXTRA_TEXT)', 'val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)')
content = content.replace('requireActivity().intent?.removeExtra', 'intent?.removeExtra')
content = content.replace('requireActivity().intent?.data = null', 'intent?.data = null')
content = content.replace('requireActivity().intent?.action = null', 'intent?.action = null')
content = content.replace('requireActivity().intent =(Intent())', 'requireActivity().intent = Intent()')

# replace the rest of requireActivity().intent that were not caught
content = content.replace('requireActivity().intent?', 'intent?')
content = content.replace('requireActivity().intent.', 'intent.')
content = content.replace('requireActivity().intent ', 'intent ')

with open('/tmp/HomeFragment.kt', 'w') as f:
    f.write(content)
