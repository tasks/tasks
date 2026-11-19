package org.tasks.sync.microsoft

import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT

class MsalAccountSetupTest {
    private val alice = MsalIdentity(subject = "1234", username = "alice@example.com")

    @Test
    fun anAccountStoredUnderTheSubjectMatches() {
        assertSame(alice, listOf(alice).findFor("1234", "alice@example.com") { it })
    }

    @Test
    fun anAccountStoredUnderItsEmailMatches() {
        assertSame(alice, listOf(alice).findFor("alice@example.com", "alice@example.com") { it })
    }

    @Test
    fun anAccountWithoutClaimsMatchesItsName() {
        val noClaims = MsalIdentity(subject = null, username = "alice@example.com")

        assertSame(noClaims, listOf(noClaims).findFor("1234", "alice@example.com") { it })
    }

    @Test
    fun theSubjectWinsOverAnotherAccountsEmail() {
        val other = MsalIdentity(subject = "5678", username = "1234")

        assertSame(alice, listOf(other, alice).findFor("1234", null) { it })
    }

    @Test
    fun theSubjectWinsOverTheName() {
        val other = MsalIdentity(subject = "5678", username = "alice@example.com")

        assertSame(alice, listOf(other, alice).findFor("1234", "alice@example.com") { it })
    }

    @Test
    fun theEmailIsMatchedExactly() {
        assertNull(listOf(alice).findFor("Alice@Example.com", "Alice@Example.com") { it })
    }

    @Test
    fun nothingMatchesAnAccountWithoutAUsername() {
        assertNull(listOf(alice).findFor(null, "alice@example.com") { it })
    }

    @Test
    fun subjectsAreKeyedByEmail() {
        assertEquals(mapOf("alice@example.com" to "1234"), listOf(alice).subjectsByUsername())
    }

    @Test
    fun anEmailSharedByTwoAccountsHasNoSubject() {
        val work = MsalIdentity(subject = "5678", username = "alice@example.com")

        assertEquals(emptyMap<String, String>(), listOf(alice, work).subjectsByUsername())
    }

    @Test
    fun anAccountWithoutASubjectIsLeftOut() {
        val legacy = MsalIdentity(subject = null, username = "alice@example.com")

        assertEquals(emptyMap<String, String>(), listOf(legacy).subjectsByUsername())
    }

    @Test
    fun aNewAccountIsStoredUnderTheSubject(): Unit = runBlocking {
        val caldavDao = mock<CaldavDao>()
        var added = false

        setupMsalMicrosoftAccount(MsalSignIn("alice@example.com", "1234"), caldavDao) { added = true }

        verifyBlocking(caldavDao) {
            insert(argThat<CaldavAccount> {
                accountType == TYPE_MICROSOFT && username == "1234" && name == "alice@example.com"
            })
        }
        assertEquals(true, added)
    }

    @Test
    fun aNewAccountWithoutASubjectIsStoredUnderItsEmail(): Unit = runBlocking {
        val caldavDao = mock<CaldavDao>()

        setupMsalMicrosoftAccount(MsalSignIn("alice@example.com", null), caldavDao)

        verifyBlocking(caldavDao) {
            insert(argThat<CaldavAccount> { username == "alice@example.com" })
        }
    }

    @Test
    fun signingInAgainMovesAnEmailAccountOntoTheSubjectAndUpdatesTheName(): Unit = runBlocking {
        val existing = account(2L, "old@example.com")
        val caldavDao = mock<CaldavDao> {
            onBlocking { findMicrosoftAccount("1234", "alice@example.com", null) } doReturn existing
        }
        var added = false

        setupMsalMicrosoftAccount(MsalSignIn("alice@example.com", "1234"), caldavDao) { added = true }

        verifyBlocking(caldavDao) { setMicrosoftReauth(eq(2L), eq("1234"), eq("alice@example.com"), eq("")) }
        verifyBlocking(caldavDao, never()) { insert(any<CaldavAccount>()) }
        assertEquals(false, added)
    }

    @Test
    fun signingInAgainWithoutASubjectKeepsTheStoredUsername(): Unit = runBlocking {
        val caldavDao = mock<CaldavDao> {
            onBlocking { findMicrosoftAccount("alice@example.com", "alice@example.com", null) } doReturn
                account(2L, "Alice@Example.com")
        }

        setupMsalMicrosoftAccount(MsalSignIn("alice@example.com", null), caldavDao)

        verifyBlocking(caldavDao) {
            setMicrosoftReauth(eq(2L), eq("Alice@Example.com"), eq("alice@example.com"), eq(""))
        }
    }

    private fun account(id: Long, username: String) = CaldavAccount(
        id = id,
        accountType = TYPE_MICROSOFT,
        username = username,
    )

    @Test
    fun simultaneousSignInsStoreOneAccount(): Unit = runBlocking {
        var stored: CaldavAccount? = null
        val caldavDao = mock<CaldavDao> {
            onBlocking { findMicrosoftAccount(any(), any(), isNull()) } doSuspendableAnswer {
                val found = stored
                yield()
                found
            }
            onBlocking { insert(any<CaldavAccount>()) } doSuspendableAnswer {
                stored = it.getArgument(0)
                1L
            }
        }

        List(2) {
            launch { setupMsalMicrosoftAccount(MsalSignIn("alice@example.com", "1234"), caldavDao) }
        }.joinAll()

        verifyBlocking(caldavDao, times(1)) { insert(any<CaldavAccount>()) }
    }
}
