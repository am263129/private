package com.emanuelef.remote_capture.utils;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class makeZip {
    static final int BUFFER = 2048;

    ZipOutputStream out;
    byte data[];

    public makeZip(String name) {
        FileOutputStream dest = null;
        try {
            dest = new FileOutputStream(name);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        out = new ZipOutputStream(new BufferedOutputStream(dest));
        data = new byte[BUFFER];
    }

    public void addZipFile(String name,String path) {
        Log.v("addFile", "Adding: ");
        FileInputStream fi = null;
        try {
            fi = new FileInputStream(path);
            Log.v("addFile", "Adding: ");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.v("atch", "Adding: ");
        }
        BufferedInputStream origin = new BufferedInputStream(fi, BUFFER);
        ZipEntry entry = new ZipEntry(name);
        try {
            out.putNextEntry(entry);
            Log.v("put", "Adding: ");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        int count;
        try {
            while ((count = origin.read(data, 0, BUFFER)) != -1) {
                out.write(data, 0, count);
                //Log.v("Write", "Adding: "+origin.read(data, 0, BUFFER));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.v("catch", "Adding: ");
        }
        try {
            origin.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void closeZip() {
        try {
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}