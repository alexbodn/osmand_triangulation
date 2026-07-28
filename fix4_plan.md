I merged my code correctly.
Why does the user say it has no visible effect?
Wait! In `HomeFragment.kt`, `loadState()` is called in `onViewCreated`:
```kotlin
            libraryManager = com.example.triangulation.data.LocationLibraryManager(requireContext())
            loadState()
            handleIntent(requireActivity().intent)
```
What does `loadState()` do?
```kotlin
    private fun loadState() {
        val sharedPrefs = requireActivity().getSharedPreferences("triangulation_prefs", Context.MODE_PRIVATE)

        val isMagneticChecked = sharedPrefs.getBoolean("isMagneticChecked", false)
        cbMagnetic.isChecked = isMagneticChecked

        val isManualAzimuthChecked = sharedPrefs.getBoolean("isManualAzimuthChecked", false)
        cbManualAzimuth.isChecked = isManualAzimuthChecked
        // ...
```
When `cbMagnetic.isChecked = isMagneticChecked` is called, the `setOnCheckedChangeListener` FIRES.
But wait, what if the listener was already set BEFORE `loadState()` is called?
In `onViewCreated`, the listeners are set:
```kotlin
            cbMagnetic.setOnCheckedChangeListener { ... }
            cbManualAzimuth.setOnCheckedChangeListener { ... }
            // ...
            loadState()
```
Yes! The listeners are set BEFORE `loadState()`!
So when `loadState()` sets `cbMagnetic.isChecked = true`, the listener fires!
If `cbManualAzimuth` is ALREADY true (maybe it was saved as true?), wait, `cbManualAzimuth` is set right AFTER `cbMagnetic` in `loadState`. So when `cbMagnetic` fires, `cbManualAzimuth` might be false, so it calls `updateBackAzimuthDisplay()`.
This is fine.

But wait. "the declination check / uncheck has no visible effect."
What if they click the checkbox in the UI, and the text box does NOT change?
If `cbManualAzimuth.isChecked` is true, the `try` block executes.
```kotlin
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
```
If this executes, `etAzimuth.setText(String.format("%.1f", currentVal))` IS CALLED.
Why would it have no visible effect?
Because `updateBackAzimuthDisplay()` is NOT called? Wait, `updateBackAzimuthDisplay()` updates `tvBackAzimuth` and `tvDeclination` (the text that says "Magnetic declination: X°").
If I don't call `updateBackAzimuthDisplay()`, the "Magnetic declination: X°" text DOES NOT UPDATE if the declination changed? (Wait, declination doesn't change when you toggle the checkbox, it just gets applied/unapplied).
But if I don't call `updateBackAzimuthDisplay()`, maybe `tvDeclination` never gets initialized if manual mode was on from the start?!
YES!
If `cbManualAzimuth` is checked, I bypass `updateBackAzimuthDisplay()`, which means `tvDeclination.text = "Magnetic declination: ..."` NEVER runs when toggling the checkbox!
Wait, but toggling the checkbox doesn't change the declination value, it just changes `azimuthToDisplay`. But does it mean the user thinks the checkbox has no effect because the text box isn't changing?
Why wouldn't the text box change?
Ah. What if `currentText` is an integer like "180"? `toFloat()` parses it to `180.0`.
If `declination` is `0.0`, it stays `180.0`.
Why would `declination` be `0.0`?
If they tap "Share" from OsmAnd, they share a `geo:lat,lon` intent.
The app opens. `handleIntent` parses `currentLat` and `currentLon`.
Then `handleIntent` calls `updateBackAzimuthDisplay(true)`. (Wait, I added that in my review fix!)
Let's look at `handleIntent`:
```kotlin
        if (locationParsed) {
            // ...
            if (isManualDefault) {
                etAzimuth.isEnabled = false
                pendingInitialSensorUpdate = true
            } else {
                etAzimuth.isEnabled = false
                etAzimuth.text.clear()
                etAzimuth.clearFocus()
            }
            // I ADDED THIS EARLIER? Wait, I didn't add it in the final patch.
```

