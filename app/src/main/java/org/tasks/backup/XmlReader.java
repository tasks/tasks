package org.tasks.backup;

import com.todoroo.astrid.backup.TasksXmlExporter;

import org.xmlpull.v1.XmlPullParser;

public class XmlReader {

    public interface ValueWriter<T> {
        void write(T value);
    }

    private final XmlPullParser xpp;

    public XmlReader(XmlPullParser xpp) {
        this.xpp = xpp;
    }

    public void readLong(String name, ValueWriter<Long> writer) {
        String value = xpp.getAttributeValue(null, name);
        if(value != null) {
            writer.write(TasksXmlExporter.XML_NULL.equals(value) ?
                    null : Long.parseLong(value));
        }
    }

    public void readInteger(String name, ValueWriter<Integer> writer) {
        String value = xpp.getAttributeValue(null, name);
        if(value != null) {
            writer.write(TasksXmlExporter.XML_NULL.equals(value) ?
                    null : Integer.parseInt(value));
        }
    }

    public void readString(String name, ValueWriter<String> writer) {
        String value = xpp.getAttributeValue(null, name);
        if (value != null) {
            writer.write(value);
        }
    }

    public void readDouble(String name, ValueWriter<Double> writer) {
        String value = xpp.getAttributeValue(null, name);
        if (value != null) {
            writer.write(TasksXmlExporter.XML_NULL.equals(value) ?
                    null : Double.parseDouble(value));
        }
    }
}
