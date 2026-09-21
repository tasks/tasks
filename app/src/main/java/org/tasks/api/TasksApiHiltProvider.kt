package org.tasks.api

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.tasks.analytics.Analytics
import org.tasks.analytics.Firebase
import org.tasks.data.db.Database

class TasksApiHiltProvider : TasksApiProvider() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ApiEntryPoint {
        val database: Database
        val queryEngine: ApiQueryEngine
        val writer: ApiWriter
        val firebase: Firebase
    }

    override val dependencies: Dependencies
        get() = EntryPointAccessors
            .fromApplication(context!!.applicationContext, ApiEntryPoint::class.java)
            .let {
                object : Dependencies {
                    override val database: Database = it.database
                    override val queryEngine: ApiQueryEngine = it.queryEngine
                    override val writer: ApiWriter = it.writer
                    override val analytics: Analytics = it.firebase
                }
            }
}
