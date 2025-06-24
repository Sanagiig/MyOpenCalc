package com.example.myopencalc.activities

import android.icu.number.NumberFormatter
import android.icu.text.NumberingSystem
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.myopencalc.R
import com.example.myopencalc.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var popupMenu: PopupMenu? = null

    private var isEqualLastAction = false

    private val decimalSeparatorSymbol =
        DecimalFormatSymbols.getInstance().decimalSeparator.toString()

    private val groupingSeparatorSymbol =
        DecimalFormatSymbols.getInstance().groupingSeparator.toString()

    private var numberingSystem = NumberingSystem.INTERNATIONAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun openAppMenu(view: View) {
        if (popupMenu == null) {
            popupMenu = PopupMenu(this, view)
            val inflater = popupMenu!!.menuInflater
            inflater.inflate(R.menu.app_menu, popupMenu!!.menu)
        }
        popupMenu!!.show()
    }

    fun squareButton(view: View) {}
    fun piButton(view: View) {}
    fun exponentButton(view: View) {}
    fun factorialButton(view: View) {}
    fun scientistModeSwitchButton(view: View) {}
    fun degreeButton(view: View) {}
    fun sineButton(view: View) {}
    fun cosineButton(view: View) {}
    fun tangentButton(view: View) {}
    fun invButton(view: View) {}
    fun eButton(view: View) {}
    fun naturalLogarithmButton(view: View) {}
    fun logarithmButton(view: View) {}
    fun log2Button(view: View) {}
    fun clearButton(view: View) {}
    fun leftParenthesisButton(view: View) {}
    fun rightParenthesisButton(view: View) {}
    fun parenthesesButton(view: View) {}
    fun percent(view: View) {}
    fun divideButton(view: View) {}
    fun keyDigitPadMappingToDisplay(view: View) {
        updateDisplay(view, (view as Button).text.toString())
    }

    fun multiplyButton(view: View) {}
    fun subtractButton(view: View) {}
    fun pointButton(view: View) {}
    fun backspaceButton(view: View) {}
    fun equalsButton(view: View) {}
    fun addButton(view: View) {}

    private fun keyVibration(view: View) {
        if (MyPreferences(this).vibrationMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }

    private fun updateDisplay(view: View, value: String) {
        val valueNoSeparators = value.replace(groupingSeparatorSymbol, "")
        val isValueInt = valueNoSeparators.toIntOrNull() != null

        if (isEqualLastAction) {
            if (isValueInt || value == decimalSeparatorSymbol) {
                binding.input?.setText("")
            } else {
                binding.input?.setSelection(binding.input?.text?.length ?: 0)
            }
        }

        if(!binding.input?.isCursorVisible!!){
            binding.input?.isCursorVisible = true
        }

        lifecycleScope.launch(Dispatchers.Default){
            withContext(Dispatchers.Main){
                keyVibration(view)
            }

            val formerValue = binding.input!!.text.toString()
            val cursorPosition = binding.input!!.selectionStart
            val leftValue = formerValue.substring(0, cursorPosition)
            val leftValueFormatted = NumberFormatter.format(leftValue,decimalSeparatorSymbol,groupingSeparatorSymbol,number)
        }
    }
}