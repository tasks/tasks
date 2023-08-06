package com.todoroo.astrid.core

import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.api.BooleanCriterion
import com.todoroo.astrid.api.CustomFilterCriterion
import com.todoroo.astrid.api.MultipleSelectCriterion
import com.todoroo.astrid.api.TextInputCriterion
import com.todoroo.astrid.helper.UUIDHelper

class CriterionInstance {
    lateinit var criterion: CustomFilterCriterion
    var selectedIndex = -1
    var selectedText: String? = null
    var type = TYPE_INTERSECT
    var end = 0
    var start = 0
    var max = 0
    var id: String = UUIDHelper.newUUID()
        private set

    constructor()

    constructor(other: CriterionInstance) {
        id = other.id
        criterion = other.criterion
        selectedIndex = other.selectedIndex
        selectedText = other.selectedText
        type = other.type
        end = other.end
        start = other.start
        max = other.max
    }

    // $NON-NLS-1$
    val titleFromCriterion: String
        get() {
            when (criterion) {
                is MultipleSelectCriterion -> {
                    if (selectedIndex >= 0 && (criterion as MultipleSelectCriterion).entryTitles != null && selectedIndex < (criterion as MultipleSelectCriterion).entryTitles.size) {
                        val title = (criterion as MultipleSelectCriterion).entryTitles[selectedIndex]
                        return criterion.text.replace("?", title)
                    }
                    return criterion.text
                }

                is TextInputCriterion -> {
                    return if (selectedText == null) {
                        criterion.text
                    } else criterion.text.replace("?", selectedText!!)
                }

                is BooleanCriterion -> {
                    return criterion.name
                }
                // $NON-NLS-1$
                else -> throw UnsupportedOperationException("Unknown criterion type")
            }
        }

    // $NON-NLS-1$
    val valueFromCriterion: String?
        get() {
            if (type == TYPE_UNIVERSE) {
                return null
            }
            when (criterion) {
                is MultipleSelectCriterion -> {
                    return if (selectedIndex >= 0 && (criterion as MultipleSelectCriterion).entryValues != null && selectedIndex < (criterion as MultipleSelectCriterion).entryValues.size) {
                        (criterion as MultipleSelectCriterion).entryValues[selectedIndex]
                    } else criterion.text
                }

                is TextInputCriterion -> {
                    return selectedText
                }

                is BooleanCriterion -> {
                    return criterion.name
                }
                // $NON-NLS-1$
                else -> throw UnsupportedOperationException("Unknown criterion type")
            }
        }

    private fun serialize(): String {
        // criterion|entry|text|type|sql
        return listOf(
                escape(criterion.identifier),
                escape(valueFromCriterion),
                escape(criterion.text),
                type,
                criterion.sql ?: "")
                .joinToString(AndroidUtilities.SERIALIZATION_SEPARATOR)
    }

    override fun toString(): String {
        return "CriterionInstance(criterion=$criterion, selectedIndex=$selectedIndex, selectedText=$selectedText, type=$type, end=$end, start=$start, max=$max, id='$id')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CriterionInstance) return false

        if (criterion != other.criterion) return false
        if (selectedIndex != other.selectedIndex) return false
        if (selectedText != other.selectedText) return false
        if (type != other.type) return false
        if (end != other.end) return false
        if (start != other.start) return false
        if (max != other.max) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = criterion.hashCode()
        result = 31 * result + selectedIndex
        result = 31 * result + (selectedText?.hashCode() ?: 0)
        result = 31 * result + type
        result = 31 * result + end
        result = 31 * result + start
        result = 31 * result + max
        result = 31 * result + id.hashCode()
        return result
    }

    companion object {
        const val TYPE_ADD = 0
        const val TYPE_SUBTRACT = 1
        const val TYPE_INTERSECT = 2
        const val TYPE_UNIVERSE = 3

        private fun escape(item: String?): String {
            return item?.replace(
                    AndroidUtilities.SERIALIZATION_SEPARATOR, AndroidUtilities.SEPARATOR_ESCAPE)
                    ?: "" // $NON-NLS-1$
        }

        fun serialize(criterion: List<CriterionInstance>): String {
            return criterion
                    .joinToString("\n") { it.serialize() }
                    .trim()
        }
    }
}