/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.timsu.astrid.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.task.TaskController;

/**
 * Interface for views that are displayed from the main view page.
 *
 * @author timsu
 */
abstract public class SubActivity {
	private TaskList parent;
	int code;
	private View view;

	public SubActivity(TaskList parent, int code, View view) {
		this.parent = parent;
		this.code = code;
		this.view = view;
		view.setTag(this);
	}

	// --- pass-through to activity listeners

	/** Called when this subactivity is displayed to the user */
	void onDisplay(Bundle variables) {
		//
	}

	boolean onPrepareOptionsMenu(Menu menu) {
		return false;
	}

	void onActivityResult(int requestCode, int resultCode, Intent data) {
		//
	}

	boolean onMenuItemSelected(int featureId, MenuItem item) {
		return false;
	}


	void onWindowFocusChanged(boolean hasFocus) {
		//
	}

	boolean onKeyDown(int keyCode, KeyEvent event) {
		return false;
	}

	void onSaveInstanceState(Bundle outState) {
	    //
	}

	Object onRetainNonConfigurationInstance() {
	    return null;
	}

	// --- pass-through to activity methods

	public Resources getResources() {
		return parent.getResources();
	}

	public View findViewById(int id) {
		return view.findViewById(id);
	}

	public void startManagingCursor(Cursor c) {
		parent.startManagingCursor(c);
	}

	public void setTitle(CharSequence title) {
		parent.setTitle(title);
	}

	public void closeActivity() {
		parent.finish();
	}

	public void launchActivity(Intent intent, int requestCode) {
		parent.startActivityForResult(intent, requestCode);
	}

	public Object getLastNonConfigurationInstance() {
	    return parent.getLastNonConfigurationInstance();
	}

	// --- helper methods

	public Activity getParent() {
		return parent;
	}

	public TaskController getTaskController() {
		return parent.taskController;
	}

	public TagController getTagController() {
		return parent.tagController;
	}

	public View.OnTouchListener getGestureListener() {
		return parent.gestureListener;
	}

	public void switchToActivity(int activity, Bundle state) {
		parent.switchToActivity(activity, state);
	}

	// --- internal methods

	protected int getActivityCode() {
		return code;
	}

	protected View getView() {
		return view;
	}
}