Wait, if they share a location, the intent is processed.
`isManualDefault` is TRUE.
`etAzimuth.isEnabled = false`.
`pendingInitialSensorUpdate = true`.
NO text is cleared.
Then `onSensorChanged` fires, and hits:
```kotlin
            // Handle deferred UI update for new intents with manual mode defaulted ON
            if (pendingInitialSensorUpdate) {
                pendingInitialSensorUpdate = false
                updateBackAzimuthDisplay(true)
                etAzimuth.isEnabled = true
            }
```
If `updateBackAzimuthDisplay(true)` runs, it DOES:
```kotlin
        if (!cbManualAzimuth.isChecked || forceUpdateEditText) {
            etAzimuth.setText(String.format("%.1f", azimuthToDisplay))
```
So it DOES update the text to `azimuthToDisplay` (which is `baseAzimuth` + declination).
Wait, if it does that, why does the user say "the azimuth default is a previously edited value"?
Maybe `onSensorChanged` DOES NOT FIRE!
If they are testing on an emulator, or the phone is perfectly flat on a desk, `onSensorChanged` (Rotation Vector) might not emit a new event until the phone is physically moved!
If it doesn't emit an event, `pendingInitialSensorUpdate` stays true, the editor remains DISABLED, and the OLD TEXT is still in the box!
Wait. If the editor is DISABLED, how can they type in it?
Maybe they tap the checkbox while it's disabled?
If they tap the checkbox, `cbMagnetic.setOnCheckedChangeListener` fires!
```kotlin
                if (cbManualAzimuth.isChecked) {
                    try {
                        val currentText = etAzimuth.text.toString()
```
It reads the OLD TEXT, adds declination, and puts it back. So the text DOES change!
Why did they say "the declination check / uncheck has no visible effect"?
Maybe `cbMagnetic.setOnCheckedChangeListener` is somehow not working?

Let's rethink:
"when sharing a location with our app, and the azimuth is manual, the azimuth default is a previously edited value, not the magnetic sensor value."
If `onSensorChanged` doesn't fire, it's a previously edited value.
If I CLEAR the text immediately:
```kotlin
                if (isManualDefault) {
                    etAzimuth.text.clear()
                    etAzimuth.isEnabled = false
                    pendingInitialSensorUpdate = true
                }
```
Then the box is blank. The user will see a blank box until they move the phone. That's a strong hint to move the phone. But better yet, I should just force an immediate update using the LAST known `baseAzimuth`!
```kotlin
                if (isManualDefault) {
                    etAzimuth.isEnabled = true
                    updateBackAzimuthDisplay(true)
                }
```
Why wait for `onSensorChanged`? `baseAzimuth` still holds the last known rotation (or 0.0 if freshly launched). If freshly launched, it'll say 0.0. When they pick up the phone, it will NOT update because manual mode is ON. But wait, if they want the *live* magnetic sensor value, they shouldn't use manual mode! If manual mode is defaulted ON, they WANT it to be locked to a specific value.
Wait, my previous logic: "When sharing a NEW location with the app, ensure we start taking live sensor data immediately... lock editor initially until the sensor catches up".
If the sensor hasn't caught up, `baseAzimuth` is 0.0.
If we force `updateBackAzimuthDisplay(true)` immediately, it will lock in `0.0`.
If we wait for `onSensorChanged` with `pendingInitialSensorUpdate`, it will wait forever if the phone is still.
To fix this: `pendingInitialSensorUpdate` is okay, but I MUST clear the text so they don't see the previous value.
And if they toggle the declination checkbox while `pendingInitialSensorUpdate` is true (text is blank), it will parse `""`, fail silently in the `catch`, and have NO VISIBLE EFFECT!
Ah! If the text is blank, parsing fails, and it has no visible effect!
Or if the text is the old value, and they click it, it DOES have an effect. But they said it doesn't.

Wait! What if they are talking about the LIST ITEM?
"when sharing a location with our app..." -> That's the intent.
What if they meant: "When I have a shared location active, and the checkbox is checked, the text box doesn't change when I toggle it?"
Wait. Look at `updateBackAzimuthDisplay(true)`:
```kotlin
        if (cbMagnetic.isChecked) {
            azimuthToDisplay += declination
```
If I toggle the checkbox, it changes `azimuthToDisplay`.
BUT IN MANUAL MODE, `updateBackAzimuthDisplay` IS NOT CALLED BY THE CHECKBOX LISTENER!
In manual mode, the checkbox listener manually does:
```kotlin
                            if (isChecked) {
                                currentVal += declination
                            } else {
                                currentVal -= declination
                            }
                            etAzimuth.setText(...)
```
Wait. What if `calculateCurrentDeclination()` throws an exception?
```kotlin
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
```
It returns `0f`. If it returns `0f`, `currentVal` doesn't change.
Why would it throw? Or why would it be `0f`?
If `declinationTargetLat == 0.0 && declinationTargetLon == 0.0`, it returns `0f`.
Is there a bug in `calculateCurrentDeclination()`?
No.

What if they mean: I tap the list item to edit it. The text box populates. I uncheck the checkbox. NOTHING HAPPENS!
Wait, if I tap the list item, `cbManualAzimuth.isChecked = true`.
So manual mode is ON.
If I uncheck the checkbox, the listener fires:
```kotlin
                if (cbManualAzimuth.isChecked) {
                    try {
                        val currentText = etAzimuth.text.toString()
```
It reads the text. It adds/subtracts declination. It calls `etAzimuth.setText`.
Why wouldn't it have a visible effect?
Ah! Because of `etAzimuth.addTextChangedListener`? No, I removed the `TextWatcher` logic that changes `baseAzimuth`. The new `TextWatcher` only updates `tvBackAzimuth`:
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
That works perfectly.

