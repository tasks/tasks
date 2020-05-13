package org.tasks.jobs

import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito
import org.tasks.date.DateTimeUtils
import org.tasks.time.DateTime
import java.io.File

class BackupWorkTest {
    @Test
    fun filterExcludesXmlFiles() {
        assertFalse(BackupWork.FILE_FILTER.accept(File("/a/b/c/d/auto.180329-0001.xml")))
    }

    @Test
    fun filterIncludesJsonFiles() {
        assertTrue(BackupWork.FILE_FILTER.accept(File("/a/b/c/d/auto.180329-0001.json")))
    }

    @Test
    fun getDeleteKeepAllFiles() {
        val file1 = newFile(DateTimeUtils.newDate(2018, 3, 27))
        val file2 = newFile(DateTimeUtils.newDate(2018, 3, 28))
        val file3 = newFile(DateTimeUtils.newDate(2018, 3, 29))
        assertEquals(emptyList<Any>(), BackupWork.getDeleteList(arrayOf(file2, file1, file3), 7))
    }

    @Test
    fun getDeleteFromNullFileList() {
        assertEquals(emptyList<Any>(), BackupWork.getDeleteList(null, 2))
    }

    @Test
    fun sortFiles() {
        val file1 = newFile(DateTimeUtils.newDate(2018, 3, 27))
        val file2 = newFile(DateTimeUtils.newDate(2018, 3, 28))
        val file3 = newFile(DateTimeUtils.newDate(2018, 3, 29))
        assertEquals(
                listOf(file1), BackupWork.getDeleteList(arrayOf(file2, file1, file3), 2))
    }

    companion object {
        private fun newFile(lastModified: DateTime): File {
            val result = Mockito.mock(File::class.java)
            Mockito.`when`(result.lastModified()).thenReturn(lastModified.millis)
            return result
        }
    }
}