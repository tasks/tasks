/*
 * Copyright 2011 castLabs, Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.mp4parser.boxes.ultraviolet;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;

/**
 *
 */
public class BaseLocationBox extends AbstractFullBox {
    String baseLocation = "";
    String purchaseLocation = "";

    public BaseLocationBox() {
        super("bloc");
    }

    public BaseLocationBox(String baseLocation, String purchaseLocation) {
        super("bloc");
        this.baseLocation = baseLocation;
        this.purchaseLocation = purchaseLocation;
    }

    public String getBaseLocation() {
        return baseLocation;
    }

    public void setBaseLocation(String baseLocation) {
        this.baseLocation = baseLocation;
    }

    public String getPurchaseLocation() {
        return purchaseLocation;
    }

    public void setPurchaseLocation(String purchaseLocation) {
        this.purchaseLocation = purchaseLocation;
    }

    @Override
    protected long getContentSize() {
        return 1028;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        baseLocation = IsoTypeReader.readString(content);
        content.get(new byte[256 - Utf8.utf8StringLengthInBytes(baseLocation) - 1]);
        purchaseLocation = IsoTypeReader.readString(content);
        content.get(new byte[256 - Utf8.utf8StringLengthInBytes(purchaseLocation) - 1]);
        content.get(new byte[512]);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        byteBuffer.put(Utf8.convert(baseLocation));
        byteBuffer.put(new byte[256 - Utf8.utf8StringLengthInBytes(baseLocation)]); // string plus term zero
        byteBuffer.put(Utf8.convert(purchaseLocation));
        byteBuffer.put(new byte[256 - Utf8.utf8StringLengthInBytes(purchaseLocation)]); // string plus term zero
        byteBuffer.put(new byte[512]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseLocationBox that = (BaseLocationBox) o;

        if (baseLocation != null ? !baseLocation.equals(that.baseLocation) : that.baseLocation != null) return false;
        if (purchaseLocation != null ? !purchaseLocation.equals(that.purchaseLocation) : that.purchaseLocation != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = baseLocation != null ? baseLocation.hashCode() : 0;
        result = 31 * result + (purchaseLocation != null ? purchaseLocation.hashCode() : 0);
        return result;
    }
}
