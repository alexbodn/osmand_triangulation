import re

with open('app/app/src/main/res/layout/fragment_locations.xml', 'r') as f:
    content = f.read()

new_buttons = """
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvLocations"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false"
        android:paddingBottom="16dp" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="8dp">

        <Button
            android:id="@+id/btnExportAll"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Export All"
            android:layout_marginEnd="4dp"/>

        <Button
            android:id="@+id/btnExportBounded"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Export Bounded"
            android:layout_marginStart="4dp"/>
    </LinearLayout>

</LinearLayout>
"""

content = content.replace("""    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvLocations"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false"
        android:paddingBottom="16dp" />

</LinearLayout>""", new_buttons)

with open('app/app/src/main/res/layout/fragment_locations.xml', 'w') as f:
    f.write(content)
