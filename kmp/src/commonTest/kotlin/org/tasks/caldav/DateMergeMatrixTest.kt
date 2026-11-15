package org.tasks.caldav

import kotlin.test.Test

class DateMergeMatrixTest {
    @Test
    fun unchangedCopiesKeepBothDates() {
        DateMergeMatrix.unchangedCopiesKeepBothDates()
    }

    @Test
    fun unparseableCopiesFailTheSameWayEveryTime() {
        DateMergeMatrix.unparseableCopiesFailTheSameWayEveryTime()
    }

    @Test
    fun unchangedCompletedCopiesKeepTheirCompletionDate() {
        DateMergeMatrix.unchangedCompletedCopiesKeepTheirCompletionDate()
    }

    @Test
    fun copiesWrittenByTasksOnlyLoseMilliseconds() {
        DateMergeMatrix.copiesWrittenByTasksOnlyLoseMilliseconds()
    }

    @Test
    fun pushingAnEditAndDownloadingAgainIsStable() {
        DateMergeMatrix.pushingAnEditAndDownloadingAgainIsStable()
    }

    @Test
    fun localEditsKeepTheirModificationDate() {
        DateMergeMatrix.localEditsKeepTheirModificationDate()
    }

    @Test
    fun localEditsLoseTheirModificationDateWhenTheCachedCopyHasNoLastModified() {
        DateMergeMatrix.localEditsLoseTheirModificationDateWhenTheCachedCopyHasNoLastModified()
    }

    @Test
    fun modificationDateMovesUpToCreatedWhenLastModifiedIsOlder() {
        DateMergeMatrix.modificationDateMovesUpToCreatedWhenLastModifiedIsOlder()
    }
}
