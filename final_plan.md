Alright, I am completely confident about the root cause now.
The user is right: when they share a new intent with the app, the declination text `tvDeclination` never updates because `updateBackAzimuthDisplay()` is not called at the end of `handleIntent`.
Because it is not called, `tvDeclination` remains "0.0°" or whatever the old location was.
Because it remains 0.0, toggling the checkbox has no visible effect because `currentVal += 0.0` does nothing.
And because `updateBackAzimuthDisplay()` is not called, the `etAzimuth` text box is NOT initialized with the `baseAzimuth` (sensor value). It just holds whatever they typed last time!
If `onSensorChanged` doesn't fire immediately (phone is still), the UI looks completely broken.

To fix both issues, I must forcefully call `updateBackAzimuthDisplay(true)` at the end of the `if (locationParsed)` block in `handleIntent` (before we switch the tab).
If I call `updateBackAzimuthDisplay(true)`, it will update the declination text AND overwrite the `etAzimuth` text box with the current `baseAzimuth` value, completely solving the "previously edited value" bug AND the "no visible effect" bug!

Let's make sure:
```kotlin
            if (isManualDefault) {
                etAzimuth.isEnabled = false
                pendingInitialSensorUpdate = true
            } else {
                etAzimuth.isEnabled = false
                etAzimuth.text.clear()
                etAzimuth.clearFocus()
            }

            updateBackAzimuthDisplay(true)
```
Wait, if `isManualDefault` is FALSE, calling `updateBackAzimuthDisplay(true)` will populate the text box!
But wait, if `isManualDefault` is FALSE, the app is in LIVE mode. So it SHOULD be populated!
Wait, if it's in LIVE mode, why did my previous code have:
```kotlin
                } else {
                    etAzimuth.isEnabled = false
                    etAzimuth.text.clear()
                    etAzimuth.clearFocus()
                }
```
If I call `updateBackAzimuthDisplay(true)` after clearing it, it will just fill it with `baseAzimuth`. That's correct! Live mode means it should show the compass value!
But wait, earlier, when I fixed the `pendingInitialSensorUpdate` bug, I noticed that `onSensorChanged` DOES continuously update it if manual mode is OFF. So if `onSensorChanged` is running, it fills it. But if it's NOT running, we still want it filled with the last known value! So calling `updateBackAzimuthDisplay(true)` is correct.
I will add `updateBackAzimuthDisplay(true)` right after the `isManualDefault` block!
