
package com.example.triangulation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.example.triangulation.databinding.ActivityMainBinding
import com.example.triangulation.ui.ViewPagerAdapter
import com.example.triangulation.ui.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter


        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Home"
                1 -> "Locations"
                2 -> "Settings"
                3 -> "Usage"
                else -> null
            }
        }.attach()

        val sharedPrefs = getSharedPreferences("triangulation_prefs", android.content.Context.MODE_PRIVATE)
        val isFirstRun = sharedPrefs.getBoolean("isFirstRun", true)
        if (isFirstRun) {
            sharedPrefs.edit().putBoolean("isFirstRun", false).apply()
            binding.viewPager.setCurrentItem(3, false)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the activity's intent

        // Forward to HomeFragment
        val fragments = supportFragmentManager.fragments
        for (fragment in fragments) {
            if (fragment is HomeFragment) {
                fragment.onNewIntent(intent)
            }
        }
    }
}
