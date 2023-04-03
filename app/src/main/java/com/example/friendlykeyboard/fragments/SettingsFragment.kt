package com.example.friendlykeyboard.fragments

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.friendlykeyboard.SettingsKeyboardBackgroundActivity
import com.example.friendlykeyboard.SettingsKeyboardColorActivity
import com.example.friendlykeyboard.SettingsKeyboardFontActivity
import com.example.friendlykeyboard.SettingsKeyboardSizeActivity
import com.example.friendlykeyboard.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private lateinit var pref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(layoutInflater, container, false)
        pref = requireActivity().getSharedPreferences("setting", Activity.MODE_PRIVATE)
        editor = pref.edit()

        initClickListener()

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        val height = pref.getInt("keyboardHeight", 150) - 50
        val paddingLeft = pref.getInt("keyboardPaddingLeft", 0)
        val paddingRight = pref.getInt("keyboardPaddingRight", 0)
        val paddingBottom = pref.getInt("keyboardPaddingBottom", 0)
        binding.settingsItemKeyboardSize.attribute.text =
            "높이: ${height}%, 좌측: ${paddingLeft}dp, 우측: ${paddingRight}dp, 하단: ${paddingBottom}dp"

        val fontColor = pref.getInt("keyboardFontColor", 0)
        with (binding.settingsItemKeyboardFont) {
            imageView.drawable.setTint(fontColor)
            attribute.setTextColor(fontColor)
        }

        val color = pref.getInt("keyboardColor", 0)
        with (binding.settingsItemKeyboardColor) {
            imageView.drawable.setTint(color)
            attribute.setTextColor(color)
        }

        val background_color = pref.getInt("keyboardBackground", 0)
        with (binding.settingsItemKeyboardBackground) {
            imageView.drawable.setTint(background_color)
            attribute.setTextColor(background_color)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initClickListener() {
        binding.settingsItemKeyboardSize.item.setOnClickListener {
            startActivity(Intent(activity, SettingsKeyboardSizeActivity::class.java))
        }

        binding.settingsItemKeyboardFont.item.setOnClickListener {
            startActivity(Intent(activity, SettingsKeyboardFontActivity::class.java))
        }

        binding.settingsItemKeyboardColor.item.setOnClickListener {
            startActivity(Intent(activity, SettingsKeyboardColorActivity::class.java))
        }

        binding.settingsItemKeyboardBackground.item.setOnClickListener {
            startActivity(Intent(activity, SettingsKeyboardBackgroundActivity::class.java))
        }
    }
}