package org.tasks.wear

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import org.tasks.GrpcProto.Settings
import java.io.InputStream
import java.io.OutputStream

object SettingsSerializer : Serializer<Settings> {
    override val defaultValue: Settings
        get() = Settings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Settings =
        try {
            Settings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }

    override suspend fun writeTo(t: Settings, output: OutputStream) {
        t.writeTo(output)
    }
}