Wait! Look at `cbMagnetic.setOnCheckedChangeListener`:
```kotlin
                            if (currentVal >= 360f) currentVal -= 360f
                            if (currentVal < 0f) currentVal += 360f

                            etAzimuth.setText(String.format("%.1f", currentVal))
```
Does `etAzimuth.setText()` cause `tvDeclination` to update? NO!
Because `updateBackAzimuthDisplay()` is not called!
If `tvDeclination` doesn't update, the user might say "the declination check / uncheck has no visible effect" because the "Magnetic declination: X°" label doesn't appear or update?
No, the label says "Magnetic declination: X°", it doesn't change when you check/uncheck the box! The text in the edit box should change!
Wait! "the declination check / uncheck has no visible effect."
What if they are NOT in manual mode?
If they are NOT in manual mode, `cbMagnetic` listener calls `updateBackAzimuthDisplay()`.
```kotlin
    private fun updateBackAzimuthDisplay(forceUpdateEditText: Boolean = false) {
        // ...
        if (!cbManualAzimuth.isChecked || forceUpdateEditText) {
            etAzimuth.setText(String.format("%.1f", azimuthToDisplay))
```
It calls `etAzimuth.setText`.
If they are NOT in manual mode, the compass is constantly updating `etAzimuth` via `onSensorChanged` anyway.
So checking/unchecking the box immediately changes the value by ~declination.

Wait. Let me re-read the user's second sentence:
"when sharing a location with our app, and the azimuth is manual, the azimuth default is a previously edited value, not the magnetic sensor value."

If the app was backgrounded, `baseAzimuth` remembers the last rotation vector (or 0 if killed).
If they share a location, the intent opens the app.
`handleIntent` runs.
```kotlin
            } else {
                // When sharing a NEW location with the app, ensure we start taking live sensor data immediately
                val sharedPrefs = requireActivity().getSharedPreferences("triangulation_prefs", Context.MODE_PRIVATE)
                val isManualDefault = sharedPrefs.getBoolean("isManualAzimuthChecked", false)
                // ...
                if (isManualDefault) {
                    etAzimuth.isEnabled = false
                    pendingInitialSensorUpdate = true
                }
```
If `isManualDefault` is TRUE, we set `pendingInitialSensorUpdate = true`.
The text box is NOT cleared. It holds the previously edited value from the last session!
Then `onSensorChanged` fires (if the sensor is active).
```kotlin
            // Handle deferred UI update for new intents with manual mode defaulted ON
            if (pendingInitialSensorUpdate) {
                pendingInitialSensorUpdate = false
                updateBackAzimuthDisplay(true)
                etAzimuth.isEnabled = true
            }
```
If `updateBackAzimuthDisplay(true)` fires, it DOES overwrite the text box!
But what if the user has `cbManualAzimuth` checked, and they share a location...
Ah! If they share a location, `handleIntent` is called in `onNewIntent` AND in `onViewCreated`!
In `onViewCreated`:
```kotlin
            loadState()
            handleIntent(requireActivity().intent)
```
If the app was completely killed, `requireActivity().intent` is the shared location.
`baseAzimuth` is 0.0.
`pendingInitialSensorUpdate` is set to true.
`onResume` registers the sensor.
The sensor fires. `azimuthInDegrees` might be 0.0 initially, or a real value.
It calls `updateBackAzimuthDisplay(true)`.
The text box is populated with the sensor value.
Why would it be a "previously edited value"?
Because `etAzimuth.setText` is somehow restoring state?
Android automatically saves and restores the state of `EditText` views across configuration changes and process deaths!
When the fragment is recreated, Android restores the `text` of `etAzimuth` to whatever the user typed before the app was killed!
If `onSensorChanged` DOES NOT fire (e.g. phone is perfectly still), `pendingInitialSensorUpdate` stays true, and the `EditText` simply displays the Android-restored state (the previously edited value)!
AND, if the user taps the `cbMagnetic` checkbox while `pendingInitialSensorUpdate` is true, the `EditText` is technically locked, but the checkbox listener reads the restored text, adds declination, and calls `setText()`. So the text WOULD change.
Wait, if the text changes, why did they say "has no visible effect"?

What if the user is talking about a DIFFERENT checkbox? No.
What if `calculateCurrentDeclination()` is failing because `currentLat` is null?
If they share a location, `currentLat` is NOT null.

