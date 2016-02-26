package com.simplemobiletools.draw;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String FOLDER_NAME = "images";
    private static final String FILE_NAME = "simple-draw.png";
    @Bind(R.id.my_canvas) MyCanvas myCanvas;
    @Bind(R.id.color_picker) View colorPicker;
    private int color;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setColor(Color.BLACK);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_share:
                shareImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void shareImage() {
        final String shareTitle = getResources().getString(R.string.share_via);
        final Bitmap bitmap = myCanvas.getBitmap();
        final Intent sendIntent = new Intent();
        final Uri uri = getImageUri(bitmap);
        if (uri == null)
            return;

        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendIntent.setDataAndType(uri, getContentResolver().getType(uri));
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setType("image/*");
        startActivity(Intent.createChooser(sendIntent, shareTitle));
    }

    private Uri getImageUri(Bitmap bitmap) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, bytes);

        final File folder = new File(getCacheDir(), FOLDER_NAME);
        if (!folder.exists()) {
            if (!folder.mkdir())
                return null;
        }

        final File file = new File(folder, FILE_NAME);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bytes.toByteArray());
        } catch (Exception e) {
            Log.e(TAG, "getImageUri 1 " + e.getMessage());
        } finally {
            try {
                if (fileOutputStream != null)
                    fileOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "getImageUri 2 " + e.getMessage());
            }
        }

        return FileProvider.getUriForFile(this, "com.simplemobiletools.draw.fileprovider", file);
    }

    @OnClick(R.id.undo)
    public void undo() {
        myCanvas.undo();
    }

    @OnClick(R.id.color_picker)
    public void pickColor() {
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, color, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int pickedColor) {
                setColor(pickedColor);
            }
        });

        dialog.show();
    }

    private void setColor(int pickedColor) {
        color = pickedColor;
        colorPicker.setBackgroundColor(color);
        myCanvas.setColor(color);
    }
}
