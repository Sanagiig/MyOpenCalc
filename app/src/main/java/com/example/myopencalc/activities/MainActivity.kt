package com.example.myopencalc.activities

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myopencalc.MyPreferences
import com.example.myopencalc.R
import com.example.myopencalc.TextSizeAdjuster
import com.example.myopencalc.calculator.parser.Expression
import com.example.myopencalc.calculator.parser.NumberFormatter
import com.example.myopencalc.calculator.parser.NumberingSystem
import com.example.myopencalc.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import com.example.myopencalc.calculator.parser.*
import com.example.myopencalc.history.History
import com.example.myopencalc.history.HistoryAdapter
import java.util.UUID

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var root: View
    private var popupMenu: PopupMenu? = null

    private var isEqualLastAction = false
    private var errorStatusOld = false
    private var isStillTheSameCalculation_autoSaveCalculationWithoutEqualOption = false
    private var lastHistoryElementId = ""

    private val decimalSeparatorSymbol =
        DecimalFormatSymbols.getInstance().decimalSeparator.toString()

    private val groupingSeparatorSymbol =
        DecimalFormatSymbols.getInstance().groupingSeparator.toString()

    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var historyLayoutMgr: LinearLayoutManager
    private lateinit var binding: ActivityMainBinding

    private var calculationResult = BigDecimal.ZERO

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

        binding = ActivityMainBinding.inflate(layoutInflater)
        root = binding.root
        setContentView(root)
        init();
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
    fun exponentButton(view: View) {
        addSymbol(view, "^")
    }

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
    fun clearButton(view: View) {
        keyVibration(view)
        binding.calcInput?.setText("")
        binding.resultDisplay?.text = ""
        isStillTheSameCalculation_autoSaveCalculationWithoutEqualOption = false
    }

    fun leftParenthesisButton(view: View) {}
    fun rightParenthesisButton(view: View) {}
    fun parenthesesButton(view: View) {}
    fun percent(view: View) {}
    fun divideButton(view: View) {
        addSymbol(view, "÷")
    }

    fun keyDigitPadMappingToDisplay(view: View) {
        updateDisplay(view, (view as Button).text.toString())
    }

    fun multiplyButton(view: View) {
        addSymbol(view, "×")
    }

    fun subtractButton(view: View) {
        addSymbol(view, "-")
    }

    fun pointButton(view: View) {
        val cursorPosition = binding.calcInput?.selectionStart ?: 0
        var currentNumber = ""
        if (binding.calcInput?.text.toString().isNotEmpty()) {
            var startPosition = 0
            var endPosition = 0
            if (cursorPosition > 0) {
                startPosition = cursorPosition
                while (startPosition > 0 && (binding.calcInput!!.text!![startPosition - 1].isDigit()
                            || binding.calcInput!!.text[startPosition - 1].toString() == decimalSeparatorSymbol
                            || binding.calcInput!!.text[startPosition - 1].toString() == groupingSeparatorSymbol)
                ) {
                    startPosition -= 1
                }
            }

            if (cursorPosition == binding.calcInput!!.text.length) {
                endPosition = binding.calcInput!!.text.length
            }

            if (cursorPosition < binding.calcInput!!.text.length) {
                endPosition = if (cursorPosition != 0) cursorPosition else 0
                while (endPosition < binding.calcInput!!.text.length
                    && (binding.calcInput!!.text[endPosition].isDigit()
                            || binding.calcInput!!.text[endPosition].toString() == decimalSeparatorSymbol
                            || binding.calcInput!!.text[endPosition].toString() == groupingSeparatorSymbol)
                ) {
                    endPosition += 1
                }
            }

            currentNumber = binding.calcInput!!.text.substring(startPosition, endPosition)
        }

        if (decimalSeparatorSymbol !in currentNumber) {
            updateDisplay(view, decimalSeparatorSymbol)
        }
    }

    fun backspaceButton(view: View) {}

    @SuppressLint("SetTextI18n")
    fun equalsButton(view: View) {
        lifecycleScope.launch(Dispatchers.Default) {
            keyVibration(view)

            val calculation = binding.calcInput?.text.toString()

            Expression().addParenthesis(calculation)

            if (calculation != "") {
                val resultString = calculationResult.toString()
                var formattedResult = NumberFormatter.format(
                    resultString.replace(".", decimalSeparatorSymbol),
                    decimalSeparatorSymbol,
                    groupingSeparatorSymbol,
                    numberingSystem
                )

                // If result is a number and it is finite
                if (!(division_by_0 || domain_error || syntax_error || is_infinity || require_real_number)) {

                    // Remove zeros at the end of the results (after point)
                    val resultSplited = resultString.split('.')
                    if (resultSplited.size > 1) {
                        val resultPartAfterDecimalSeparator = resultSplited[1].trimEnd('0')
                        var resultWithoutZeros = resultSplited[0]

                        if (resultPartAfterDecimalSeparator != "") {
                            resultWithoutZeros =
                                resultSplited[0] + "." + resultPartAfterDecimalSeparator
                        }

                        formattedResult = NumberFormatter.format(
                            resultWithoutZeros.replace(".", decimalSeparatorSymbol),
                            decimalSeparatorSymbol,
                            groupingSeparatorSymbol,
                            numberingSystem
                        )
                    }

                    // Hide the cursor before updating binding.input to avoid weird cursor movement
                    withContext(Dispatchers.Main) {
                        binding.calcInput!!.isCursorVisible = false
                    }

                    // Display result
                    withContext(Dispatchers.Main) {
                        binding.calcInput!!.setText(formattedResult)
                    }

                    // Set cursor
                    withContext(Dispatchers.Main) {
                        binding.calcInput!!.setSelection(binding.calcInput!!.length())

                        binding.calcInput!!.isCursorVisible = false

                        binding.resultDisplay!!.text = ""
                    }

                    if (calculation != formattedResult) {
                        val history = MyPreferences(this@MainActivity).getHistory()

                        isStillTheSameCalculation_autoSaveCalculationWithoutEqualOption = false

                        if (history.isEmpty() || history[history.size - 1].calculation != calculation) {
                            // Store time
                            val currentTime = System.currentTimeMillis().toString()

                            // Save to history
                            val historyElementId = UUID.randomUUID().toString()
                            history.add(
                                History(
                                    calculation = calculation,
                                    result = formattedResult,
                                    time = currentTime,
                                    id = historyElementId // Generate a random id
                                )
                            )

                            MyPreferences(this@MainActivity).saveHistory(history)

                            lastHistoryElementId = historyElementId
                            isStillTheSameCalculation_autoSaveCalculationWithoutEqualOption = true

                            MyPreferences(this@MainActivity).saveHistory(history)

                            withContext(Dispatchers.Main) {
                                historyAdapter.appendOneHistoryElement(
                                    History(
                                        calculation = calculation,
                                        result = formattedResult,
                                        time = currentTime,
                                        id = UUID.randomUUID().toString() // Generate a random id
                                    )
                                )

                                val historySize =
                                    MyPreferences(this@MainActivity).historySize!!.toInt()
                                while (historySize != -1 && historyAdapter.itemCount >= historySize && historyAdapter.itemCount > 0) {
                                    historyAdapter.removeFirstHistoryElement()
                                }
                                checkEmptyHistoryForNoHistoryLabel()
                                // Scroll to the bottom of the recycle view
                                binding.historyRecylcleView!!.scrollToPosition(historyAdapter.itemCount - 1)
                            }
                        }
                    }

                    isEqualLastAction = true
                } else {
                    withContext(Dispatchers.Main) {
                        if (syntax_error) {
                            setErrorColor(true)
                            binding.resultDisplay!!.text = getString(R.string.syntax_error)
                        } else if (domain_error) {
                            setErrorColor(true)
                            binding.resultDisplay!!.text = getString(R.string.domain_error)
                        } else if (require_real_number) {
                            setErrorColor(true)
                            binding.resultDisplay!!.text = getString(R.string.require_real_number)
                        } else if (division_by_0) {
                            setErrorColor(true)
                            binding.resultDisplay!!.text = getString(R.string.division_by_0)
                        } else if (is_infinity) {
                            if (calculationResult < BigDecimal.ZERO) binding.resultDisplay!!.text =
                                "-" + getString(
                                    R.string.infinity
                                )
                            else binding.resultDisplay!!.text = getString(R.string.value_too_large)
                            //} else if (result.isNaN()) {
                            //    setErrorColor(true)
                            //    binding.resultDisplay.setText(getString(R.string.math_error))
                        } else {
                            binding.resultDisplay!!.text = formattedResult
                            isEqualLastAction =
                                true // Do not clear the calculation (if you click into a number) if there is an error
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) { binding.resultDisplay!!.text = "" }
            }
        }
    }

    fun addButton(view: View) {
        addSymbol(view, "+")
    }

    private fun keyVibration(view: View) {
        if (MyPreferences(this).vibrationMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun addSymbol(view: View, currentSymbol: String) {
        val text = binding.calcInput?.text.toString()
        if (text.isEmpty()) {
            if (currentSymbol == "-") {
                updateDisplay(view, currentSymbol);
            } else {
                keyVibration(view)
            }
        } else {
            val curPosition = binding.calcInput?.selectionStart ?: 0
            val nextChar = if (curPosition < text.length) text[curPosition].toString() else "0"
            val preChar = if (curPosition > 0) text[curPosition - 1].toString() else "0"
            val preSymbol = if (curPosition > 1) text[curPosition - 2].toString() else "0"

            if (currentSymbol != preChar // Ignore multiple presses of the same button
                && currentSymbol != nextChar
                && preSymbol != "√" // No symbol can be added on an empty square root
                && (preChar != "(" || currentSymbol == "-")  // Ensure that we are not at the beginning of a parenthesis
                && (preSymbol !in "+\\-÷×" || preChar !in "+\\-÷×") && preChar != decimalSeparatorSymbol
            ) {
                if (preChar.matches("[+\\-÷×^]".toRegex())) {
                    keyVibration(view)
                    val leftString =
                        binding.calcInput?.text?.subSequence(0, curPosition - 1).toString()
                    val rightString =
                        binding.calcInput?.text?.subSequence(curPosition, text.length).toString()

                    if (currentSymbol == "-") {
                        if (preChar in "+-") {
                            binding.calcInput?.setText(leftString + currentSymbol + rightString)
                            binding.calcInput?.setSelection(curPosition)
                        } else {
                            // ???
                            binding.calcInput?.setText(leftString + preChar + currentSymbol + rightString)
                            binding.calcInput?.setSelection(curPosition + 1)
                        }
                    } else if (curPosition > 1 && binding.calcInput?.text?.get(curPosition - 2) != '(') {
                        binding.calcInput?.setText(leftString + currentSymbol + rightString)
                        binding.calcInput?.setSelection(curPosition)
                    } else if (currentSymbol == "+") {
                        binding.calcInput?.setText(leftString + rightString)
                        binding.calcInput?.setSelection(curPosition - 1)
                    }


                } // If next character is a symbol, replace it
                else if (nextChar.matches("[+\\-÷×^%!]".toRegex())
                    && currentSymbol != "%"
                ) { // Make sure that percent symbol doesn't replace succeeding symbols
                    keyVibration(view)

                    val leftString =
                        binding.calcInput?.text?.subSequence(0, curPosition).toString()

                    val rightString =
                        binding.calcInput?.text?.subSequence(curPosition + 1, text.length)
                            .toString()

                    if (curPosition > 0 && preChar != "(") {
                        binding.calcInput?.setText(leftString + currentSymbol + rightString)
                        binding.calcInput?.setSelection(curPosition + 1)
                    } else if (currentSymbol == "+") binding.calcInput?.setText(leftString + rightString)

                }// Otherwise just update the display
                else if (curPosition > 0 || nextChar != "0" && currentSymbol == "-") {
                    updateDisplay(view, currentSymbol)
                } else keyVibration(view)
            }

        }
    }

    private fun updateDisplay(view: View, value: String) {
        val valueNoSeparators = value.replace(groupingSeparatorSymbol, "")
        val isValueInt = valueNoSeparators.toIntOrNull() != null

        if (isEqualLastAction) {
            if (isValueInt || value == decimalSeparatorSymbol) {
                binding.calcInput?.setText("")
            } else {
                binding.calcInput?.setSelection(binding.calcInput?.text?.length ?: 0)
            }
        }

        if (!binding.calcInput?.isCursorVisible!!) {
            binding.calcInput?.isCursorVisible = true
        }

        lifecycleScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                keyVibration(view)
            }
            Log.d(TAG, "updateDisplay: ${binding.calcInput!!.text}")

            val formerValue = binding.calcInput!!.text.toString()
            val cursorPosition = binding.calcInput!!.selectionStart
            val leftValue = formerValue.subSequence(0, cursorPosition).toString()
            val leftValueFormatted = NumberFormatter.format(
                leftValue,
                decimalSeparatorSymbol,
                groupingSeparatorSymbol,
                numberingSystem
            )
            val rightValue = formerValue.subSequence(cursorPosition, formerValue.length).toString()

            val newValue = leftValue + value + rightValue

            val newValueFormatted =
                NumberFormatter.format(
                    newValue,
                    decimalSeparatorSymbol,
                    groupingSeparatorSymbol,
                    numberingSystem
                )

            withContext(Dispatchers.Main) {

                // Update Display
                binding.calcInput!!.setText(newValueFormatted)
                Log.d(TAG, "updateDisplay: $newValueFormatted")
                // Set cursor position
                if (isValueInt) {
                    val cursorOffset = newValueFormatted.length - newValue.length
                    binding.calcInput!!.setSelection(cursorPosition + value.length + cursorOffset)
                } else {
                    val desiredCursorPosition = leftValueFormatted.length + value.length
                    // Limit the cursor position to the length of the calcInput
                    val safeCursorPosition =
                        desiredCursorPosition.coerceAtMost(binding.calcInput!!.text.length)
                    binding.calcInput!!.setSelection(safeCursorPosition)
                }
            }
        }


    }

    @SuppressLint("SetTextI18n")
    private fun updateResultDisplay() {
        lifecycleScope.launch(Dispatchers.Default) {
            // Reset text color
            setErrorColor(false)

            val calculation = binding.calcInput!!.text.toString()

            if (calculation != "") {
                division_by_0 = false
                domain_error = false
                syntax_error = false
                is_infinity = false
                require_real_number = false

                val calculationTmp = Expression().getCleanExpression(
                    binding.calcInput!!.text.toString(),
                    decimalSeparatorSymbol,
                    groupingSeparatorSymbol
                )

                calculationResult =
                    Calculator(MyPreferences(this@MainActivity).numberPrecision!!.toInt()).evaluate(
                        calculationTmp,
                        isDegreeModeActivated
                    )
            }


        }
    }

    fun init() {
        val textSizeAdjuster = TextSizeAdjuster(this)
        binding.calcInput?.addTextChangedListener(object : TextWatcher {
            var preLen: Int = 0
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                preLen = p0?.length ?: 0
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                updateResultDisplay()
                textSizeAdjuster.adjustTextSize(
                    binding.calcInput!!,
                    TextSizeAdjuster.AdjustableTextType.Input
                )
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })

        historyLayoutMgr = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.historyRecylcleView!!.layoutManager = historyLayoutMgr
        historyAdapter = HistoryAdapter(
            mutableListOf(),
            { value ->
                updateDisplay(window.decorView, value)
            },
            this // Assuming this is an Activity or Fragment with a Context
        )
        historyAdapter.updateHistoryList()
        binding.historyRecylcleView!!.adapter = historyAdapter
    }

    fun checkEmptyHistoryForNoHistoryLabel() {
        if (historyAdapter.itemCount == 0) {
            binding.historyRecylcleView!!.visibility = View.GONE
            binding.noHistoryText!!.visibility = View.VISIBLE
        } else {
            binding.noHistoryText!!.visibility = View.GONE
            binding.historyRecylcleView!!.visibility = View.VISIBLE
        }
    }

    private fun setErrorColor(errorStatus: Boolean) {
        // Only run if the color needs to be updated
        runOnUiThread {
            if (errorStatus != errorStatusOld) {
                // Set error color
                if (errorStatus) {
                    binding.calcInput!!.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.calculation_error_color
                        )
                    )
                    binding.resultDisplay!!.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.calculation_error_color
                        )
                    )
                }
                // Clear error color
                else {
                    binding.calcInput!!.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.text_color
                        )
                    )
                    binding.resultDisplay!!.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.text_second_color
                        )
                    )
                }
                errorStatusOld = errorStatus
            }
        }
    }
}