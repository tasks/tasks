package org.tasks.sync.microsoft

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface MicrosoftService {
    @GET("/v1.0/me/todo/lists")
    suspend fun getLists(): Response<TaskLists>

    @GET
    suspend fun paginateLists(@Url nextPage: String): Response<TaskLists>

    @POST("/v1.0/me/todo/lists")
    suspend fun createList(@Body body: RequestBody): Response<TaskLists.TaskList>

    @PATCH("/v1.0/me/todo/lists/{listId}")
    suspend fun updateList(
        @Path("listId") listId: String,
        @Body body: RequestBody
    ): Response<TaskLists.TaskList>

    @DELETE("/v1.0/me/todo/lists/{listId}")
    suspend fun deleteList(@Path("listId") listId: String)

    @GET("/v1.0/me/todo/lists/{listId}/tasks/delta")
    suspend fun getTasks(@Path("listId") listId: String): Response<Tasks>

    @GET
    suspend fun paginateTasks(@Url nextPage: String): Response<Tasks>

    @POST("/v1.0/me/todo/lists/{listId}/tasks")
    suspend fun createTask(
        @Path("listId") listId: String,
        @Body body: RequestBody
    ): Response<Tasks.Task>

    @PATCH("/v1.0/me/todo/lists/{listId}/tasks/{taskId}")
    suspend fun updateTask(
        @Path("listId") listId: String,
        @Path("taskId") taskId: String,
        @Body body: RequestBody
    ): Response<Tasks.Task>

    @DELETE("/v1.0/me/todo/lists/{listId}/tasks/{taskId}")
    suspend fun deleteTask(
        @Path("listId") listId: String,
        @Path("taskId") taskId: String
    ): Response<ResponseBody>
}