package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.MakeItEasy
import com.natpryce.makeiteasy.PropertyValue

internal object Maker {
    fun <T> make(instantiator: Instantiator<T>, vararg properties: PropertyValue<in T, *>?): T {
        return MakeItEasy.make(MakeItEasy.a(instantiator, *properties))
    }
}