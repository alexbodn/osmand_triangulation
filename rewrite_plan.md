**Goal:** Fix the manual azimuth entry bug that double-subtracts the declination during programmatic edits (e.g. tapping an item in the list or importing).

**Root Cause:**
The declination double-subtraction is caused because `isUserEditing` is set `true` when the edit text gets focus, causing the `TextWatcher` to incorrectly treat programmatic inserts (`etAzimuth.setText()`) as human edits, reverse-calculating the `baseAzimuth` (subtracting declination). This was exacerbated by `updateBackAzimuthDisplay(true)` triggering another cycle.

**Solution:**
1. Entirely remove the `TextWatcher` and `isUserEditing` flag.
2. The `etAzimuth` UI should literally just show the direct azimuth. If the user wants to edit it, they type a new direct azimuth.
3. When the user taps the red "save" (arrow) button (`ivArrow`), we simply take the literal float value inside `etAzimuth.text`. We don't need a complex reactive loop backing out `baseAzimuth`.
4. The compass updates `baseAzimuth`. If `!cbManualAzimuth.isChecked`, we calculate `azimuthToDisplay` (baseAzimuth + declination) and put it into `etAzimuth`.
5. If `cbManualAzimuth.isChecked`, the compass updates STILL update `baseAzimuth` in the background, but we SKIP putting anything into `etAzimuth`. The value in `etAzimuth` remains totally static unless the user types in it.
6. When `cbManualAzimuth` is toggled ON, the text box freezes at the last compass value (which is already displayed with declination applied).
7. When tapping an item in the list, we just inject the item's direct azimuth into `etAzimuth`, skip the focus tricks, and let it freeze. No `TextWatcher` fires, so no reverse math occurs.

I need to confirm if we need to store `baseAzimuth` for points, or just the final direct azimuth. Wait, `Reading` stores `azimuth` and `backAzimuth`. It doesn't even store `baseAzimuth`. The `baseAzimuth` is just the current hardware compass reading. So we don't need to reverse-calculate it at all when the user types!

Wait, `ivArrow` touch listener currently uses:
```kotlin
var azimuthToUse = baseAzimuth
if (cbMagnetic.isChecked) {
    azimuthToUse += calculateCurrentDeclination()
    // ...
}
```

This means `ivArrow` uses the hidden `baseAzimuth` to generate the new reading! If the user types "180" in the box, `ivArrow` ignores the box and reads `baseAzimuth` instead! This is a massive bug! The `TextWatcher` was added to hack `baseAzimuth` so that `ivArrow` would read the user's input.
Instead, `ivArrow` should just read `etAzimuth.text.toString().toFloat()`!

Let's verify this in the plan.
