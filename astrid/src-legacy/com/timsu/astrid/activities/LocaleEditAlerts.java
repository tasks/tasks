package com.timsu.astrid.activities;

import java.util.LinkedList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.tag.TagModelForView;
import com.timsu.astrid.utilities.Constants;

/**
 * Activity to edit alerts from Locale
 *
 * @author timsu
 *
 */
public final class LocaleEditAlerts extends Activity {

    /** value for action type for tag alert */
    public static final String ACTION_LOCALE_ALERT = "com.timsu.astrid.action.LOCALE_ALERT";

    /** key name for tag id/name in bundle */
    public static final String KEY_TAG_ID = "tag";
    public static final String KEY_TAG_NAME = "name";

    private LinkedList<TagModelForView> tags = null;
    private String[] tagNames = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.locale_edit_alerts);

		// Set up the breadcrumbs in the title bar
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.locale_ellipsizing_title);

		String breadcrumbString = getIntent().getStringExtra(com.twofortyfouram.Intent.EXTRA_STRING_BREADCRUMB);
		if (breadcrumbString == null)
			breadcrumbString = getString(R.string.locale_edit_alerts_title);
		else
			breadcrumbString = breadcrumbString + com.twofortyfouram.Intent.BREADCRUMB_SEPARATOR + getString(R.string.locale_edit_alerts_title);

		((TextView) findViewById(R.id.locale_ellipsizing_title_text)).setText(breadcrumbString);
		setTitle(breadcrumbString);

		final Spinner tagSpinner = (Spinner) findViewById(R.id.spinner);

		TagController tagController = new TagController(this);
		tagController.open();
		tags = tagController.getAllTags();
		tagController.close();

		tagNames = new String[tags.size()];
		for(int i = 0; i < tags.size(); i++)
		    tagNames[i] = tags.get(i).getName();

		ArrayAdapter<String> tagAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, tagNames);
		tagAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		tagSpinner.setAdapter(tagAdapter);

		// Save the state into the return Intent whenever the field
		tagSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                updateResult();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // do nothing
            }

		});
	}

    @Override
    protected void onStart() {
        super.onStart();

        // set up flurry
        FlurryAgent.onStartSession(this, Constants.FLURRY_KEY);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }

	/**
	 * Private helper method to persist the Toast message in the return {@code Intent}.
	 */
	private void updateResult() {
	    // no tags, so it's not possible to save
	    if(tagNames.length == 0) {
	        setResult(com.twofortyfouram.Intent.RESULT_REMOVE);
	        return;
	    }

		final int index = ((Spinner) findViewById(R.id.spinner)).getSelectedItemPosition();
		final String tagName = tagNames[index];
		final TagModelForView tag = tags.get(index);

        /*
         * If the message is of 0 length, then there isn't a setting to save
         */
        if (tagName == null) {
            setResult(com.twofortyfouram.Intent.RESULT_REMOVE);
        } else {
            final Intent intent = new Intent();
            intent.putExtra(com.twofortyfouram.Intent.EXTRA_STRING_ACTION_FIRE,
            		ACTION_LOCALE_ALERT);
            intent.putExtra(KEY_TAG_ID, tag.getTagIdentifier().getId());
            intent.putExtra(KEY_TAG_NAME, tagName);
            intent.putExtra(com.twofortyfouram.Intent.EXTRA_STRING_BLURB, tagName);
            setResult(RESULT_OK, intent);
        }
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.locale_edit_alerts, menu);

		menu.findItem(R.id.menu_save).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
		{
			public boolean onMenuItemClick(final MenuItem item)
			{
				updateResult();
				finish();
				return true;
			}
		});

		menu.findItem(R.id.menu_dontsave).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
		{
			public boolean onMenuItemClick(final MenuItem item)
			{
				setResult(RESULT_CANCELED);
				finish();
				return true;
			}
		});

		menu.findItem(R.id.menu_help).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
		{
			public boolean onMenuItemClick(final MenuItem item)
			{

				final Intent helpIntent = new Intent(com.twofortyfouram.Intent.ACTION_HELP);
				helpIntent.putExtra("com.twofortyfouram.locale.intent.extra.HELP_URL", "http://www.androidlocale.com/app_data/toast/1.0/help_toast.htm"); //$NON-NLS-1$ //$NON-NLS-2$

				// Set up the breadcrumbs in the title bar
				String breadcrumbString = getIntent().getStringExtra(com.twofortyfouram.Intent.EXTRA_STRING_BREADCRUMB);
				if (breadcrumbString == null)
					helpIntent.putExtra(com.twofortyfouram.Intent.EXTRA_STRING_BREADCRUMB, getString(R.string.locale_edit_alerts_title));
				else
					helpIntent.putExtra(com.twofortyfouram.Intent.EXTRA_STRING_BREADCRUMB, breadcrumbString + com.twofortyfouram.Intent.BREADCRUMB_SEPARATOR
							+ getString(R.string.locale_edit_alerts_title));

				startActivity(helpIntent);
				return true;
			}
		});

		return true;
	}
}