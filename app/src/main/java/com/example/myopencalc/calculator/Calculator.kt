package com.example.myopencalc.calculator.parser

import java.math.BigDecimal

var division_by_0 = false
var domain_error = false
var syntax_error = false
var is_infinity = false
var require_real_number = false

class Calculator(
    private val numberPrecisionDecimal: Int
) {
    fun evaluate(equation: String, isDegreeModeActivated: Any): BigDecimal {
        println("Equation BigDecimal : $equation")

        return object :Any() {

        }
    }

}