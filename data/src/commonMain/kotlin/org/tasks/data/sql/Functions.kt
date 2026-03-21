package org.tasks.data.sql

object Functions {
    fun upper(title: Field): Field = Field("UPPER($title)")

    fun now(): Field = Field("(strftime('%s','now')*1000)")
}