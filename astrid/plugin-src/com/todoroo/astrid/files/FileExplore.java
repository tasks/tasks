/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DialogUtilities;

/**
 * Based on the Android-File-Explore project by Manish Burman
 * https://github.com/mburman/Android-File-Explore
 *
 */
public class FileExplore extends Activity {

	// Stores names of traversed directories
	ArrayList<String> str = new ArrayList<String>();

	// Check if the first level of the directory structure is the one showing
	private Boolean firstLvl = true;

	private static final String TAG = "F_PATH"; //$NON-NLS-1$

	public static final String RESULT_FILE_SELECTED = "fileSelected"; //$NON-NLS-1$

	public static final String RESULT_DIR_SELECTED = "dirSelected"; //$NON-NLS-1$

	public static final String EXTRA_DIRECTORIES_SELECTABLE = "directoriesSelectable"; //$NON-NLS-1$

	private Item[] fileList;
	private File path;
	private String chosenFile;
	private static final int DIALOG_LOAD_FILE = 1000;
	private String upString;

	private boolean directoryMode;

	private ListAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
		    path = new File(Environment.getExternalStorageDirectory().toString());
		else
		    path = Environment.getRootDirectory();

		loadFileList();

		directoryMode = getIntent().getBooleanExtra(EXTRA_DIRECTORIES_SELECTABLE, false);

		showDialog(DIALOG_LOAD_FILE);
		upString = getString(R.string.file_browser_up);
		Log.d(TAG, path.getAbsolutePath());

	}

	private void loadFileList() {
		try {
			path.mkdirs();
		} catch (SecurityException e) {
		    DialogUtilities.okDialog(this, getString(R.string.file_browser_err_permissions), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
		}

		// Checks whether path exists
		if (path.exists()) {
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					File sel = new File(dir, filename);
					// Filters based on whether the file is hidden or not
					return (sel.isFile() || sel.isDirectory())
							&& !sel.isHidden();

				}
			};

			String[] fList = path.list(filter);
			fileList = new Item[fList.length];
			for (int i = 0; i < fList.length; i++) {
				fileList[i] = new Item(fList[i], R.drawable.file_icon);

				// Convert into file path
				File sel = new File(path, fList[i]);

				// Set drawables
				if (sel.isDirectory()) {
					fileList[i].icon = R.drawable.directory_icon;
				}
			}

			if (!firstLvl) {
				Item temp[] = new Item[fileList.length + 1];
				for (int i = 0; i < fileList.length; i++) {
					temp[i + 1] = fileList[i];
				}
				temp[0] = new Item(upString, R.drawable.directory_up);
				fileList = temp;
			}
		} else {
			Log.e(TAG, "path does not exist"); //$NON-NLS-1$
		}

		adapter = new ArrayAdapter<Item>(this,
				android.R.layout.select_dialog_item, android.R.id.text1,
				fileList) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				// creates view
				View view = super.getView(position, convertView, parent);
				TextView textView = (TextView) view
						.findViewById(android.R.id.text1);

				// put the image on the text view
				textView.setCompoundDrawablesWithIntrinsicBounds(
						fileList[position].icon, 0, 0, 0);

				// add margin between image and text (support various screen
				// densities)
				int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
				textView.setCompoundDrawablePadding(dp5);

				return view;
			}
		};

	}

	private class Item {
		public String file;
		public int icon;

		public Item(String file, Integer icon) {
			this.file = file;
			this.icon = icon;
		}

		@Override
		public String toString() {
			return file;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		AlertDialog.Builder builder = new Builder(this);

		if (fileList == null) {
			dialog = builder.create();
			return dialog;
		}

		switch (id) {
		case DIALOG_LOAD_FILE:
			builder.setTitle(getString(directoryMode ? R.string.dir_browser_title : R.string.file_browser_title));
			builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int which) {
					chosenFile = fileList[which].file;
					File sel = new File(path + File.separator + chosenFile);
					if (sel.isDirectory()) {
						firstLvl = false;

						// Adds chosen directory to list
						str.add(chosenFile);
						fileList = null;
						path = new File(sel.toString());

						loadFileList();

						removeDialog(DIALOG_LOAD_FILE);
						showDialog(DIALOG_LOAD_FILE);
					} else if (chosenFile.equals(upString) && !sel.exists()) { // Checks if 'up' was clicked
						// present directory removed from list
						String s = str.remove(str.size() - 1);

						// path modified to exclude present directory
						path = new File(path.toString().substring(0,
								path.toString().lastIndexOf(s)));
						fileList = null;

						// if there are no more directories in the list, then
						// its the first level
						if (str.isEmpty()) {
							firstLvl = true;
						}
						loadFileList();

						removeDialog(DIALOG_LOAD_FILE);
						showDialog(DIALOG_LOAD_FILE);
					} else {
					    Intent result = new Intent();
					    if (directoryMode) {
					        result.putExtra(RESULT_DIR_SELECTED, path.getAbsolutePath());
					    } else {
					        result.putExtra(RESULT_FILE_SELECTED, sel.getAbsolutePath());
					    }
					    setResult(RESULT_OK, result);
					    removeDialog(DIALOG_LOAD_FILE);
					    finish();
					}

				}
			});
			break;
		}
		if (directoryMode) {
		    builder.setPositiveButton(R.string.file_dir_dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    Intent result = new Intent();
                    result.putExtra(RESULT_DIR_SELECTED, path.getAbsolutePath());
                    setResult(RESULT_OK, result);
                    removeDialog(DIALOG_LOAD_FILE);
                    finish();
                }
            });
		    builder.setNegativeButton(R.string.file_dir_dialog_default, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    Intent result = new Intent();
                    result.putExtra(RESULT_DIR_SELECTED, ""); //$NON-NLS-1$
                    setResult(RESULT_OK, result);
                    removeDialog(DIALOG_LOAD_FILE);
                    finish();
                }
            });
		}

		dialog = builder.show();
		dialog.setCancelable(true);
		dialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface d) {
                finish();
            }
        });
		return dialog;
	}

}