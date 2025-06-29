package com.example.myopencalc.calculator.parser

class Expression {

    fun addParenthesis(calculation: String): String {
        // Add ")" which lack
        var cleanCalculation = calculation
        var openParentheses = 0
        var closeParentheses = 0

        for (i in calculation.indices) {
            if (calculation[i] == '(') {
                openParentheses += 1
            } else if (calculation[i] == ')') {
                closeParentheses += 1
            }
        }

        if (closeParentheses < openParentheses) {
            for (i in 0 until openParentheses - closeParentheses) {
                cleanCalculation += ")"
            }
        }

        if(closeParentheses > openParentheses){
            syntax_error = true
        }

        return cleanCalculation
    }
}