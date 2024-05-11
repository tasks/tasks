package com.todoroo.andlib.sql

class Operator private constructor(private val operator: String) {
    override fun toString() = operator

    companion object {
        val eq = Operator("=")
        val isNull = Operator("IS NULL")
        val isNotNull = Operator("IS NOT NULL")
        val and = Operator("AND")
        val or = Operator("OR")
        val not = Operator("NOT")
        val like = Operator("LIKE")
        val `in` = Operator("IN")
        val exists = Operator("EXISTS")
        val gt = Operator(">")
        val lt = Operator("<")
        val lte = Operator("<=")
    }
}