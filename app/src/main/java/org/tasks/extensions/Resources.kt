package org.tasks.extensions

import android.content.res.Resources

object Resources {
    fun Resources.getMutableStringList(id: Int): MutableList<String> =
        getStringArray(id).toMutableList()

    fun Resources.getMutableIntList(id: Int): MutableList<Int> {
        val typedArray = obtainTypedArray(id)
        val result = IntArray(typedArray.length())
        for (i in result.indices) {
            result[i] = typedArray.getResourceId(i, 0)
        }
        typedArray.recycle()
        return result.toMutableList()
    }
}
