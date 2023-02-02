package org.tasks.makers

import com.natpryce.makeiteasy.Instantiator
import com.natpryce.makeiteasy.Property
import com.natpryce.makeiteasy.Property.newProperty
import com.natpryce.makeiteasy.PropertyValue
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.makers.Maker.make

object CaldavAccountMaker {
    val ID: Property<CaldavAccount, Long> = newProperty()
    val NAME: Property<CaldavAccount, String> = newProperty()
    val UUID: Property<CaldavAccount, String> = newProperty()
    val ACCOUNT_TYPE: Property<CaldavAccount, Int> = newProperty()

    private val instantiator = Instantiator { lookup ->
        CaldavAccount().apply {
            id = lookup.valueOf(ID, 0L)
            name = lookup.valueOf(NAME, null as String?)
            uuid = lookup.valueOf(UUID, "account")
            accountType = lookup.valueOf(ACCOUNT_TYPE, TYPE_CALDAV)
        }
    }

    fun newCaldavAccount(vararg properties: PropertyValue<in CaldavAccount?, *>): CaldavAccount {
        return make(instantiator, *properties)
    }
}