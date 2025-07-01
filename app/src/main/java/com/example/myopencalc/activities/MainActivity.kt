package com.example.myopencalc.activities

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myopencalc.MyPreferences
import com.example.myopencalc.R
import com.example.myopencalc.TextSizeAdjuster
import com.example.myopencalc.Themes
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
import com.example.myopencalc.calculator.parser.NumberingSystem.Companion.toNumberingSystem
import com.example.myopencalc.history.History
import com.example.myopencalc.history.HistoryAdapter
import com.sothree.slidinguppanel.PanelSlideListener
import com.sothree.slidinguppanel.PanelState
import java.math.RoundingMode
import java.util.Locale
import java.util.UUID

var appLanguage: Locale = Locale.getDefault()
var currentTheme: Int = 0

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var view: View
    private var popupMenu: PopupMenu? = null

    private var isEqualLastAction = false
    private var isDegreeModeActivated = true // Set degree by default
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
    private lateinit var itemTouchHelper: ItemTouchHelper

    private var calculationResult = BigDecimal.ZERO

    private var numberingSystem = NumberingSystem.INTERNATIONAL

    override  fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable the possibility to show the activity on the lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // Themes
        val themes = Themes(this)
        themes.applyDayNightOverride()
        setTheme(themes.getTheme())

        val fromPrefs = MyPreferences(this).numberingSystem
        numberingSystem = fromPrefs.toNumberingSystem()

        currentTheme = themes.getTheme()

        binding = ActivityMainBinding.inflate(layoutInflater)
        view = binding.root
        setContentView(view)

        // Disable the keyboard on display EditText
        binding.calcInput!!.showSoftInputOnFocus = false

        // https://www.geeksforgeeks.org/how-to-detect-long-press-in-android/
        binding.backspaceButton!!.setOnLongClickListener {
            binding.calcInput!!.setText("")
            binding.resultDisplay!!.text = ""
            isStillTheSameCalculation_autoSaveCalculationWithoutEqualOption = false
            true
        }

        // Long click to view popup options for double and triple zeroes
        binding.zeroButton!!.setOnLongClickListener {
            showPopupMenu(binding.zeroButton!!)
            true
        }

        // Set default animations and disable the fade out default animation
        // https://stackoverflow.com/questions/19943466/android-animatelayoutchanges-true-what-can-i-do-if-the-fade-out-effect-is-un
        val lt = LayoutTransition()
        lt.disableTransitionType(LayoutTransition.DISAPPEARING)
        binding.tableLayout!!.layoutTransition = lt

        // Set decimalSeparator
        binding.pointButton!!.setImageResource(if (decimalSeparatorSymbol == ",") R.drawable.comma else R.drawable.dot)

        // Set history
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

        // Scroll to the bottom of the recycle view
        if (historyAdapter.itemCount > 0) {
            binding.historyRecylcleView!!.scrollToPosition(historyAdapter.itemCount - 1)
        }

        setSwipeTouchHelperForRecyclerView()

        // Disable history if setting enabled
        val historySize = MyPreferences(this).historySize!!.toInt()
        if (historySize == 0) {
            binding.historyRecylcleView!!.visibility = View.GONE
            binding.slidingLayoutButton!!.visibility = View.GONE
            binding.slidingLayout!!.isEnabled = false
        } else {
            binding.slidingLayoutButton!!.visibility = View.VISIBLE
            binding.slidingLayout!!.isEnabled = true
            checkEmptyHistoryForNoHistoryLabel()
        }

        // Set the sliding layout
        binding.slidingLayout!!.addPanelSlideListener(object : PanelSlideListener {
            override fun onPanelSlide(panel: View, slideOffset: Float) {
                if (slideOffset == 0f) { // If the panel got collapsed
                    binding.slidingLayout!!.scrollableView = binding.historyRecylcleView
                }
            }

            override fun onPanelStateChanged(
                panel: View,
                previousState: PanelState,
                newState: PanelState
            ) {
                if (newState == PanelState.ANCHORED) { // To prevent the panel from getting stuck in the middle
                    binding.slidingLayout!!.setPanelState(PanelState.EXPANDED)
                }
            }
        })

        // Set the history sliding layout button (click to open or close the history panel)
        binding.historySlidingLayoutButton!!.setOnClickListener {
            if (binding.slidingLayout!!.getPanelState() == PanelState.EXPANDED) {
                binding.slidingLayout!!.setPanelState(PanelState.COLLAPSED)
            } else {
                binding.slidingLayout!!.setPanelState(PanelState.EXPANDED)
            }
        }

        val textSizeAdjuster = TextSizeAdjuster(this)

        // Prevent the phone from sleeping (if option enabled)
        if (MyPreferences(this).preventPhoneFromSleepingMode) {
            view.keepScreenOn = true
        }

        if (resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            // scientific mode enabled by default in portrait mode (if option enabled)
            if (MyPreferences(this).scientificMode) {
                enableOrDisableScientistMode()
            }
        }

        // use radians instead of degrees by default (if option enabled)
        if (MyPreferences(this).useRadiansByDefault) {
            toggleDegreeMode()
        }

        // Focus by default
        binding.calcInput!!.requestFocus()

        // Makes the input take the whole width of the screen by default
        val screenWidthPX = resources.displayMetrics.widthPixels
        binding.calcInput!!.minWidth =
            screenWidthPX - (binding.calcInput!!.paddingRight + binding.calcInput!!.paddingLeft) // remove the paddingHorizontal

        // Do not clear after equal button if you move the cursor
        binding.calcInput!!.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun sendAccessibilityEvent(host: View, eventType: Int) {
                super.sendAccessibilityEvent(host, eventType)
                if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
                    isEqualLastAction = false
                }
                if (!binding.calcInput!!.isCursorVisible) {
                    binding.calcInput!!.isCursorVisible = true
                }
            }
        }

        // LongClick on result to copy it
        binding.resultDisplay!!.setOnLongClickListener {
            when {
                binding.resultDisplay!!.text.toString() != "" -> {
                    if (MyPreferences(this).longClickToCopyValue) {
                        val clipboardManager =
                            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        clipboardManager.setPrimaryClip(
                            ClipData.newPlainText(
                                R.string.copied_result.toString(),
                                binding.resultDisplay!!.text
                            )
                        )
                        // Only show a toast for Android 12 and lower.
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                            Toast.makeText(this, R.string.value_copied, Toast.LENGTH_SHORT).show()
                        true
                    } else {
                        false
                    }
                }

                else -> false
            }
        }

        // Handle changes into input to update resultDisplay
        binding.calcInput!!.addTextChangedListener(object : TextWatcher {
            private var beforeTextLength = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                beforeTextLength = s?.length ?: 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateResultDisplay()
                textSizeAdjuster.adjustTextSize(binding.calcInput!!,
                    TextSizeAdjuster.AdjustableTextType.Input
                )
            }

            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }
        })

        binding.resultDisplay!!.addTextChangedListener(object: TextWatcher {
            private var beforeTextLength = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                beforeTextLength = s?.length ?: 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                textSizeAdjuster.adjustTextSize(binding.resultDisplay!!,
                    TextSizeAdjuster.AdjustableTextType.Output
                )
            }

            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }
        })

        // Close the history panel if the user use the back button else close the app
        // https://developer.android.com/guide/navigation/navigation-custom-back#kotlin
        this.onBackPressedDispatcher.addCallback(this) {
            if (binding.slidingLayout!!.getPanelState() == PanelState.EXPANDED) {
                binding.slidingLayout!!.setPanelState(PanelState.COLLAPSED)
            } else {
                finish()
            }
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

                    // Hide the cursor before updating binding.calcInput!! to avoid weird cursor movement
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
                        calculationTmp.toString(),
                        isDegreeModeActivated
                    )

                // If result is a number and it is finite
                if (!(division_by_0 || domain_error || syntax_error || is_infinity || require_real_number)) {
                    // Round
                    calculationResult = roundResult(calculationResult)
                    var formattedResult = NumberFormatter.format(
                        calculationResult.toString().replace(".", decimalSeparatorSymbol),
                        decimalSeparatorSymbol,
                        groupingSeparatorSymbol,
                        numberingSystem
                    )

                    // Remove zeros at the end of the results (after point)
                    if (!MyPreferences(this@MainActivity).numberIntoScientificNotation || !(calculationResult >= BigDecimal(
                            9999
                        ) || calculationResult <= BigDecimal(0.1))
                    ) {
                        val resultSplited = calculationResult.toString().split('.')
                        if (resultSplited.size > 1) {
                            val resultPartAfterDecimalSeparator = resultSplited[1].trimEnd('0')
                            var resultWithoutZeros = resultSplited[0]
                            if (resultPartAfterDecimalSeparator != "") {
                                resultWithoutZeros =
                                    resultSplited[0] + "." + resultPartAfterDecimalSeparator
                            }
                            formattedResult = NumberFormatter.format(
                                resultWithoutZeros.replace(
                                    ".",
                                    decimalSeparatorSymbol
                                ), decimalSeparatorSymbol,
                                groupingSeparatorSymbol,
                                numberingSystem
                            )
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (formattedResult != calculation) {
                            binding.resultDisplay!!.text = formattedResult
                        } else {
                            binding.resultDisplay!!.text = ""
                        }
                    }

                    // Save to history if the option autoSaveCalculationWithoutEqualButton is enabled
                    if (MyPreferences(this@MainActivity).autoSaveCalculationWithoutEqualButton) {
                        if (calculation != formattedResult) {
                            val history = MyPreferences(this@MainActivity).getHistory()

                            if (isStillTheSameCalculation_autoSaveCalculationWithoutEqualOption) {
                                // If it's the same calculation as the previous one
                                // Get previous calculation and update it
                                val previousHistoryElement =
                                    MyPreferences(this@MainActivity).getHistoryElementById(
                                        lastHistoryElementId
                                    )
                                if (previousHistoryElement != null) {
                                    previousHistoryElement.calculation = calculation
                                    previousHistoryElement.result = formattedResult
                                    previousHistoryElement.time =
                                        System.currentTimeMillis().toString()
                                    MyPreferences(this@MainActivity).updateHistoryElementById(
                                        lastHistoryElementId,
                                        previousHistoryElement
                                    )
                                    withContext(Dispatchers.Main) {
                                        historyAdapter.updateHistoryElement(previousHistoryElement)
                                    }
                                }
                            } else {
                                // if it's a new calculation

                                // Store time
                                val currentTime = System.currentTimeMillis().toString()

                                // Save to history
                                val historyElementId = UUID.randomUUID().toString()
                                history.add(
                                    History(
                                        calculation = calculation,
                                        result = formattedResult,
                                        time = currentTime,
                                        id = historyElementId
                                    )
                                )

                                lastHistoryElementId = historyElementId
                                isStillTheSameCalculation_autoSaveCalculationWithoutEqualOption =
                                    true

                                MyPreferences(this@MainActivity).saveHistory(history)

                                // Update history variables in the UI
                                withContext(Dispatchers.Main) {
                                    historyAdapter.appendOneHistoryElement(
                                        History(
                                            calculation = calculation,
                                            result = formattedResult,
                                            time = currentTime,
                                            id = UUID.randomUUID()
                                                .toString() // Generate a random id
                                        )
                                    )

                                    // Remove former results if > historySize preference
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
                    }

                } else withContext(Dispatchers.Main) {
                    if (is_infinity && !division_by_0 && !domain_error && !require_real_number) {
                        if (calculationResult < BigDecimal.ZERO) binding.resultDisplay!!.text =
                            "-" + getString(
                                R.string.infinity
                            )
                        else binding.resultDisplay!!.text = getString(R.string.value_too_large)
                    } else {
                        withContext(Dispatchers.Main) {
                            binding.resultDisplay!!.text = ""
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.resultDisplay!!.text = ""
                }
            }
        }
    }

    // Displays a popup menu with options to insert double zeros ("00") or triple zeros ("000") into the specified EditText when the zero button is long-pressed.
    private fun showPopupMenu(zeroButton: Button) {
        val popupMenu = PopupMenu(this, zeroButton)
        popupMenu.menuInflater.inflate(R.menu.popup_menu_zero, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.option_double_zero -> {
                    updateDisplay(view, "00")
                    true
                }
                R.id.option_triple_zero -> {
                    updateDisplay(view, "000")
                    true
                }
                else -> false
            }
        }
        popupMenu.show()

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

    private fun setSwipeTouchHelperForRecyclerView() {
        val callBack = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun isItemViewSwipeEnabled(): Boolean {
                return MyPreferences(this@MainActivity).deleteHistoryOnSwipe
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                historyAdapter.removeHistoryElement(position)
                checkEmptyHistoryForNoHistoryLabel()
                deleteElementFromHistory(position)
            }
        }

        itemTouchHelper = ItemTouchHelper(callBack)
        itemTouchHelper.attachToRecyclerView(binding.historyRecylcleView)
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

    private fun roundResult(result: BigDecimal): BigDecimal {
        val numberPrecision = MyPreferences(this).numberPrecision!!.toInt()
        var newResult = result.setScale(numberPrecision, RoundingMode.HALF_EVEN)
        if (MyPreferences(this).numberIntoScientificNotation && (newResult >= BigDecimal(9999) || newResult <= BigDecimal(
                0.1
            ))
        ) {
            val scientificString = String.format(Locale.US, "%.4g", result)
            newResult = BigDecimal(scientificString)
        }

        // Fix how is displayed 0 with BigDecimal
        val tempResult = newResult.toString().replace("E-", "").replace("E", "")
        val allCharsEqualToZero = tempResult.all { it == '0' }
        if (
            allCharsEqualToZero
            || newResult.toString().startsWith("0E")
        ) {
            return BigDecimal.ZERO
        }

        return newResult
    }

    private fun enableOrDisableScientistMode() {
        if (binding.scientistModeRow2!!.visibility != View.VISIBLE) {
            binding.scientistModeRow2!!.visibility = View.VISIBLE
            binding.scientistModeRow3!!.visibility = View.VISIBLE
            binding.scientistModeSwitchButton?.setImageResource(R.drawable.ic_baseline_keyboard_arrow_up_24)
            binding.degreeTextView!!.visibility = View.VISIBLE
            if (isDegreeModeActivated) {
                binding.degreeButton!!.text = getString(R.string.radian)
                binding.degreeTextView!!.text = getString(R.string.degree)
            }
            else {
                binding.degreeButton!!.text = getString(R.string.degree)
                binding.degreeTextView!!.text = getString(R.string.radian)
            }
        } else {
            binding.scientistModeRow2!!.visibility = View.GONE
            binding.scientistModeRow3!!.visibility = View.GONE
            binding.scientistModeSwitchButton?.setImageResource(R.drawable.ic_baseline_keyboard_arrow_down_24)
            binding.degreeTextView!!.visibility = View.GONE
        }
    }

    // Switch between degree and radian mode
    private fun toggleDegreeMode() {
        isDegreeModeActivated = !isDegreeModeActivated
        if (isDegreeModeActivated) {
            binding.degreeButton!!.text = getString(R.string.radian)
            binding.degreeTextView!!.text = getString(R.string.degree)
        }
        else {
            binding.degreeButton!!.text = getString(R.string.degree)
            binding.degreeTextView!!.text = getString(R.string.radian)
        }

        // Flip the variable afterwards
        //isDegreeModeActivated = !isDegreeModeActivated
    }

    private fun deleteElementFromHistory(position: Int) {
        lifecycleScope.launch(Dispatchers.Default) {
            val history = MyPreferences(this@MainActivity).getHistory()
            history.removeAt(position)
            MyPreferences(this@MainActivity).saveHistory(history)
        }
    }
}