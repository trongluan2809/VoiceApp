package com.voicerecorder.axet.audiolibrary.encoders;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.preference.PreferenceManager;

import com.github.axet.androidlibrary.app.Storage;
import com.voicerecorder.axet.audiolibrary.app.MainApplication;
import com.voicerecorder.axet.audiolibrary.app.RawSamples;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

public class OnFlyEncoding implements Encoder {
    public Uri targetUri;
    public Encoder e;
    public ParcelFileDescriptor fd;

    public OnFlyEncoding(Storage storage, Uri targetUri, RawSamples.Info info) {
        Context context = storage.getContext();

        this.targetUri = targetUri;

        FileDescriptor out;

        String s = targetUri.getScheme();
        if (s.equals(ContentResolver.SCHEME_CONTENT)) {
            Uri root = Storage.getDocumentTreeUri(targetUri);
            Uri o = null;
            try {
                o = Storage.createFile(context, root, Storage.getDocumentChildPath(targetUri));
            } catch (FileNotFoundException fileNotFoundException) {
                fileNotFoundException.printStackTrace();
            }
            ContentResolver resolver = context.getContentResolver();
            try {
                fd = resolver.openFileDescriptor(o, "rw");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            out = fd.getFileDescriptor();
        } else if (s.equals(ContentResolver.SCHEME_FILE)) {
            File f = Storage.getFile(targetUri);
            try {
                fd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_READ_WRITE);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            out = fd.getFileDescriptor();
        } else {
            throw new Storage.UnknownUri();
        }

        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        String ext = shared.getString(MainApplication.PREFERENCE_ENCODING, "");

        e = Factory.getEncoder(context, ext, info, out);
    }

    public OnFlyEncoding(Storage storage, File f, RawSamples.Info info) {
        Context context = storage.getContext();

        this.targetUri = Uri.fromFile(f);

        try {
            fd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_READ_WRITE);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        FileDescriptor out = fd.getFileDescriptor();

        String ext = Storage.getExt(f);

        e = Factory.getEncoder(context, ext, info, out);
    }


    @Override
    public void encode(short[] buf, int pos, int len) {
        e.encode(buf, pos, len);
    }

    @Override
    public void close() {
        if (e != null) {
            e.close();
            e = null;
        }
        if (fd != null) {
            try {
                fd.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            fd = null;
        }
    }
}
