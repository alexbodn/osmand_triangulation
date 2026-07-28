The review pointed out one edge case regression.
Before my refactor, when the app was launched with a *new* location intent (e.g. from OsmAnd) and the user's default setting had "Manual Mode" set to TRUE, the field was initially locked (`etAzimuth.isEnabled = false`), and `pendingInitialSensorUpdate = true` was set.
Then, `onSensorChanged` would trigger, populate the text box using `updateBackAzimuthDisplay(true)`, unlock the field, and set `pendingInitialSensorUpdate = false`.
This ensured the field got populated with at least ONE live compass reading before manual mode froze it.

With my new changes, `if (isManualDefault) etAzimuth.isEnabled = true` is executed right away, but the field stays *empty*.
Since manual mode freezes the field, `onSensorChanged` will never populate it, and the user just gets an empty text box.

To fix this, I should re-introduce a lightweight version of `pendingInitialSensorUpdate`.
1. Add back `private var pendingInitialSensorUpdate = false` to the class variables.
2. In the `else` block (when sharing a new location without an explicit azimuth extra):
```kotlin
                if (isManualDefault) {
                    etAzimuth.isEnabled = false
                    pendingInitialSensorUpdate = true
                }
```
3. In `onSensorChanged`:
```kotlin
            // Handle deferred UI update for new intents with manual mode defaulted ON
            if (pendingInitialSensorUpdate) {
                pendingInitialSensorUpdate = false
                updateBackAzimuthDisplay()
                etAzimuth.isEnabled = true
            }
```
Let's make these changes.
