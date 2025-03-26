package com.todoroo.astrid.activity

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.extensions.isFromHistory

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @Test
    fun newTaskIsNotFromHistory() {
        assertFalse(Intent().setFlags(FLAG_ACTIVITY_NEW_TASK).isFromHistory)
    }

    @Test
    fun oldTaskIsNotFromHistory() {
        assertFalse(Intent().setFlags(FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY).isFromHistory)
    }

    @Test
    fun newTaskIsFromHistory() {
        assertTrue(
                Intent()
                        .setFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
                        .isFromHistory)
    }
}