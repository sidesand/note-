package com.sidesand.note.page;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.sidesand.note.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public abstract class BaseActivity extends AppCompatActivity {

    public final String TAG = "BaseActivity";
    public final String ACTION = "NIGHT_SWITCH";
    protected BroadcastReceiver receiver;
    protected IntentFilter filter;
    private SharedPreferences settingSp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingSp = getSharedPreferences("settings", MODE_PRIVATE);

        setNightMode();
        applyStatusBarColor();

        initializeBroadcastReceiver();
    }

    private void applyStatusBarColor() {
        TypedValue typedValue = new TypedValue();
        this.getTheme().resolveAttribute(R.attr.tvBackground, typedValue, true);
        int color = ContextCompat.getColor(this, typedValue.resourceId);
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        getWindow().setStatusBarColor(color);
    }

    private void initializeBroadcastReceiver() {
        filter = new IntentFilter();
        filter.addAction(ACTION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                needRefresh();
            }
        };

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
    }

    public boolean isNightMode() {
        return settingSp.getBoolean("nightMode", false);
    }

    public void setNightMode() {
        if (isNightMode()) this.setTheme(R.style.NightTheme);
        else setTheme(R.style.DayTheme);
    }

    protected abstract void needRefresh();

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }


    public long calStrToSec(String date) throws ParseException { // decode calendar date to second
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        long secTime = format.parse(date).getTime();
        return secTime;
    }
}
   