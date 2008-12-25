package com.timsu.astrid.data.task;

import java.util.Date;

import com.timsu.astrid.data.enums.Importance;

import android.database.Cursor;



/** Fields that you would want to edit in the TaskModel */
public class TaskModelForEdit extends AbstractTaskModel {

    static String[] FIELD_LIST = new String[] {
        NAME,
        IMPORTANCE,
        ESTIMATED_SECONDS,
        ELAPSED_SECONDS,
        DEFINITE_DUE_DATE,
        PREFERRED_DUE_DATE,
        HIDDEN_UNTIL,
        BLOCKING_ON,
        NOTES,
    };

    // --- constructors

    public TaskModelForEdit() {
        super();
        setCreationDate(new Date());
    }

    public TaskModelForEdit(TaskIdentifier identifier, Cursor cursor) {
        super(identifier, cursor);
    }

    // --- getters and setters

    @Override
    public Date getDefiniteDueDate() {
        return super.getDefiniteDueDate();
    }

    @Override
    public Integer getEstimatedSeconds() {
        return super.getEstimatedSeconds();
    }

    @Override
    public Integer getElapsedSeconds() {
        return super.getElapsedSeconds();
    }

    @Override
    public Date getHiddenUntil() {
        return super.getHiddenUntil();
    }

    @Override
    public Importance getImportance() {
        return super.getImportance();
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public String getNotes() {
        return super.getNotes();
    }

    @Override
    public Date getPreferredDueDate() {
        return super.getPreferredDueDate();
    }

    @Override
    public TaskIdentifier getBlockingOn() {
        return super.getBlockingOn();
    }

    @Override
    public void setDefiniteDueDate(Date definiteDueDate) {
        super.setDefiniteDueDate(definiteDueDate);
    }

    @Override
    public void setEstimatedSeconds(Integer estimatedSeconds) {
        super.setEstimatedSeconds(estimatedSeconds);
    }

    @Override
    public void setElapsedSeconds(int elapsedSeconds) {
        super.setElapsedSeconds(elapsedSeconds);
    }

    @Override
    public void setHiddenUntil(Date hiddenUntil) {
        super.setHiddenUntil(hiddenUntil);
    }

    @Override
    public void setImportance(Importance importance) {
        super.setImportance(importance);
    }

    @Override
    public void setName(String name) {
        super.setName(name);
    }

    @Override
    public void setNotes(String notes) {
        super.setNotes(notes);
    }

    @Override
    public void setPreferredDueDate(Date preferredDueDate) {
        super.setPreferredDueDate(preferredDueDate);
    }

    @Override
    public void setBlockingOn(TaskIdentifier blockingOn) {
        super.setBlockingOn(blockingOn);
    }
}
