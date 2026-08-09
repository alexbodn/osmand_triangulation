
package com.example.triangulation

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.triangulation.databinding.ActivityMainBinding
import com.example.triangulation.ui.HomeFragment
import com.example.triangulation.ui.LocationsFragment
import com.example.triangulation.ui.SettingsFragment
import com.example.triangulation.ui.UsageFragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPrefs = getSharedPreferences("triangulation_prefs", android.content.Context.MODE_PRIVATE)
        var savedTheme = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        if (savedTheme == 0) { // Fix legacy MODE_NIGHT_AUTO_TIME
            savedTheme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(savedTheme)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        toggle.drawerArrowDrawable.color = android.graphics.Color.WHITE
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        val isFirstRun = sharedPrefs.getBoolean("isFirstRun", true)

        if (savedInstanceState == null) {
            if (isFirstRun) {
                sharedPrefs.edit().putBoolean("isFirstRun", false).apply()
                navigateToFragment(UsageFragment(), "Usage", R.id.nav_usage)
            } else {
                navigateToFragment(HomeFragment(), "Home", R.id.nav_home)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> navigateToFragment(HomeFragment(), "Home", R.id.nav_home)
            R.id.nav_locations -> navigateToFragment(LocationsFragment(), "Library", R.id.nav_locations)
            R.id.nav_settings -> navigateToFragment(SettingsFragment(), "Settings", R.id.nav_settings)
            R.id.nav_usage -> navigateToFragment(UsageFragment(), "Usage", R.id.nav_usage)
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun navigateToFragment(fragment: Fragment, title: String? = null, navId: Int? = null) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        if (title != null) {
            supportActionBar?.title = title
        }
        if (navId != null) {
            binding.navView.setCheckedItem(navId)
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the activity's intent

        val hasLocationData = intent != null && (intent.hasExtra("lat") ||
            (intent.data?.scheme == "geo" && intent.data?.toString()?.contains("bbox=") == false) ||
            (intent.action == Intent.ACTION_SEND && intent.type == "text/plain" &&
             intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                 it.contains("lat=") || it.contains("geo:") || it.matches(Regex(".*[0-9]{1,2}\\.[0-9]+[^0-9.-]+[0-9]{1,3}\\.[0-9]+.*"))
             } == true))

        if (hasLocationData) {
            val isHomeFragmentVisible = supportFragmentManager.fragments.any { it is HomeFragment && it.isVisible }
            if (!isHomeFragmentVisible) {
                navigateToFragment(HomeFragment(), "Home", R.id.nav_home)
                supportFragmentManager.executePendingTransactions()
            }
        }

        // Forward to HomeFragment
        val fragments = supportFragmentManager.fragments
        for (fragment in fragments) {
            if (fragment is HomeFragment) {
                fragment.onNewIntent(intent)
            }
        }
    }
}
