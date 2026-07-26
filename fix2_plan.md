Looking at the codebase...
`cbMagnetic.setOnCheckedChangeListener` DOES have my manual replacement code inside it (lines 198-214).
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
                            etAzimuth.setText(String.format("%.1f", currentVal))
```
Why does the user say it has no visible effect?
Ah!
If `cbManualAzimuth.isChecked` is false, it goes to the `else` block and calls `updateBackAzimuthDisplay()`. That works perfectly.
But if it is true, it replaces the text using `etAzimuth.setText()`.
Wait... `etAzimuth.addTextChangedListener` DOES NOTHING if manual mode is active, right?
Wait! `etAzimuth.addTextChangedListener` DOES something if `cbManualAzimuth.isChecked` is true!
```kotlin
                override fun afterTextChanged(s: Editable?) {
                    if (cbManualAzimuth.isChecked) {
                        val azimuthStr = s.toString()
                        if (azimuthStr.isNotEmpty()) {
                            try {
                                val azimuth = azimuthStr.toFloat()
                                if (azimuth in 0f..360f) {
                                    val backAzimuth = (azimuth + 180) % 360
                                    tvBackAzimuth.text = "${String.format("%.1f", backAzimuth)}°"
                                }
```
Ah! `calculateCurrentDeclination()` returns `0.0` if `currentLat` and `currentLon` are null, and if there are no `selectedLocations`!
When the user shares a location, `currentLat` is set. So declination should be > 0.

Wait. The user said: "the declination check / uncheck has no visible effect. when sharing a location with our app, and the azimuth is manual, the azimuth default is a previously edited value, not the magnetic sensor value."

Let's look at `handleIntent`:
```kotlin
                if (isManualDefault) {
                    etAzimuth.isEnabled = false
                    pendingInitialSensorUpdate = true
                } else {
                    etAzimuth.isEnabled = false
                    etAzimuth.text.clear()
                    etAzimuth.clearFocus()
                }
```
If `isManualDefault` is true, we lock the editor and set `pendingInitialSensorUpdate = true`. We DO NOT call `etAzimuth.text.clear()`.
So the text box STILL contains the previously edited value (e.g. from the previous location session).
Then `onSensorChanged` fires. It hits `if (pendingInitialSensorUpdate)`.
```kotlin
            if (pendingInitialSensorUpdate) {
                pendingInitialSensorUpdate = false
                updateBackAzimuthDisplay(true)
                etAzimuth.isEnabled = true
            }
```
It calls `updateBackAzimuthDisplay(true)`.
```kotlin
        if (!cbManualAzimuth.isChecked || forceUpdateEditText) {
            etAzimuth.setText(String.format("%.1f", azimuthToDisplay))
```
Wait, this SHOULD overwrite the old text with `azimuthToDisplay` (which is `baseAzimuth` + declination).
Why doesn't it?
Ah! `updateBackAzimuthDisplay` formats to `%.1f`. If the old text was already that value, it doesn't visually change, but `baseAzimuth` is the LIVE sensor value.
Wait. Is `baseAzimuth` correct?
Yes, `baseAzimuth` is continuously updated from the sensor: `baseAzimuth = azimuthInDegrees`.
BUT! When the user manually typed in a value previously, did it change `baseAzimuth`? NO! I removed the `TextWatcher` that overrides `baseAzimuth`.
So `baseAzimuth` is literally always the live compass.
So `updateBackAzimuthDisplay(true)` should inject the LIVE compass value!
Why did the user say "the azimuth default is a previously edited value"?

Ah, wait! Look at the `onNewIntent` method:
```kotlin
    fun onNewIntent(intent: Intent?) {
        println(intent)
        handleIntent(requireActivity().intent)
    }
```
`onNewIntent` calls `handleIntent(requireActivity().intent)` instead of `handleIntent(intent)`!
Wait, `requireActivity().intent` is the INITIAL intent that launched the activity! NOT the new intent!
So `handleIntent` is parsing the OLD intent all over again? No, Android might update `activity.intent`, but usually you have to call `setIntent(intent)` in the Activity. The fragment just grabs `requireActivity().intent`.
But wait! If the user clicks an item in the list (`tvAzimuth.setOnClickListener`), it sets:
```kotlin
                etAzimuth.setText(String.format("%.1f", displayAzimuth))
```
And then later, if they share a new intent to the app while it's already open...
Let's see where the intent comes from. In `MainActivity.kt`:
```kotlin
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent) // <--- Oh, it does setIntent(intent)
        // pass to fragment
    }
```
Wait, when the user shares a location, does `cbManualAzimuth.setOnCheckedChangeListener` fire because we change it programmatically?
Yes!
```kotlin
                cbManualAzimuth.tag = "suppress_save"
                cbManualAzimuth.isChecked = isManualDefault
                cbManualAzimuth.tag = null
```
When `cbManualAzimuth.isChecked = isManualDefault` is called, the listener fires.
```kotlin
            cbManualAzimuth.setOnCheckedChangeListener { _, isChecked ->
                // Check if we are intentionally suppressing the save (e.g. from intent)
                if (cbManualAzimuth.tag != "suppress_save") {
                    // ... save prefs
                }

                etAzimuth.isEnabled = isChecked
                if (!isChecked) {
                    etAzimuth.clearFocus()
                    // ... hide keyboard
                }
            }
```
This listener does NOT clear the text.
If `isManualDefault` was ALREADY true, `cbManualAzimuth.isChecked = isManualDefault` DOES NOTHING (the listener doesn't even fire, state doesn't change).
So `pendingInitialSensorUpdate = true` is set.
Then `onSensorChanged` fires. It calls `updateBackAzimuthDisplay(true)`.
```kotlin
    private fun updateBackAzimuthDisplay(forceUpdateEditText: Boolean = false) {
        var azimuthToDisplay = baseAzimuth
        // ... applies declination
        if (!cbManualAzimuth.isChecked || forceUpdateEditText) {
            etAzimuth.setText(String.format("%.1f", azimuthToDisplay))
```
Wait, if `onSensorChanged` runs, it SHOULD overwrite `etAzimuth.setText(azimuthToDisplay)`.
Why doesn't it?
Is `onSensorChanged` NOT firing?
If the device is flat on the table, the rotation vector sensor might not fire a new event immediately.
But when the user says "the declination check / uncheck has no visible effect," what do they mean?
Maybe they mean the *declination value itself* (`calculateCurrentDeclination()`) is 0.0 because it's not being calculated right?
Wait. If `currentLat` and `currentLon` are set by the intent...
In `handleIntent`:
```kotlin
            if (parts.size >= 2) {
                val lat = parts[0].toDoubleOrNull()
                val lon = parts[1].toDoubleOrNull()
                if (lat != null && lon != null) {
                    currentLat = lat
                    currentLon = lon
                    locationParsed = true
                }
            }
```
Then we call:
```kotlin
        if (locationParsed) {
            // ...
            if (isManualDefault) {
                etAzimuth.isEnabled = false
                pendingInitialSensorUpdate = true
            } else { ... }
        }
```
Then `onSensorChanged` fires, calls `updateBackAzimuthDisplay(true)`.
`updateBackAzimuthDisplay` calls `calculateCurrentDeclination()`.
`calculateCurrentDeclination()` uses `currentLat` and `currentLon` (which are set). So declination is valid.
Then it sets `etAzimuth.setText`.
Wait. If `etAzimuth.setText` is called, it SHOULD update!
What if `pendingInitialSensorUpdate` is set to `true`, but `onSensorChanged` NEVER fires because the app was just launched and the sensor listener isn't registered yet?
Wait, `onResume` registers the sensor listener.
`handleIntent` is called in `onViewCreated` AND `onNewIntent`. Both happen on the main thread.
`onSensorChanged` fires shortly after.

What if they mean:
"When sharing a location, if `isManualDefault` is TRUE, the text box stays as the OLD edited value, AND checking/unchecking declination does nothing!"
Why would checking/unchecking declination do nothing?
Let's look at the `cbMagnetic.setOnCheckedChangeListener` logic I wrote:
```kotlin
            cbMagnetic.setOnCheckedChangeListener { _, isChecked ->
                val sharedPrefs = requireActivity().getSharedPreferences("triangulation_prefs", Context.MODE_PRIVATE)
                sharedPrefs.edit().putBoolean("isMagneticChecked", isChecked).apply()

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
Wait! If `calculateCurrentDeclination()` returns `0.0`, then `currentVal += 0.0` has NO VISIBLE EFFECT!
Why would `calculateCurrentDeclination()` return `0.0`?
```kotlin
    private fun calculateCurrentDeclination(): Float {
        var declinationTargetLat = 0.0
        var declinationTargetLon = 0.0

        if (currentLat != null && currentLon != null) {
            declinationTargetLat = currentLat!!
            declinationTargetLon = currentLon!!
        } else if (selectedLocations.size >= 2) {
            // ...
```
Wait. Look at the `ivArrow` click listener:
```kotlin
                            // Consume the location parameters so they are used only once
                            currentLat = null
                            currentLon = null
                            rawReceivedParameter = null
```
Once you SAVE an observation, `currentLat` and `currentLon` are SET TO NULL!
If you then check/uncheck the declination box, `calculateCurrentDeclination()` will see `currentLat == null`.
If `selectedLocations.size < 2`, it falls through to:
```kotlin
        } else if (selectedLocations.size == 1) {
            declinationTargetLat = selectedLocations[0].lat
            declinationTargetLon = selectedLocations[0].lon
        } else if (libraryManager.getLibraryLocations().isNotEmpty()) {
            val firstLoc = libraryManager.getLibraryLocations()[0]
            declinationTargetLat = firstLoc.lat
            declinationTargetLon = firstLoc.lon
        }

        if (declinationTargetLat == 0.0 && declinationTargetLon == 0.0) {
            return 0f
        }
```
Wait, if you have 1 location, it uses that location. If you have 0 locations, it uses the library. If the library is empty, it returns `0.0`!
BUT if `currentLat` is SET (because you just shared a location), it should return a non-zero declination!

Let's test my theory about `pendingInitialSensorUpdate`.
What if `pendingInitialSensorUpdate` is set to `true`, but the sensor event fires BEFORE `pendingInitialSensorUpdate` is set to true? No, they are on the same thread, UI thread.
What if `updateBackAzimuthDisplay(true)` is called, but `etAzimuth.text.toString()` is NOT empty, so when `cbMagnetic` is toggled, it adds/subtracts declination correctly... wait, no.

Wait! The user says "when sharing a location with our app, and the azimuth is manual, the azimuth default is a previously edited value, not the magnetic sensor value."
Why would the previously edited value still be there?
Because `etAzimuth.setText()` is NOT being called!
Why?
Let's look at `handleIntent`:
```kotlin
            val azimuthExtra = intent?.getFloatExtra("azimuth", Float.NaN)
            if (azimuthExtra != null && !azimuthExtra.isNaN()) {
                // ...
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
What if `cbManualAzimuth.isChecked = isManualDefault` triggers `updateBackAzimuthDisplay()`?
NO, `cbManualAzimuth.setOnCheckedChangeListener` DOES NOT call `updateBackAzimuthDisplay()`!
But wait, look at `onSensorChanged`:
```kotlin
            // Handle deferred UI update for new intents with manual mode defaulted ON
            if (pendingInitialSensorUpdate) {
                pendingInitialSensorUpdate = false
                updateBackAzimuthDisplay(true)
                etAzimuth.isEnabled = true
            }
```
Wait! `updateBackAzimuthDisplay(true)` uses `baseAzimuth`!
Is `baseAzimuth` updated from the sensor? YES, `baseAzimuth = azimuthInDegrees` happens RIGHT BEFORE this check!
```kotlin
            // Always track the underlying hardware compass
            baseAzimuth = azimuthInDegrees

            // Handle deferred UI update for new intents with manual mode defaulted ON
            if (pendingInitialSensorUpdate) { ... }
```
So `updateBackAzimuthDisplay(true)` will DEFINITELY use the live compass value, format it, and put it in `etAzimuth`.
Why is the user saying it's the "previously edited value"?
Is it possible `onSensorChanged` is simply NOT FIRING because the phone is perfectly still, so the rotation vector doesn't change?
If the sensor doesn't fire a change event, `pendingInitialSensorUpdate` stays true forever, and `etAzimuth` stays disabled, containing whatever was in it before!
Wait, they said "the azimuth default is a previously edited value", which means they can see it, and it's probably enabled?
If it's enabled, then `onSensorChanged` MUST have fired!
If `onSensorChanged` fired, it called `updateBackAzimuthDisplay(true)`.
Why didn't `etAzimuth.setText()` overwrite the text?
Ah! Because I am an idiot!
In `updateBackAzimuthDisplay(true)`, I removed the `TextWatcher` bypass hack!
```kotlin
        if (!cbManualAzimuth.isChecked || forceUpdateEditText) {
            etAzimuth.setText(String.format("%.1f", azimuthToDisplay))
            // ...
```
Does `etAzimuth.setText()` work? YES. It completely replaces the text.
But wait! If the user clicks the "Edit" button on a list item:
```kotlin
            tvAzimuth.setOnClickListener {
                currentLat = reading.lat
                currentLon = reading.lon
                // ...
                etAzimuth.setText(...)
```
And then they share a new intent:
`handleIntent` runs. `pendingInitialSensorUpdate = true`.
`onSensorChanged` runs. `updateBackAzimuthDisplay(true)` runs. `etAzimuth.setText(liveCompass)` runs.

Wait... could `pendingInitialSensorUpdate` be set to false somewhere else? No.

Wait. What if they DO NOT share an intent from outside the app?
What if they click "Import"?
No, they said "when sharing a location with our app".
What if the intent has `azimuthExtra`?
If you share a location from OsmAnd, there is no azimuth.
