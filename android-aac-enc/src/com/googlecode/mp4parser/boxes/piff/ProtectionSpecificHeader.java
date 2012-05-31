package com.googlecode.mp4parser.boxes.piff;


import com.coremedia.iso.Hex;

import java.lang.Class;
import java.lang.IllegalAccessException;
import java.lang.InstantiationException;
import java.lang.Object;
import java.lang.Override;
import java.lang.RuntimeException;
import java.lang.String;
import java.lang.StringBuilder;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class ProtectionSpecificHeader {
    protected static Map<UUID, Class<? extends ProtectionSpecificHeader>> uuidRegistry = new HashMap<UUID, Class<? extends ProtectionSpecificHeader>>();
    ByteBuffer data;

    static {
        uuidRegistry.put(UUID.fromString("9A04F079-9840-4286-AB92-E65BE0885F95"), PlayReadyHeader.class);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProtectionSpecificHeader) {
            if (this.getClass().equals(obj.getClass())) {
                return data.equals(((ProtectionSpecificHeader) obj).data);
            }
        }
        return false;
    }

    public static ProtectionSpecificHeader createFor(UUID systemId, ByteBuffer bufferWrapper) {
        final Class<? extends ProtectionSpecificHeader> aClass = uuidRegistry.get(systemId);

        ProtectionSpecificHeader protectionSpecificHeader = new ProtectionSpecificHeader();
        if (aClass != null) {
            try {
                protectionSpecificHeader = aClass.newInstance();

            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        protectionSpecificHeader.parse(bufferWrapper);
        return protectionSpecificHeader;

    }

    public void parse(ByteBuffer buffer) {
        data = buffer;

    }

    public ByteBuffer getData() {
        return data;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ProtectionSpecificHeader");
        sb.append("{data=");
        ByteBuffer data = getData().duplicate();
        data.rewind();
        byte[] bytes = new byte[data.limit()];
        data.get(bytes);
        sb.append(Hex.encodeHex(bytes));
        sb.append('}');
        return sb.toString();
    }
}
