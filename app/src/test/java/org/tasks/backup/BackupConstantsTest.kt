package org.tasks.backup

import org.junit.Assert.*
import org.junit.Test
import org.tasks.time.DateTime

class BackupConstantsTest {
    @Test
    fun autoBackupMatchesFilename() {
        assertTrue(BackupConstants.isBackupFile("auto.200909-0003.json"))
    }

    @Test
    fun userBackupMatchesFilename() {
        assertTrue(BackupConstants.isBackupFile("user.200909-1503.json"))
    }

    @Test
    fun ignoreCopiedFile() {
        assertFalse(BackupConstants.isBackupFile("user.200909-1503 (1).json"))
    }

    @Test
    fun getTimestampFromAutoBackup() {
        assertEquals(
                DateTime(2020, 9, 10, 15, 3).millis,
                BackupConstants.getTimestampFromFilename("auto.200910-1503.json")
        )
    }

    @Test
    fun getTimestampFromUserBackup() {
        assertEquals(
                DateTime(2020, 9, 10, 15, 3).millis,
                BackupConstants.getTimestampFromFilename("user.200910-1503.json")
        )
    }
}