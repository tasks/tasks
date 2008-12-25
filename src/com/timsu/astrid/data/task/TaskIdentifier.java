package com.timsu.astrid.data.task;


/** A little class that identifies a task. For saving state and passing around */
public class TaskIdentifier  {
    private long id;

    public TaskIdentifier(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
