package org.tasks.wear

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import org.tasks.GrpcProto.LastUpdate
import java.io.InputStream
import java.io.OutputStream

object LastUpdateSerializer : Serializer<LastUpdate> {
    override val defaultValue: LastUpdate
        get() = LastUpdate.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): LastUpdate =
        try {
            LastUpdate.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }

    override suspend fun writeTo(t: LastUpdate, output: OutputStream) {
        t.writeTo(output)
    }
}
