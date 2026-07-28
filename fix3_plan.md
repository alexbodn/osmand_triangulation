Oh! Wait, wait, wait!
"when sharing a location with our app, and the azimuth is manual, the azimuth default is a previously edited value, not the magnetic sensor value."

Wait. When sharing a location, `handleIntent` is called.
```kotlin
            } else {
                // When sharing a NEW location with the app, ensure we start taking live sensor data immediately
                val sharedPrefs = requireActivity().getSharedPreferences("triangulation_prefs", Context.MODE_PRIVATE)
                val isManualDefault = sharedPrefs.getBoolean("isManualAzimuthChecked", false)

                cbManualAzimuth.tag = "suppress_save"
                cbManualAzimuth.isChecked = isManualDefault
                cbManualAzimuth.tag = null

                if (isManualDefault) {
                    etAzimuth.isEnabled = false
                    pendingInitialSensorUpdate = true
                } else {
                    etAzimuth.isEnabled = false
                    etAzimuth.text.clear()
                    etAzimuth.clearFocus()
                }
            }
```
If `isManualDefault` is true, we set `pendingInitialSensorUpdate = true`.
BUT WE DON'T CLEAR THE TEXT!
If `onSensorChanged` DOES NOT FIRE IMMEDIATELY (because the phone is perfectly still, which is very common during development and testing on emulators or flat on a desk), the text box STILL CONTAINS THE PREVIOUS VALUE, and it's disabled!
Wait, if it's disabled, the user would notice it's disabled. Did they say it's disabled?
"the azimuth default is a previously edited value".
What if we DO clear the text when setting `pendingInitialSensorUpdate = true`?
```kotlin
                if (isManualDefault) {
                    etAzimuth.text.clear()
                    etAzimuth.isEnabled = false
                    pendingInitialSensorUpdate = true
                }
```
Wait! If the user says "the azimuth default is a previously edited value," they might mean the app opens, they see the old value, and they CAN edit it? No, if it's disabled they can't.
What if `onSensorChanged` DOES fire, but `updateBackAzimuthDisplay(true)` doesn't actually update it?
Let's check `updateBackAzimuthDisplay`:
```kotlin
    private fun updateBackAzimuthDisplay(forceUpdateEditText: Boolean = false) {
        var azimuthToDisplay = baseAzimuth

        val declination = calculateCurrentDeclination()
        tvDeclination.text = "Magnetic declination: ${String.format("%.1f", declination)}°"

        if (cbMagnetic.isChecked) {
            azimuthToDisplay += declination
            if (azimuthToDisplay >= 360f) azimuthToDisplay -= 360f
            if (azimuthToDisplay < 0f) azimuthToDisplay += 360f
        }

        if (!cbManualAzimuth.isChecked || forceUpdateEditText) {
            etAzimuth.setText(String.format("%.1f", azimuthToDisplay))
            val backAzimuth = (azimuthToDisplay + 180) % 360
            tvBackAzimuth.text = "${String.format("%.1f", backAzimuth)}°"
        }
    }
```
If `forceUpdateEditText` is true, it DOES update it.

Wait, what if `onSensorChanged` NEVER fired? If I clear the text, the box will be blank. That's better than showing a false previous value. But why isn't it taking the LIVE compass value if the phone is moving?
Oh! When the fragment is created or the app is resumed, `baseAzimuth` might be initialized to `0.0f`!
Wait, in `HomeFragment`, `baseAzimuth` is `private var baseAzimuth = 0f`.
If `onSensorChanged` hasn't fired yet, `baseAzimuth` is `0f`.
If the user checks the checkbox, `cbMagnetic.setOnCheckedChangeListener` triggers:
```kotlin
                if (cbManualAzimuth.isChecked) {
                    try {
                        val currentText = etAzimuth.text.toString()
                        if (currentText.isNotEmpty()) {
                            var currentVal = currentText.toFloat()
                            val declination = calculateCurrentDeclination()
                            if (isChecked) {
                                currentVal += declination
                            } else {
                                currentVal -= declination
                            }
                            // ...
```
Wait, if they check the checkbox and it adds declination, it WILL add declination.
BUT they said: "the declination check / uncheck has no visible effect."
WHY would it have no visible effect?
Because `etAzimuth.setText` is NOT visually changing?
If they check it, `isChecked` is true.
`currentVal += declination`
If `declination` is 0.0, it has no effect.
When would `declination` be 0.0?
If `currentLat` and `currentLon` are null, and there are no points.
But they JUST SHARED A LOCATION. So `currentLat` and `currentLon` ARE NOT NULL.
Let's look at `calculateCurrentDeclination()`:
```kotlin
    private fun calculateCurrentDeclination(): Float {
        var declinationTargetLat = 0.0
        var declinationTargetLon = 0.0

        if (currentLat != null && currentLon != null) {
            declinationTargetLat = currentLat!!
            declinationTargetLon = currentLon!!
        }
        // ...
        if (declinationTargetLat == 0.0 && declinationTargetLon == 0.0) {
            return 0f
        }
        try {
            val geomagneticField = GeomagneticField(
                declinationTargetLat.toFloat(),
                declinationTargetLon.toFloat(),
                0f,
                System.currentTimeMillis()
            )
            return geomagneticField.declination
        } catch (e: Exception) {
            return 0f
        }
    }
```
If `declination` is not 0.0, then `currentVal` changes.
Why would they say it has no visible effect?
Let's check if the `setOnCheckedChangeListener` is actually running the code!
Did I merge my patch into the file?
Let's check the file contents of `HomeFragment.kt`.
