package org.tasks.jobs

import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito
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
        val file1 = newFile("auto.180327-0000.json")
        val file2 = newFile("auto.180328-0000.json")
        val file3 = newFile("auto.180329-0000.json")
        assertEquals(emptyList<Any>(), BackupWork.getDeleteList(arrayOf(file2, file1, file3), 7))
    }

    @Test
    fun getDeleteFromNullFileList() {
        assertEquals(emptyList<Any>(), BackupWork.getDeleteList(null, 2))
    }

    @Test
    fun sortFiles() {
        val file1 = newFile("auto.180327-0000.json")
        val file2 = newFile("auto.180328-0000.json")
        val file3 = newFile("auto.180329-0000.json")
        assertEquals(
                listOf(file1), BackupWork.getDeleteList(arrayOf(file2, file1, file3), 2))
    }

    companion object {
        private fun newFile(name: String): File {
            val result = Mockito.mock(File::class.java)
            Mockito.`when`(result.name).thenReturn(name)
            return result
        }
    }
}