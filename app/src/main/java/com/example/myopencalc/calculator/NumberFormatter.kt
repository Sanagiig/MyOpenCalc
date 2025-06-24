package com.example.myopencalc.calculator

object NumberFormatter {
    fun format(
        text: String,
        decimalSeparatorSymbol: String,
        groupingSeparatorSymbol: String,
        numberingSystem: NumberingSystem = NumberingSystem.INTERNATIONAL
    ): String {
        val textNoSeparator = removeSeparators(text, groupingSeparatorSymbol)
        val numbersList = extractString(textNoSeparator, decimalSeparatorSymbol)
        val numbersWithSeparators =
            addSeparators(numbersList, decimalSeparatorSymbol, groupingSeparatorSymbol, numberingSystem)

        val newString = StringBuilder()

        for (item in numbersWithSeparators) {
            newString.append(item)
        }

        return newString.toString()
    }

    private fun extractString(text: String, decimalSeparatorSymbol: String): List<String> {
        val result = mutableListOf<String>()
        var currentNumber = StringBuilder()

        for (char in text) {
            when {
                char.isDigit() || char == decimalSeparatorSymbol.single() -> {
                    currentNumber.append(char)
                }
                else -> {
                    if (currentNumber.isNotEmpty()) {
                        result.add(currentNumber.toString())
                        currentNumber = StringBuilder()
                    }
                    result.add(char.toString())
                }
            }
        }

        if (currentNumber.isNotEmpty()) {
            result.add(currentNumber.toString())
        }

        return result
    }

    private fun removeSeparators(text: String, groupingSeparatorSymbol: String): String {
        return text.replace(groupingSeparatorSymbol, "")
    }
}


enum class NumberingSystem(val value: Int, val description: String) {
    INTERNATIONAL(0, "International Numbering System"),
    INDIAN(1, "Indian Numbering System");

    companion object {
        fun getDescription(value: Int): String {
            return when (value) {
                0 -> INTERNATIONAL.description
                1 -> INDIAN.description
                else -> INTERNATIONAL.description
            }
        }

        fun Int.toNumberingSystem() : NumberingSystem {
            return when (this) {
                0 -> INTERNATIONAL
                1 -> INDIAN
                else -> INTERNATIONAL
            }
        }
    }
}