package org.tasks.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.utility.DateUtilities;

import org.tasks.backup.XmlReader;
import org.tasks.backup.XmlWriter;

@Entity(tableName = "google_tasks")
public class GoogleTask {

    public static final String KEY = "gtasks"; //$NON-NLS-1$

    @Deprecated
    public static final Table TABLE = new Table("google_tasks", null);

    @Deprecated
    public static final Property.IntegerProperty INDENT = new Property.IntegerProperty(GoogleTask.TABLE, "indent");

    @Deprecated
    public static final Property.LongProperty ORDER = new Property.LongProperty(GoogleTask.TABLE, "`order`");

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private long id;

    @ColumnInfo(name = "task")
    private long task;

    @ColumnInfo(name = "remote_id")
    private String remoteId = "";

    @ColumnInfo(name = "list_id")
    private String listId = "";

    @ColumnInfo(name = "parent")
    private long parent;

    @ColumnInfo(name = "indent")
    private int indent;

    @ColumnInfo(name = "order")
    private long order;

    @ColumnInfo(name = "remote_order")
    private long remoteOrder;

    @ColumnInfo(name = "last_sync")
    private long lastSync;

    @ColumnInfo(name = "deleted")
    private long deleted;

    @Ignore
    private boolean suppressSync;

    public GoogleTask() {

    }

    @Ignore
    public GoogleTask(long task, String listId) {
        this.task = task;
        this.order = DateUtilities.now();
        this.listId = listId;
    }

    @Ignore
    public GoogleTask(XmlReader xml) {
        remoteId = xml.readString("remote_id");
        listId = xml.readString("list_id");
        parent = xml.readLong("parent");
        indent = xml.readInteger("indent");
        order = xml.readLong("order");
        remoteOrder = xml.readLong("remote_order");
        lastSync = xml.readLong("last_sync");
        deleted = xml.readLong("deleted");
    }

    public void writeToXml(XmlWriter xml) {
        xml.writeString("remote_id", remoteId);
        xml.writeString("list_id", listId);
        xml.writeLong("parent", parent);
        xml.writeInteger("indent", indent);
        xml.writeLong("order", order);
        xml.writeLong("remote_order", remoteOrder);
        xml.writeLong("last_sync", lastSync);
        xml.writeLong("deleted", deleted);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTask() {
        return task;
    }

    public void setTask(long task) {
        this.task = task;
    }

    public String getRemoteId() {
        return remoteId;
    }

    public void setRemoteId(String remoteId) {
        this.remoteId = remoteId;
    }

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public long getParent() {
        return parent;
    }

    public void setParent(long parent) {
        this.parent = parent;
    }

    public int getIndent() {
        return indent;
    }

    public void setIndent(int indent) {
        this.indent = indent;
    }

    public long getOrder() {
        return order;
    }

    public void setOrder(long order) {
        this.order = order;
    }

    public long getRemoteOrder() {
        return remoteOrder;
    }

    public void setRemoteOrder(long remoteOrder) {
        this.remoteOrder = remoteOrder;
    }

    public long getLastSync() {
        return lastSync;
    }

    public void setLastSync(long lastSync) {
        this.lastSync = lastSync;
    }

    public long getDeleted() {
        return deleted;
    }

    public void setDeleted(long deleted) {
        this.deleted = deleted;
    }

    public boolean isSuppressSync() {
        return suppressSync;
    }

    public void setSuppressSync(boolean suppressSync) {
        this.suppressSync = suppressSync;
    }

    @Override
    public String toString() {
        return "GoogleTask{" +
                "id=" + id +
                ", task=" + task +
                ", remoteId='" + remoteId + '\'' +
                ", listId='" + listId + '\'' +
                ", parent=" + parent +
                ", indent=" + indent +
                ", order=" + order +
                ", remoteOrder=" + remoteOrder +
                ", lastSync=" + lastSync +
                ", deleted=" + deleted +
                '}';
    }
}
