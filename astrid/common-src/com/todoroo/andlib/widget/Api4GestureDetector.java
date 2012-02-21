package com.todoroo.andlib.widget;

import java.util.ArrayList;

import android.app.Activity;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;

import com.todoroo.andlib.widget.GestureService.GestureInterface;

public class Api4GestureDetector implements OnGesturePerformedListener {
    private final GestureLibrary mLibrary;
    private final GestureInterface listener;

    public Api4GestureDetector(Activity activity, int view, int gestureLibrary, GestureInterface listener) {
        this.listener = listener;
        mLibrary = GestureLibraries.fromRawResource(activity, gestureLibrary);

        if(mLibrary.load()) {
            GestureOverlayView gestures = (GestureOverlayView) activity.findViewById(view);
            if(gestures != null)
                gestures.addOnGesturePerformedListener(this);
        }
    }

    @Override
    public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
        ArrayList<Prediction> predictions = mLibrary.recognize(gesture);

        // We want at least one prediction
        if (predictions.size() > 0) {
            Prediction prediction = predictions.get(0);
            // We want at least some confidence in the result
            if (prediction.score > 2.0) {
                listener.gesturePerformed(prediction.name);
            }
        }
    }
}