Let's do this:
1. When sharing a location, ALWAYS clear the text box if we are waiting for the sensor.
```kotlin
                if (isManualDefault) {
                    etAzimuth.text.clear()
                    etAzimuth.isEnabled = false
                    pendingInitialSensorUpdate = true
                }
```
2. In `updateBackAzimuthDisplay`, make sure we update `tvDeclination` ALWAYS.
```kotlin
        val declination = calculateCurrentDeclination()
        tvDeclination.text = "Magnetic declination: ${String.format("%.1f", declination)}°"
```
Wait, `updateBackAzimuthDisplay` IS updating `tvDeclination`. But if `cbMagnetic` listener DOES NOT call `updateBackAzimuthDisplay` (because it's in manual mode), `tvDeclination` doesn't get updated when you toggle the checkbox! Wait, the declination value itself doesn't change when you toggle the checkbox. The only thing that changes is the azimuth value.

Wait! If `cbMagnetic` listener does this:
```kotlin
                            var currentVal = currentText.toFloat()
                            val declination = calculateCurrentDeclination()
                            if (isChecked) {
                                currentVal += declination
                            } else {
                                currentVal -= declination
                            }
```
What if `currentText` is an ALREADY modified value?
Let's say they type "100".
They check the box. Declination is 10.
It becomes "110".
They uncheck the box. It reads "110". It subtracts 10. It becomes "100".
This works perfectly!

Why would the user say "the declination check / uncheck has no visible effect"?
Maybe they mean WHEN they share a location?
"when sharing a location with our app, and the azimuth is manual, the azimuth default is a previously edited value, not the magnetic sensor value."
Wait! If they share a location, we set `pendingInitialSensorUpdate = true`.
If `onSensorChanged` fires, it calls `updateBackAzimuthDisplay(true)`.
```kotlin
        if (!cbManualAzimuth.isChecked || forceUpdateEditText) {
            etAzimuth.setText(String.format("%.1f", azimuthToDisplay))
```
If `azimuthToDisplay` is exactly the same as the previous sensor value (because the phone hasn't moved much), then `etAzimuth.setText` will set it to the SAME value.
But they said "a previously edited value". Which means a value they TYPED.
How can a value they TYPED survive?
Because if `onSensorChanged` NEVER fires (because the sensor is asleep or the phone is perfectly still), `etAzimuth` is never updated!
To fix this, I MUST set `etAzimuth.setText(String.format("%.1f", baseAzimuth))` immediately in `handleIntent` if I can't guarantee `onSensorChanged` will fire!
Actually, if I just clear the text, they will see a blank box. Then when the sensor fires, it populates. If it never fires, it stays blank. That proves the sensor hasn't fired!
But wait! If they share an intent, `currentLat` and `currentLon` change.
Does that trigger `updateBackAzimuthDisplay()`?
In my last fix, I changed `handleIntent` to:
```kotlin
            if (isManualDefault) {
                etAzimuth.isEnabled = false
                pendingInitialSensorUpdate = true
            } else { ... }
```
Wait, I did NOT call `updateBackAzimuthDisplay()` at the end of `handleIntent` if `locationParsed` is true!
Before my changes, it called `updateBackAzimuthDisplay(true)`.
If `updateBackAzimuthDisplay` is NOT called, then `tvDeclination.text` is NOT updated to reflect the newly shared location!
Ah! If `tvDeclination` is not updated, it might say "Magnetic declination: 0.0°" or show the declination for the old location!
If `tvDeclination` shows 0.0, then when they toggle `cbMagnetic`, it adds/subtracts 0.0, which HAS NO VISIBLE EFFECT!
YES! That is the bug!
I removed `updateBackAzimuthDisplay(true)` from the end of `handleIntent` because I thought `pendingInitialSensorUpdate` would handle it! But if `pendingInitialSensorUpdate` waits for the sensor, and the sensor never fires, `updateBackAzimuthDisplay` is NEVER called!
So `tvDeclination` never updates, and `calculateCurrentDeclination` might be stale or not reflected in the UI!
AND `etAzimuth` never updates, leaving the previously edited value!

So I MUST call `updateBackAzimuthDisplay(true)` immediately in `handleIntent`!
If I call `updateBackAzimuthDisplay(true)` immediately, it will use the current `baseAzimuth` (even if stale), update the text box (overwriting the previously edited value), AND update `tvDeclination` with the correct declination for the new coordinates!
Then if the sensor fires later, it will update it again.

Let's check `handleIntent`:
```kotlin
            if (isManualDefault) {
                etAzimuth.isEnabled = false
                pendingInitialSensorUpdate = true
            } else {
                etAzimuth.isEnabled = false
                etAzimuth.text.clear()
                etAzimuth.clearFocus()
            }

            // Set pending flag so the next sensor event updates UI dynamically.
            // Wait, in my previous patch I REMOVED the call to updateBackAzimuthDisplay() here!
```
