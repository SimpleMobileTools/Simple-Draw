package com.simplemobiletools.draw.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.simplemobiletools.draw.Config;
import com.simplemobiletools.draw.MyCanvas;
import com.simplemobiletools.draw.R;
import com.simplemobiletools.draw.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity implements MyCanvas.PathsChangedListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String FOLDER_NAME = "images";
    private static final String FILE_NAME = "simple-draw.png";
    private static final String SAVE_FOLDER_NAME = "Simple Draw";
    private static final int STORAGE_PERMISSION = 1;

    @BindView(R.id.my_canvas) MyCanvas mMyCanvas;
    @BindView(R.id.undo) View mUndoBtn;
    @BindView(R.id.color_picker) View mColorPicker;

    private String curFileName;

    private int color;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mMyCanvas.setListener(this);

        setColor(Color.BLACK);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Config.newInstance(getApplicationContext()).setIsFirstRun(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                saveImage();
                return true;
            case R.id.menu_share:
                shareImage();
                return true;
            case R.id.about:
                final Intent intent = new Intent(getApplicationContext(), AboutActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImage();
            } else {
                Toast.makeText(this, getResources().getString(R.string.no_permissions), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION);
            return;
        }

        final View saveFileView = getLayoutInflater().inflate(R.layout.save_file, null);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.save_file));

        final EditText fileNameET = (EditText) saveFileView.findViewById(R.id.file_name);
        fileNameET.setText(curFileName);
        builder.setView(saveFileView);

        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Cancel", null);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String fileName = fileNameET.getText().toString().trim();

                if (!fileName.isEmpty()) {
                    if (saveFile(fileName + ".png")) {
                        curFileName = fileName;
                        Utils.showToast(getApplicationContext(), R.string.saving_ok);
                        alertDialog.dismiss();
                    } else {
                        Utils.showToast(getApplicationContext(), R.string.saving_error);
                    }
                } else {
                    Utils.showToast(getApplicationContext(), R.string.enter_file_name);
                }
            }
        });
    }

    private boolean saveFile(final String fileName) {
        final String path = Environment.getExternalStorageDirectory().toString();
        final File directory = new File(path, SAVE_FOLDER_NAME);
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                return false;
            }
        }

        final Bitmap bitmap = mMyCanvas.getBitmap();
        FileOutputStream out = null;
        try {
            final File file = new File(directory, fileName);
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            MediaScannerConnection.scanFile(getApplicationContext(), new String[]{file.getAbsolutePath()}, null, null);
        } catch (Exception e) {
            Log.e(TAG, "MainActivity SaveFile " + e.getMessage());
            return false;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "MainActivity SaveFile 2 " + e.getMessage());
            }
        }

        return true;
    }

    private void shareImage() {
        final String shareTitle = getResources().getString(R.string.share_via);
        final Bitmap bitmap = mMyCanvas.getBitmap();
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
        mMyCanvas.undo();
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
        mColorPicker.setBackgroundColor(color);
        mMyCanvas.setColor(color);
    }

    @Override
    public void pathsChanged(int cnt) {
        mUndoBtn.setVisibility(cnt > 0 ? View.VISIBLE : View.GONE);
    }
}
