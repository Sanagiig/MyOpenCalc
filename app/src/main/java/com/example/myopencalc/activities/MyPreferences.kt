package com.example.myopencalc.activities

import android.content.Context
import android.preference.PreferenceManager

class MyPreferences(context: Context) {
    var ctx = context

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        private const val THEME = "darkempire78.opencalculator.THEME"
        private const val FORCE_DAY_NIGHT = "darkempire78.opencalculator.FORCE_DAY_NIGHT"

        private const val KEY_VIBRATION_STATUS = "darkempire78.opencalculator.KEY_VIBRATION_STATUS"
        private const val KEY_HISTORY = "darkempire78.opencalculator.HISTORY_ELEMENTS"
        private const val KEY_PREVENT_PHONE_FROM_SLEEPING = "darkempire78.opencalculator.PREVENT_PHONE_FROM_SLEEPING"
        private const val KEY_HISTORY_SIZE = "darkempire78.opencalculator.HISTORY_SIZE"
        private const val KEY_SCIENTIFIC_MODE_ENABLED_BY_DEFAULT = "darkempire78.opencalculator.SCIENTIFIC_MODE_ENABLED_BY_DEFAULT"
        private const val KEY_RADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT = "darkempire78.opencalculator.RADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT"
        private const val KEY_NUMBER_PRECISION = "darkempire78.opencalculator.NUMBER_PRECISION"
        private const val KEY_WRITE_NUMBER_INTO_SCIENTIC_NOTATION = "darkempire78.opencalculator.WRITE_NUMBER_INTO_SCIENTIC_NOTATION"
        private const val KEY_LONG_CLICK_TO_COPY_VALUE = "darkempire78.opencalculator.LONG_CLICK_TO_COPY_VALUE"
        private const val KEY_ADD_MODULO_BUTTON = "darkempire78.opencalculator.ADD_MODULO_BUTTON"
        private const val KEY_SPLIT_PARENTHESIS_BUTTON = "darkempire78.opencalculator.SPLIT_PARENTHESIS_BUTTON"
        private const val KEY_DELETE_HISTORY_ON_SWIPE = "darkempire78.opencalculator.DELETE_HISTORY_ELEMENT_ON_SWIPE"
        private const val KEY_AUTO_SAVE_CALCULATION_WITHOUT_EQUAL_BUTTON = "darkempire78.opencalculator.AUTO_SAVE_CALCULATION_WITHOUT_EQUAL_BUTTON"
        private const val KEY_NUMBERING_SYSTEM = "darkempire78.opencalculator.NUMBERING_SYSTEM"
    }

    var vibrationMode = preferences.getBoolean(KEY_VIBRATION_STATUS, true)
}