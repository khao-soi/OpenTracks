package de.dennisguse.opentracks.io.file.exporter;

import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.documentfile.provider.DocumentFile;

import de.dennisguse.opentracks.util.ExportUtils;

public class ExportService extends JobIntentService {

    private static final int JOB_ID = 1;

    private static final String EXTRA_RECEIVER = "extra_receiver";
    private static final String EXTRA_EXPORT_TASK = "export_task";
    private static final String EXTRA_DIRECTORY_URI = "extra_directory_uri";
    private static final String TAG = ExportService.class.getSimpleName();

    public static void enqueue(Context context, ExportServiceResultReceiver receiver, ExportTask exportTask, Uri directoryUri) {
        Intent intent = new Intent(context, JobService.class);
        intent.putExtra(EXTRA_RECEIVER, receiver);
        intent.putExtra(EXTRA_EXPORT_TASK, exportTask);
        intent.putExtra(EXTRA_DIRECTORY_URI, directoryUri);
        enqueueWork(context, ExportService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        // Get all data.
        ResultReceiver resultReceiver = intent.getParcelableExtra(EXTRA_RECEIVER);
        ExportTask exportTask = intent.getParcelableExtra(EXTRA_EXPORT_TASK);
        Uri directoryUri = intent.getParcelableExtra(EXTRA_DIRECTORY_URI);

        // Prepare resultCode and bundle to send to the receiver.
        Bundle bundle = new Bundle();
        bundle.putParcelable(ExportServiceResultReceiver.RESULT_EXTRA_EXPORT_TASK, exportTask);

        // Build directory file.
        DocumentFile directoryFile = DocumentFile.fromTreeUri(this, directoryUri);
        if (directoryFile == null || !directoryFile.canWrite()) {
            Log.e(TAG, "Can't write to directory: " + directoryFile);
            resultReceiver.send(ExportServiceResultReceiver.RESULT_CODE_ERROR, bundle);
            return;
        }

        // Export.
        boolean success = ExportUtils.exportTrack(this, directoryFile, exportTask);

        // Send result to the receiver.
        int resultCode = success ? ExportServiceResultReceiver.RESULT_CODE_SUCCESS : ExportServiceResultReceiver.RESULT_CODE_ERROR;
        resultReceiver.send(resultCode, bundle);
    }
}
