package org.tasks.data.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS

class CaldavAccountTest {
    @Test
    fun caldavBackendsPushTheParentTheyAreGiven() {
        listOf(TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE, TYPE_OPENTASKS).forEach {
            assertTrue("account type $it", CaldavAccount.pushesRemoteParent(it))
        }
    }

    @Test
    fun microsoftWorksItsHierarchyOutByComparingTheTwo() {
        assertFalse(CaldavAccount.pushesRemoteParent(TYPE_MICROSOFT))
        assertFalse(CaldavAccount(accountType = TYPE_MICROSOFT).pushesRemoteParent)
    }

    @Test
    fun googleTasksPushesItsHierarchySomewhereElseEntirely() {
        assertFalse(CaldavAccount.pushesRemoteParent(TYPE_GOOGLE_TASKS))
    }

    @Test
    fun aLocalListKeepsTheColumnInStepEvenThoughNothingPushesIt() {
        assertTrue(CaldavAccount.pushesRemoteParent(TYPE_LOCAL))
    }

    @Test
    fun anAccountTypeNobodyRecognisesIsTreatedAsCaldav() {
        assertTrue(CaldavAccount.pushesRemoteParent(null))
    }
}
