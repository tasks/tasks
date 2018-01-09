package org.tasks.backup;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

import timber.log.Timber;

import static com.todoroo.astrid.backup.TasksXmlExporter.XML_NULL;

public class XmlWriter {
    private final XmlSerializer xml;

    public XmlWriter(XmlSerializer xml) {
        this.xml = xml;
    }

    public void writeLong(String name, Long value) {
        try {
            String valueString = (value == null) ? XML_NULL : value.toString();
            xml.attribute(null, name, valueString);
        } catch (UnsupportedOperationException e) {
            // didn't read this value, do nothing
            Timber.e(e, e.getMessage());
        } catch (IllegalArgumentException | IOException | IllegalStateException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeString(String name, String value) {
        try {
            if(value != null) {
                xml.attribute(null, name, value);
            }
        } catch (UnsupportedOperationException e) {
            // didn't read this value, do nothing
            Timber.v(e, e.getMessage());
        } catch (IllegalArgumentException | IOException | IllegalStateException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeInteger(String name, Integer value) {
        try {
            String valueString = (value == null) ? XML_NULL : value.toString();
            xml.attribute(null, name, valueString);
        } catch (UnsupportedOperationException e) {
            // didn't read this value, do nothing
            Timber.e(e, e.getMessage());
        } catch (IllegalArgumentException | IOException | IllegalStateException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeDouble(String name, Double value) {
        try {
            String valueString = (value == null) ? XML_NULL : value.toString();
            xml.attribute(null, name, valueString);
        } catch (UnsupportedOperationException e) {
            // didn't read this value, do nothing
            Timber.e(e, e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
