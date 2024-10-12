package org.tasks.wear

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import org.tasks.GrpcProto.Tasks
import java.io.InputStream
import java.io.OutputStream

object TasksSerializer : Serializer<Tasks> {
    override val defaultValue: Tasks
        get() = Tasks.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Tasks =
        try {
            Tasks.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }

    override suspend fun writeTo(t: Tasks, output: OutputStream) {
        t.writeTo(output)
    }
}