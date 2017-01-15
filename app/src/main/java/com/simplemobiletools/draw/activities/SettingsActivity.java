package com.simplemobiletools.draw.activities;

import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.widget.SwitchCompat;

import com.simplemobiletools.draw.Config;
import com.simplemobiletools.draw.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingsActivity extends SimpleActivity {
    @BindView(R.id.settings_dark_theme) SwitchCompat mDarkThemeSwitch;
    @BindView(R.id.settings_brush_size) SwitchCompat mBrushSizeSwitch;

    private static Config mConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mConfig = Config.newInstance(getApplicationContext());
        ButterKnife.bind(this);

        setupSwitches();
    }

    private void setupSwitches() {
        mDarkThemeSwitch.setChecked(mConfig.getIsDarkTheme());
        mBrushSizeSwitch.setChecked(mConfig.getIsStrokeWidthBarEnabled());
    }

    @OnClick(R.id.settings_dark_theme_holder)
    public void handleDarkTheme() {
        mDarkThemeSwitch.setChecked(!mDarkThemeSwitch.isChecked());
        mConfig.setIsDarkTheme(mDarkThemeSwitch.isChecked());
        restartActivity();
    }

    @OnClick(R.id.settings_brush_size_holder)
    public void handleBrushSize() {
        mBrushSizeSwitch.setChecked(!mBrushSizeSwitch.isChecked());
        mConfig.setIsStrokeWidthBarEnabled(mBrushSizeSwitch.isChecked());
    }

    private void restartActivity() {
        TaskStackBuilder.create(getApplicationContext()).addNextIntentWithParentStack(getIntent()).startActivities();
    }
}
