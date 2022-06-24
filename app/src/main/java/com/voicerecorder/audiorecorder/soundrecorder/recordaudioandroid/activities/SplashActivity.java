package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities;

import static com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.Common.EXTRA_AUDIO_URI;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.amazic.ads.callback.InterCallback;
import com.amazic.ads.util.Admod;
import com.bumptech.glide.Glide;
import com.github.axet.audiorecorder.R;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.LoadAdError;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.Common;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.utils.SharePrefUtils;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.utils.SystemUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class SplashActivity extends AppCompatActivity {
    boolean guide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set Language
        SystemUtil.setLocale(getBaseContext());
        setContentView(R.layout.activity_splash);

        SharedPreferences sharedPreferences = getSharedPreferences("MY_PREFS_GUIDE", MODE_PRIVATE);
        guide = sharedPreferences.getBoolean("guided", false);


        Uri uri = getIntent().getData();
        if(uri!=null){
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(this, uri);
            String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String length = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            Intent intent = new Intent(this,PlayActivity.class);
            try {
                File file = fileFromContentUri(this, uri);
                if(title==null)title = file.getName();
                intent.putExtra("title",title);
                intent.putExtra("length",length);
                intent.putExtra("path",file.getPath());
            } catch (IOException e) {
                title = getFileName(uri.getPath());
                intent.putExtra("title",title);
                intent.putExtra("length",length);
                intent.putExtra("path",convertLink(uri.getPath()));
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(intent);
                }
            },2000);


        }else{
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity();
                }
            },2000);



        }
        //end
    }

    public void startActivity(){
       if(SharePrefUtils.getCountOpenFirstHelp(this)==0){
            startActivity(new Intent(SplashActivity.this, LanguageStartActivity.class));
        }else{
            startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
        }
       finish();
    }



    File fileFromContentUri(Context context, Uri contentUri) throws IOException {
        String abc = "";
        String fileName = "";
        try {
            String fileDevice = getPath(contentUri);
            File file = new File(fileDevice);
            abc = file.getName();
        } catch (Exception e) {
            String fileExtension = getFileExtension(context, contentUri);
            try {
                String listUri[] = contentUri.toString().split("/");
                String fileNameEx = listUri[listUri.length - 1];
                Log.e("fileNameEx", fileNameEx);
                String urlDecodedTitle = URLDecoder.decode(fileNameEx, StandardCharsets.UTF_8.toString());
                String listUri2[] = urlDecodedTitle.split("_");
                if (listUri2.length > 1) {
                    for (int i = 0; i < listUri2.length - 1; i++) {
                        abc += listUri2[i];
                        if (i < listUri2.length - 2) {
                            abc += "_";
                        }
                    }
                } else {
                    abc = urlDecodedTitle;
                }
                abc += "." + fileExtension;
            } catch (Exception x) {
                if (fileExtension != null) {
                    abc = abc + fileExtension;
                } else {
                    abc = "";
                }
            }
        }
        fileName = abc;
        File tempFile = new File(context.getCacheDir(), fileName);
        tempFile.createNewFile();
        try {
            FileOutputStream oStream = new FileOutputStream(tempFile);
            InputStream inputStream = context.getContentResolver().openInputStream(contentUri);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                oStream.write(buf, 0, len);
            }
            oStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return tempFile;
        }
        return tempFile;
    }

    private String getFileExtension(Context context, Uri uri) {
        String fileType = context.getContentResolver().getType(uri);
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType);
    }
    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String s = cursor.getString(column_index);
        cursor.close();
        return s;
    }
    private String getFileName(String path){
         try {
             return path.substring(path.lastIndexOf("/")+1);
        }catch (Exception e){
             return "unknown";
        }
    }
    private String convertLink(String path){
        return path.replace("/document/raw:","").trim();
    }
}
