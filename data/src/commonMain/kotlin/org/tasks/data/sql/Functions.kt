package org.tasks.data.sql

object Functions {
    @JvmStatic fun upper(title: Field): Field = Field("UPPER($title)")

    @JvmStatic fun now(): Field = Field("(strftime('%s','now')*1000)")
}