package com.todoroo.astrid.service;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;

public class UpdateScreenFlow extends Activity {

    public static final String TOKEN_SCREENS = "token_screens"; //$NON-NLS-1$
    private static final int REQUEST_CODE_SCREEN_FLOW = 5;

    private ArrayList<String> screens;
    private int currIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        screens = getIntent().getStringArrayListExtra(TOKEN_SCREENS);
        currIndex = 0;
        if (screens.size() == 0)
            finish();

        startActivityFromString(screens.get(0));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN_FLOW && resultCode == RESULT_OK) {
            currIndex++;
            if (currIndex < screens.size()) {
                String next = screens.get(currIndex);
                startActivityFromString(next);
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    private void startActivityFromString(String className) {
        try {
            Class<?> activityClass = Class.forName(className);
            Intent intent = new Intent(this, activityClass);
            startActivityForResult(intent, REQUEST_CODE_SCREEN_FLOW);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            finish();
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            finish();
        }
    }

}
