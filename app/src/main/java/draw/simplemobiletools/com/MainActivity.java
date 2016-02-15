package draw.simplemobiletools.com;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity {
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
