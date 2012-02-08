package com.todoroo.astrid.ui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.astrid.data.Task;

@SuppressWarnings("nls")
public class WebServicesView extends LinearLayout {

    private static final String GOOGLE_SEARCH_URL = "https://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=";

    private Task task;
    private final DisplayMetrics metrics = new DisplayMetrics();
    private LayoutInflater inflater;
    private Activity activity;

    @Autowired RestClient restClient;
    @Autowired ExceptionService exceptionService;

    public WebServicesView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public WebServicesView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WebServicesView(Context context) {
        super(context);
    }

    public void setTask(Task task) {
        this.task = task;
        initialize();
    }

    /**
     * Initialize view
     */
    private void initialize() {
        DependencyInjectionService.getInstance().inject(this);
        setOrientation(LinearLayout.VERTICAL);

        activity = (Activity) getContext();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        initializeAmazon();

        addSectionDivider();

        initializeGoogleSearch();
    }

    protected void initializeAmazon() {
        addSectionHeader("Amazon.com");

        final LinearLayout body = addHorizontalScroller();

        for(int i = 0; i < 10; i++) {
            ImageView aiv = new ImageView(getContext());
            aiv.setImageResource(R.drawable.icon);
            aiv.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
            body.addView(aiv);
        }
    }

    /**
     * Initialize Google search results
     */
    protected void initializeGoogleSearch() {
        addSectionHeader("Google Search");

        final LinearLayout body = addHorizontalScroller();

        ProgressBar progressBar = new ProgressBar(getContext());
        progressBar.setIndeterminate(true);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));
        body.addView(progressBar);

        new Thread() {
            @Override
            public void run() {
                Exception exception = null;
                JSONObject searchResults = null;

                try {
                    String url = GOOGLE_SEARCH_URL +
                        URLEncoder.encode(task.getValue(Task.TITLE), "UTF-8");
                    String result = restClient.get(url);
                    searchResults = new JSONObject(result);
                } catch (UnsupportedEncodingException e) {
                    exception = e;
                } catch (IOException e) {
                    exception = e;
                } catch (JSONException e) {
                    exception = e;
                }

                final Exception finalException = exception;
                final JSONObject finalResults = searchResults;
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        body.removeAllViews();

                        if(finalException != null)
                            displayError(finalException, body);
                        else {
                            try {
                                processGoogleSearchResults(body,
                                        finalResults.getJSONObject("responseData"));
                            } catch (JSONException e) {
                                displayError(e, body);
                            }
                        }
                    }
                });
            }
        }.start();
    }

    protected void processGoogleSearchResults(LinearLayout body,
            JSONObject searchResults) throws JSONException {

        JSONArray results = searchResults.getJSONArray("results");

        for(int i = 0; i < results.length(); i++) {
            JSONObject result = results.getJSONObject(i);
            View view = inflater.inflate(R.layout.web_service_text_row, body, false);
            ((TextView)view.findViewById(R.id.title)).setText(result.getString("titleNoFormatting"));
            ((TextView)view.findViewById(R.id.url)).setText(result.getString("visibleUrl"));
            body.addView(view);

            String url = result.getString("url");
            view.setTag(url);
        }

        JSONObject cursor = searchResults.getJSONObject("cursor");
        String moreLabel = String.format("Show all %s results",
                cursor.getString("estimatedResultCount"));
        String url = cursor.getString("moreResultsUrl");

        View view = inflater.inflate(R.layout.web_service_text_row, body, false);
        ((TextView)view.findViewById(R.id.title)).setText(moreLabel);
        view.setBackgroundColor(Color.rgb(200, 200, 200));
        view.setTag(url);
        body.addView(view);
    }

    protected void displayError(Exception exception, LinearLayout body) {
        exceptionService.reportError("google-error", exception);

        TextView textView = new TextView(getContext());
        textView.setTextAppearance(getContext(), R.style.TextAppearance_Medium);
        textView.setText(exception.toString());
        body.addView(textView);
    }

    protected LinearLayout addHorizontalScroller() {
        HorizontalScrollView scroll = new HorizontalScrollView(getContext());
        scroll.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
        addView(scroll);

        LinearLayout body = new LinearLayout(getContext());
        body.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,
                Math.round(100 * metrics.density)));
        scroll.addView(body);

        return body;
    }

    private void addSectionDivider() {
        View view = new View(getContext());
        MarginLayoutParams mlp = new MarginLayoutParams(LayoutParams.FILL_PARENT, 1);
        mlp.setMargins(10, 5, 10, 5);
        view.setLayoutParams(mlp);
        view.setBackgroundResource(R.drawable.black_white_gradient);
        addView(view);
    }

    private void addSectionHeader(String string) {
        TextView textView = new TextView(getContext());
        textView.setText(string);
        textView.setTextAppearance(getContext(), R.style.TextAppearance_Medium);
        addView(textView);
    }


}
