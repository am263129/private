package com.emanuelef.remote_capture.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.UUID;

import com.emanuelef.remote_capture.Utils;
import com.emanuelef.remote_capture.activities.MainActivity;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

public class AzureUploader  extends AsyncTask<String, Void, Void> {

    private File file;
    private Context context;

    public AzureUploader (Context context,File file) {
        this.file = file;
        this.context = context;
    }

    @Override
    protected Void doInBackground(String... arg0) {

        try {
            Log.e("Upload Azure","stat");
            // Setup the cloud storage account.
            CloudStorageAccount account = CloudStorageAccount
                    .parse(Utils.storageConnectionString);

            // Create a blob service client
            CloudBlobClient blobClient = account.createCloudBlobClient();

            // Get a reference to a container
            // The container name must be lower case
            // Append a random UUID to the end of the container name so that
            // this sample can be run more than once in quick succession.
            CloudBlobContainer container = blobClient.getContainerReference("sniffer-app");

            // Create the container if it does not exist
            container.createIfNotExists();
            CloudBlockBlob blob = container
                    .getBlockBlobReference(Utils.getDeviceId(context)+"/"+Utils.getUniqueUploadFileName(context));
            blob.uploadFromFile(file.getPath());
            Log.e("Upload","Azure end");
            file.delete();

        } catch (Exception t) {
            Log.e("Upload Azure","error");
            t.printStackTrace();
        }
        return null;
    }
}