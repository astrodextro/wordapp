package com.felixunlimited.word.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.os.ResultReceiver;

import java.io.File;

public class DownloadService extends IntentService {

    public static final int UPDATE_PROGRESS = 8344;
    private int result = Activity.RESULT_CANCELED;
    public static final String URL = "urlpath";
    public static final String DIR = "dir";
    public static final String FILENAME = "filename";
    public static final String FILEPATH = "filepath";
    public static final String RESULT = "result";
    public static final String NOTIFICATION = "com.felixunlimited.word.app.service.receiver";

    public DownloadService() {
        super("DownloadService");
    }

    private Context context;
    // will be called asynchronously by Android
    @Override
    protected void onHandleIntent(Intent intent) {
        context = getBaseContext();
        String urlPath = intent.getStringExtra(URL);
        String messageTitle = intent.getStringExtra("title");
        final ResultReceiver receiver = (ResultReceiver) intent.getParcelableExtra("receiver");
        String fileName = intent.getStringExtra(FILENAME);
        String dir = Utility.choosePreferredDir(context).getAbsolutePath();
        final File outputFile = new File(dir, fileName);

        final DownloadManager downloadManager = (DownloadManager) getBaseContext().getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(urlPath);
        DownloadManager.Request req=new DownloadManager.Request(uri);

        req.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI
                | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setVisibleInDownloadsUi(false)
//                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                .setTitle("Downloading: "+messageTitle)
                .setDescription("Please ensure you have good internet connectivity")
//                .setDestinationUri(Uri.fromFile(Utility.chooseFile(context, fileName)));
                .setDestinationInExternalFilesDir(getBaseContext(), null, fileName);                        //Enqueue a new download and same the referenceId
        final long downloadReference = downloadManager.enqueue(req);

        new Thread(new Runnable() {

            @Override
            public void run() {

                boolean downloading = true;

                while (downloading) {

                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(downloadReference);

                    Cursor cursor = downloadManager.query(q);
                    cursor.moveToFirst();
                    int bytes_downloaded = cursor.getInt(cursor
                            .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        result = Activity.RESULT_OK;
                        Bundle resultData = new Bundle();
                        resultData.putInt("progress", 100);
                        receiver.send(UPDATE_PROGRESS, resultData);
                        publishResults(outputFile.getAbsolutePath(), result);
                        downloading = false;
                    }

                    Bundle resultData = new Bundle();
                    int progress = (int) ((bytes_downloaded * 100) / bytes_total);
                    resultData.putInt("progress", progress);
                    receiver.send(UPDATE_PROGRESS, resultData);

                    cursor.close();
                }

            }
        }).start();

    }

    private void publishResults(String outputPath, int result) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(FILEPATH, outputPath);
        intent.putExtra(RESULT, result);
        sendBroadcast(intent);
        onDestroy();
    }
}