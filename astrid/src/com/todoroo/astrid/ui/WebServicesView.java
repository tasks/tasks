package com.todoroo.astrid.ui;

import greendroid.widget.AsyncImageView;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.AmazonRequestsHelper;
import com.todoroo.astrid.producteev.api.StringEscapeUtils;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.taskrabbit.TaskRabbitControlSet;
import com.todoroo.astrid.utility.Constants;

@SuppressWarnings("nls")
public class WebServicesView extends LinearLayout {

    private static final String ASSOCIATE_TAG = "wwwtodoroocom-20";

    private static final int ROW_HEIGHT = 100;

    private static final int ID_AMAZON = 0x3423712;
    private static final int ID_GOOGLE = 0x3487532;

    private static final String GOOGLE_SEARCH_URL = "https://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=";

    private Task task;
    private final DisplayMetrics metrics = new DisplayMetrics();
    private LayoutInflater inflater;
    private Activity activity;
    public TaskRabbitControlSet taskRabbitControl;
    private final AtomicBoolean notConnected = new AtomicBoolean(false);
    private boolean pageLoaded = false;

    private LinearLayout.LayoutParams rowParams;

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

    public int[] getScrollableViews() {
        return new int[] { ID_AMAZON, ID_GOOGLE };
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

        rowParams = new LinearLayout.LayoutParams(
                Math.round(metrics.widthPixels * 0.75f),
                Math.round(ROW_HEIGHT * metrics.density));
        rowParams.rightMargin = Math.round(10 * metrics.density);

        if (!Preferences.getBoolean(R.string.p_autoIdea, true)) {
            View loadButton = inflater.inflate(R.layout.web_services_load_button, null);
            initializeTaskRabbit();
            addView(loadButton);
            loadButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    refresh();
                }
            });
        }
    }

    public void onPageSelected(Runnable runnable) {
        if(!pageLoaded && Preferences.getBoolean(R.string.p_autoIdea, true))
            refresh();
        runnable.run();
    }

    public void refresh() {
        if(TextUtils.isEmpty(task.getValue(Task.TITLE)))
            return;

        pageLoaded = true;

        removeAllViews();

        initializeTaskRabbit();

        initializeAmazon();

        addSectionDivider();

        initializeGoogleSearch();
    }

    private void showNotConnected() {
        ImageView imageView = new ImageView(getContext());
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        imageView.setImageResource(R.drawable.icon);
        imageView.setPadding(20, 50, 20, 20);
        imageView.setScaleType(ScaleType.CENTER);
        addView(imageView);

        TextView textView = new TextView(getContext());
        textView.setTextAppearance(getContext(), R.style.TextAppearance_Medium);
        textView.setText(R.string.WSV_not_online);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(20, 50, 20, 20);
        addView(textView);
    }

    protected void initializeAmazon() {
        addSectionHeader("Amazon.com");

        final LinearLayout body = addHorizontalScroller(ID_AMAZON);

        new Thread() {
            @Override
            public void run() {
                try {
                    AmazonRequestsHelper helper = AmazonRequestsHelper.getInstance(
                            Constants.AWS_ENDPOINT, Constants.AWS_ACCESS_KEY_ID,
                            Constants.AWS_SECRET_KEY_ID);

                    Map<String, String> params = new HashMap<String, String>();
                    params.put("Service", "AWSECommerceService");
                    params.put("Version", "2011-08-01");
                    params.put("Operation", "ItemSearch");
                    params.put("Availability", "Available");
                    params.put("ResponseGroup", "Medium");
                    params.put("Keywords", task.getValue(Task.TITLE));
                    params.put("SearchIndex", "All");
                    params.put("AssociateTag", ASSOCIATE_TAG);

                    String requestUrl = helper.sign(params);
                    String result = restClient.get(requestUrl);
                    activity.runOnUiThread(new AmazonSearchResultsProcessor(body,
                            result));

                } catch (Exception e) {
                    displayError(e, body);
                }
            }
        }.start();
    }

    private class AmazonSearchResultsProcessor implements Runnable {

        private final LinearLayout body;
        private final String searchResults;

        public AmazonSearchResultsProcessor(LinearLayout body,
                String searchResults) {
            this.body = body;
            this.searchResults = searchResults;
        }

        @Override
        public void run() {
            try {
                body.removeAllViews();

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);

                XmlPullParser xpp = factory.newPullParser();

                xpp.setInput(new StringReader(searchResults));
                int eventType = xpp.getEventType();

                HashMap<String, String> attributes = new HashMap<String, String>();
                ArrayList<String> authors = new ArrayList<String>();

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if(eventType == XmlPullParser.START_TAG) {
                        if("Title".equals(xpp.getName()))
                            attributes.put("title", xpp.nextText());
                        else if("FormattedPrice".equals(xpp.getName()))
                            attributes.put("price", xpp.nextText());
                        else if("DetailPageURL".equals(xpp.getName()))
                            attributes.put("url", xpp.nextText());
                        else if("Brand".equals(xpp.getName()) ||
                                "Studio".equals(xpp.getName()) ||
                                "Label".equals(xpp.getName()))
                            attributes.put("subtitle", xpp.nextText());
                        else if("Author".equals(xpp.getName()) ||
                                "Director".equals(xpp.getName()) ||
                                "Artist".equals(xpp.getName()))
                            authors.add(xpp.nextText());
                        else if("MediumImage".equals(xpp.getName())) {
                            xpp.next();
                            attributes.put("image", xpp.nextText());
                        }
                        else if("LowestNewPrice".equals(xpp.getName())) {
                            xpp.next();
                            attributes.put("lowestNew", xpp.nextText());
                        }
                        else if("LowestUsedPrice".equals(xpp.getName())) {
                            xpp.next();
                            attributes.put("lowestUsed", xpp.nextText());
                        }
                        else if("TotalNew".equals(xpp.getName()))
                            attributes.put("totalNew", xpp.nextText());
                        else if("TotalUsed".equals(xpp.getName()))
                            attributes.put("totalUsed", xpp.nextText());
                        else if("Error".equals(xpp.getName())) {
                            xpp.next();
                            String code = xpp.nextText();
                            String message = xpp.nextText();
                            throw new AmazonException(code, message);
                        }
                    } else if(eventType == XmlPullParser.END_TAG) {
                        if("Item".equals(xpp.getName())) {
                            renderItem(attributes, authors);
                            attributes.clear();
                            authors.clear();
                        }
                    }
                    eventType = xpp.next();
                }

            } catch (AmazonException e) {
                if(!"AWS.ECommerceService.NoExactMatches".equals(e.getCode()))
                    exceptionService.reportError("amazon-error", e);
            } catch (Exception e) {
                exceptionService.reportError("amazon-other-error", e);
            }

            try {
                String moreLabel = "Show all results";
                String url = String.format("http://www.amazon.com/s/?field-keywords=%s&tag=%s",
                        URLEncoder.encode(task.getValue(Task.TITLE), "UTF-8"), ASSOCIATE_TAG);

                View view = inflateRow(body, null, moreLabel, "",
                        new LinkTag(url, "amazon-more", null));
                view.setBackgroundColor(Color.argb(128, 128, 128, 128));
            } catch (UnsupportedEncodingException e) {
                //
            }
        }

        private void renderItem(HashMap<String, String> attributes,
                ArrayList<String> authors) {
            View view = inflater.inflate(R.layout.web_service_amazon_row, body, false);

            ((AsyncImageView)view.findViewById(R.id.image)).setUrl(
                    attributes.get("image"));
            ((TextView)view.findViewById(R.id.title)).setText(
                    attributes.get("title"));
            ((TextView)view.findViewById(R.id.price)).setText(
                    attributes.get("price"));
            view.setTag(new LinkTag(attributes.get("url"),
                    "amazon", attributes.get("price")));

            String subtitle = attributes.get("subtitle");
            if(authors.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for(String author : authors) {
                    sb.append(author).append(", ");
                }
                subtitle = sb.toString().substring(0, sb.length() - 2);
            }
            ((TextView)view.findViewById(R.id.subtitle)).setText(subtitle);

            String newAndUsed = null;
            if(attributes.containsKey("lowestNew") || attributes.containsKey("lowestUsed")) {
                int lowestNew = Integer.MAX_VALUE;
                try {
                    lowestNew = attributes.containsKey("lowestNew") ?
                            Integer.parseInt(attributes.get("lowestNew")) : Integer.MAX_VALUE;
                } catch (NumberFormatException e) {
                    // text, i.e. "too low to display"
                }
                int lowestUsed = Integer.MAX_VALUE;
                try {
                    lowestUsed = attributes.containsKey("lowestUsed") ?
                            Integer.parseInt(attributes.get("lowestUsed")) : Integer.MAX_VALUE;
                } catch (NumberFormatException e) {
                    // text, i.e. "too low to display"
                }

                int lowest = Math.min(lowestNew, lowestUsed);
                int total = 0;
                if(attributes.containsKey("totalNew"))
                    total += Integer.parseInt(attributes.get("totalNew"));
                if(attributes.containsKey("totalUsed"))
                    total += Integer.parseInt(attributes.get("totalUsed"));

                String price = String.format("$%.2f", lowest / 100f);
                if(!price.equals(attributes.get("price")))
                    newAndUsed = String.format("%d New & Used from %s", total, price);
            }
            ((TextView)view.findViewById(R.id.new_and_used)).setText(newAndUsed);


            view.setOnClickListener(linkClickListener);
            view.setLayoutParams(rowParams);

            body.addView(view);
        }

    }

    private class AmazonException extends Exception {

        private static final long serialVersionUID = -6974618119363113454L;
        private final String code;

        public AmazonException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }

    }

    /**
     * Initialize Google search results
     */
    protected void initializeGoogleSearch() {
        addSectionHeader("Google Search");

        final LinearLayout body = addHorizontalScroller(ID_GOOGLE);

        new Thread() {
            @Override
            public void run() {
                try {
                    String url = GOOGLE_SEARCH_URL +
                    URLEncoder.encode(task.getValue(Task.TITLE), "UTF-8");
                    String result = restClient.get(url);
                    final JSONObject searchResults = new JSONObject(result);

                    activity.runOnUiThread(new GoogleSearchResultsProcessor(body,
                            searchResults.getJSONObject("responseData")));

                } catch (Exception e) {
                    displayError(e, body);
                }
            }
        }.start();
    }

    private class GoogleSearchResultsProcessor implements Runnable {

        private final LinearLayout body;
        private final JSONObject searchResults;

        public GoogleSearchResultsProcessor(LinearLayout body,
                JSONObject searchResults) {
            this.body = body;
            this.searchResults = searchResults;
        }

        public void run() {
            body.removeAllViews();

            try {
                JSONArray results = searchResults.getJSONArray("results");

                for(int i = 0; i < results.length(); i++) {
                    JSONObject result = results.getJSONObject(i);
                    String title = StringEscapeUtils.unescapeHtml(result.getString("titleNoFormatting"));
                    inflateRow(body, null, title,
                            result.getString("visibleUrl"),
                            new LinkTag(result.getString("url"),
                                    "google", null));
                }

                JSONObject cursor = searchResults.getJSONObject("cursor");
                String moreLabel = "Show more results";
                if(results.length() == 0)
                    moreLabel = "Search Google";
                String url = cursor.getString("moreResultsUrl");

                View view = inflateRow(body, null, moreLabel, "",
                        new LinkTag(url, "google-more", null));
                view.setBackgroundColor(Color.argb(128, 128, 128, 128));

            } catch (JSONException e) {
                displayError(e, body);
            }
        }
    }


    /**
     * Initialize Google search results
     */
    protected void initializeTaskRabbit() {

        if(taskRabbitControl != null && taskRabbitControl.isEnabledForTRLocation == true) {
            addSectionHeader("Outsource this task to someone");

            final LinearLayout body = new LinearLayout(activity);
            body.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                    ROW_HEIGHT));
            body.setOrientation(LinearLayout.VERTICAL);


            ImageView imageView = new ImageView(getContext());
            imageView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                    ROW_HEIGHT));
            imageView.setImageResource(R.drawable.task_rabbit_logo);
            imageView.setScaleType(ScaleType.CENTER_INSIDE);
            body.addView(imageView);

            body.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    taskRabbitControl.showTaskRabbitActivity();

                }
            });
            addView(body);
            addSectionDivider();

        }

    }


    protected View inflateRow(ViewGroup body, String imageUrl, String title, String subtitle,
            LinkTag tag) {
        View view = inflater.inflate(R.layout.web_service_row, body, false);
        AsyncImageView imageView = (AsyncImageView)view.findViewById(R.id.image);

        if(imageUrl == null)
            imageView.setVisibility(View.GONE);
        else
            imageView.setUrl(imageUrl);

        ((TextView)view.findViewById(R.id.title)).setText(title);
        ((TextView)view.findViewById(R.id.subtitle)).setText(subtitle);
        view.setOnClickListener(linkClickListener);
        view.setTag(tag);
        view.setLayoutParams(rowParams);

        body.addView(view);
        return view;
    }

    private class LinkTag {
        private final String url;
        private final String service;
        private String price;

        public LinkTag(String url, String servce, String price) {
            this.url = url;
            this.service = servce;
            this.price = price;
        }

    }

    public OnClickListener linkClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v.getTag() instanceof LinkTag) {
                LinkTag tag = (LinkTag) v.getTag();

                if(tag.price != null) {
                    try {
                        double price = Double.parseDouble(tag.price.replaceAll("[^.\\d]", ""));
                        if(price < 5)
                            tag.price = "$0 - $4";
                        else if(price < 10)
                            tag.price = "$5 - $9";
                        else if(price < 25)
                            tag.price = "$10 - $24";
                        else if(price < 50)
                            tag.price = "$25 - $49";
                        else if(price < 100)
                            tag.price = "$50 - $99";
                        else if(price < 500)
                            tag.price = "$100 - $499";
                        else
                            tag.price = "$500+";
                    } catch(Exception e) {
                        tag.price = null;
                    }
                }

                StatisticsService.reportEvent(StatisticsConstants.IDEAS_LINK_CLICKED,
                        "service", tag.service, "price", tag.price);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(tag.url));
                activity.startActivity(intent);
            }
        }
    };

    protected void displayError(final Exception exception, final LinearLayout body) {
        if(exception instanceof IOException) {
            if(notConnected.getAndSet(true))
                return;

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    removeAllViews();

                    initializeTaskRabbit();
                    showNotConnected();
                }
            });
            return;
        }

        exceptionService.reportError("web-service-error", exception);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                body.removeAllViews();

                TextView textView = new TextView(getContext());
                textView.setTextAppearance(getContext(), R.style.TextAppearance_Medium);
                textView.setText(exception.getClass().getSimpleName() + ": " +
                        exception.getLocalizedMessage());
                textView.setLines(2);
                body.addView(textView);
            }
        });
    }

    protected LinearLayout addHorizontalScroller(int id) {
        HorizontalScrollView scroll = new HorizontalScrollView(getContext());
        scroll.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
        scroll.setId(id);
        scroll.setScrollbarFadingEnabled(false);
        addView(scroll);

        LinearLayout body = new LinearLayout(getContext());
        body.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,
                Math.round(ROW_HEIGHT * metrics.density)));
        scroll.addView(body);

        ProgressBar progressBar = new ProgressBar(getContext());
        progressBar.setIndeterminate(true);
        LayoutParams layoutParams = new LinearLayout.LayoutParams(metrics.widthPixels,
                Math.round(10 * metrics.density));
        layoutParams.gravity = Gravity.CENTER;
        progressBar.setLayoutParams(layoutParams);
        progressBar.setIndeterminateDrawable(getResources().getDrawable(
                android.R.drawable.progress_indeterminate_horizontal));
        body.addView(progressBar);

        return body;

    }

    private View addSectionDivider() {
        View view = new View(getContext());
        LayoutParams mlp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 1);
        mlp.setMargins(10, 20, 10, 20);
        view.setLayoutParams(mlp);
        view.setBackgroundColor(Color.GRAY);
        addView(view);
        return view;
    }

    private void addSectionHeader(String string) {
        TextView textView = new TextView(getContext());
        textView.setText(string);
        textView.setTextAppearance(getContext(), R.style.TextAppearance_GEN_EditLabel);
        addView(textView);
    }


}
