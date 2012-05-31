package com.todoroo.astrid.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.aacenc.RecognizerApi;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.ui.PopupControlSet;

public class FilesControlSet extends PopupControlSet {

    @Autowired
    private MetadataService metadataService;

    private final ArrayList<Metadata> files = new ArrayList<Metadata>();
    private final LinearLayout fileDisplayList;
    private LinearLayout fileList;
    private final LayoutInflater inflater;

    public FilesControlSet(Activity activity, int viewLayout, int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);
        DependencyInjectionService.getInstance().inject(this);

        displayText.setText(activity.getString(R.string.TEA_control_files));
        fileDisplayList = (LinearLayout) getDisplayView().findViewById(R.id.files_list);
        inflater = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    protected void refreshDisplayView() {
        fileDisplayList.removeAllViews();
        for (final Metadata m : files) {
            View fileRow = inflater.inflate(R.layout.file_display_row, null);
            LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.RIGHT;
            setUpFileRow(m, fileRow, fileDisplayList, lp);
        }
    }

    @Override
    public void readFromTask(Task task) {
        super.readFromTask(task);

        refreshMetadata();
        refreshDisplayView();
    }

    public void refreshMetadata() {
        TodorooCursor<Metadata> cursor = metadataService.query(
                Query.select(Metadata.PROPERTIES)
                     .where(MetadataCriteria.byTaskAndwithKey(model.getId(), FileMetadata.METADATA_KEY)));
        try {
            files.clear();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                Metadata metadata = new Metadata();
                metadata.readFromCursor(cursor);
                files.add(metadata);
            }
        } finally {
            cursor.close();
        }
        if (initialized)
            afterInflate();
    }

    @Override
    protected void readFromTaskOnInitialize() {
        // TODO Auto-generated method stub
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        // Nothing to write
        return null;
    }

    @Override
    protected void afterInflate() {
        fileList = (LinearLayout) getView().findViewById(R.id.files_list);
        final LinearLayout finalList = fileList;
        fileList.removeAllViews();
        LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        for (final Metadata m : files) {
            final View fileRow = inflater.inflate(R.layout.file_row, null);

            setUpFileRow(m, fileRow, fileList, lp);
            View name = fileRow.findViewById(R.id.file_text);
            View clearFile = fileRow.findViewById(R.id.remove_file);
            clearFile.setVisibility(View.VISIBLE);

            setupFileClickListener(name, m);
            clearFile.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogUtilities.okCancelDialog(activity, activity.getString(R.string.premium_remove_file_confirm),
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            File f = new File(m.getValue(FileMetadata.FILE_PATH));
                            if (f.delete()) {
                                metadataService.delete(m);
                                files.remove(m);
                                refreshDisplayView();
                                finalList.removeView(fileRow);
                            }
                        }
                    }, null);
                }
            });
        }
    }

    private void setupFileClickListener(View view, final Metadata m) {
        String fileType = m.containsNonNullValue(FileMetadata.FILE_TYPE) ? m.getValue(FileMetadata.FILE_TYPE) : FileMetadata.FILE_TYPE_OTHER;
        final String filePath = m.containsNonNullValue(FileMetadata.FILE_PATH) ? m.getValue(FileMetadata.FILE_PATH) : null;
        if (TextUtils.isEmpty(filePath)) {
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogUtilities.okCancelDialog(activity, activity.getString(R.string.file_download_title),
                            activity.getString(R.string.file_download_body), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            downloadFile(m);
                        }
                    }, null);
                }
            });
        } else if (fileType.startsWith(FileMetadata.FILE_TYPE_AUDIO)) {
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    RecognizerApi.play(m.getValue(FileMetadata.FILE_PATH));
                }
            });
        } else if (fileType.startsWith(FileMetadata.FILE_TYPE_IMAGE)) {
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog image = new AlertDialog.Builder(activity).create();
                    ImageView imageView = new ImageView(activity);
                    imageView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
                    Bitmap bitmap = AndroidUtilities.readScaledBitmap(filePath);

                    if (bitmap == null) {
                        Toast.makeText(activity, R.string.file_err_memory, Toast.LENGTH_LONG);
                        return;
                    }

                    imageView.setImageBitmap(bitmap);
                    image.setView(imageView);

                    image.setButton(activity.getString(R.string.DLG_close), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            return;
                        }
                    });
                    image.show();
                }
            });
        }
    }

    @SuppressWarnings("nls")
    private void downloadFile(final Metadata m) {
        final ProgressDialog pd = new ProgressDialog(activity);
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setMessage(activity.getString(R.string.file_download_progress));
        pd.setMax(100);

        new Thread() {
            @Override
            public void run() {
                String urlString = m.getValue(FileMetadata.URL);
                String name;
                if (urlString.endsWith("/"))
                    urlString = urlString.substring(0, urlString.length() - 1);

                int lastComponent = urlString.lastIndexOf('/');
                if (lastComponent > 0)
                    name = urlString.substring(lastComponent + 1);
                else
                    name = urlString;
                StringBuilder filePathBuilder = new StringBuilder();
                filePathBuilder.append(activity.getExternalFilesDir(FileMetadata.FILES_DIRECTORY).toString())
                    .append(File.separator)
                    .append(name);

                File file = new File(filePathBuilder.toString());
                if (file.exists()) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, R.string.file_err_download, Toast.LENGTH_LONG);
                        }
                    });
                    return;
                }

                try {
                    URL url = new URL(urlString);

                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");

                    urlConnection.setDoOutput(true);

                    urlConnection.connect();

                    FileOutputStream fileOutput = new FileOutputStream(file);

                    InputStream inputStream = urlConnection.getInputStream();

                    int totalSize = urlConnection.getContentLength();

                    int downloadedSize = 0;

                    byte[] buffer = new byte[1024];

                    int bufferLength = 0; //used to store a temporary size of the buffer

                    while ((bufferLength = inputStream.read(buffer)) > 0) {
                        fileOutput.write(buffer, 0, bufferLength);
                        downloadedSize += bufferLength;

                        int progress = (int) (downloadedSize*100/totalSize);
                        pd.setProgress(progress);
                    }

                    fileOutput.flush();
                    fileOutput.close();

                    m.setValue(FileMetadata.FILE_PATH, file.getAbsolutePath());
                    metadataService.save(m);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshMetadata();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    file.delete();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, R.string.file_err_download, Toast.LENGTH_LONG);
                        }
                    });
                } finally {
                    pd.dismiss();
                }
            }
        }.start();
    }

    private void setUpFileRow(Metadata m, View row, LinearLayout parent, LayoutParams lp) {
        TextView nameView = (TextView) row.findViewById(R.id.file_text);
        TextView typeView = (TextView) row.findViewById(R.id.file_type);
        String name = getNameString(m);
        String type = getTypeString(m);
        nameView.setText(name);

        if (TextUtils.isEmpty(type))
            typeView.setVisibility(View.GONE);
        else
            typeView.setText(type);

        parent.addView(row, lp);
    }

    private String getNameString(Metadata metadata) {
        String name = metadata.getValue(FileMetadata.NAME);
        int extension = name.lastIndexOf('.');
        if (extension < 0)
            return name;
        return name.substring(0, extension);
    }

    @SuppressWarnings("nls")
    private String getTypeString(Metadata metadata) {
        String name;
        if (metadata.containsNonNullValue(FileMetadata.FILE_PATH)) {
            File f = new File(metadata.getValue(FileMetadata.FILE_PATH));
            name = f.getName();
        } else {
            name = metadata.getValue(FileMetadata.URL);
        }

        int extension = name.lastIndexOf('.');
        if (extension < 0 || extension + 1 >= name.length())
            return "";
        return name.substring(extension + 1).toUpperCase();

    }

}
