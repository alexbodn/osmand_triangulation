package com.example.triangulation.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.example.triangulation.R

class SettingsFragment : Fragment() {

    private lateinit var etDistance: EditText
    private lateinit var etHorizon: EditText
    private lateinit var cbVerbose: CheckBox

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etDistance = view.findViewById(R.id.etDistance)
        etHorizon = view.findViewById(R.id.etHorizon)
        cbVerbose = view.findViewById(R.id.cbVerbose)

        val sharedPrefs = requireActivity().getSharedPreferences("triangulation_prefs", Context.MODE_PRIVATE)
        val savedDist = sharedPrefs.getFloat("defaultDistance", 3.0f)
        etDistance.setText(savedDist.toString())

        val savedHorizon = sharedPrefs.getInt("horizon_threshold", 2000)
        etHorizon.setText(savedHorizon.toString())

        cbVerbose.isChecked = sharedPrefs.getBoolean("verbose_mode", false)

        cbVerbose.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("verbose_mode", isChecked).apply()
        }

        etDistance.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT) {
                v.clearFocus()
                val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                try {
                    val dist = etDistance.text.toString().toFloat()
                    sharedPrefs.edit().putFloat("defaultDistance", dist).apply()
                } catch (e: Exception) {}
                true
            } else false
        }

        etDistance.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                try {
                    val dist = etDistance.text.toString().toFloat()
                    sharedPrefs.edit().putFloat("defaultDistance", dist).apply()
                } catch (e: Exception) {}
            }
        }

        etHorizon.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT) {
                v.clearFocus()
                val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                try {
                    val threshold = etHorizon.text.toString().toInt()
                    sharedPrefs.edit().putInt("horizon_threshold", threshold).apply()
                } catch (e: Exception) {}
                true
            } else false
        }

        etHorizon.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                try {
                    val threshold = etHorizon.text.toString().toInt()
                    sharedPrefs.edit().putInt("horizon_threshold", threshold).apply()
                } catch (e: Exception) {}
            }
        }
    }
}
