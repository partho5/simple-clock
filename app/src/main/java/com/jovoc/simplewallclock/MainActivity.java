package com.jovoc.simplewallclock;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private View statusOverlay;
    private LinearLayout cardsContainer;
    private ImageButton btnClose;

    private final Handler autoDismissHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoDismissRunnable = this::dismissOverlay;

    private static class StatusItem {
        final String title;
        final int iconResId;

        StatusItem(String title, int iconResId) {
            this.title = title;
            this.iconResId = iconResId;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen awake continuously while app is in foreground
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        // Hide system status and navigation bars for clean wall clock look
        hideSystemUI();

        // Initialize UI components
        statusOverlay = findViewById(R.id.statusOverlay);
        cardsContainer = findViewById(R.id.cardsContainer);
        btnClose = findViewById(R.id.btnClose);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismissOverlay());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        // Check active connections whenever app comes to foreground
        checkAndShowActiveConnections();
    }

    private void checkAndShowActiveConnections() {
        autoDismissHandler.removeCallbacks(autoDismissRunnable);

        List<StatusItem> activeItems = new ArrayList<>();

        // Check if Internet (Wi-Fi or Mobile Data) is ON
        if (isInternetOn()) {
            activeItems.add(new StatusItem("internet", R.drawable.ic_internet));
        }

        // Check if SIM Card is ON / Inserted
        if (isSimCardOn()) {
            activeItems.add(new StatusItem("SIM connection", R.drawable.ic_sim));
        }

        if (!activeItems.isEmpty()) {
            cardsContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(this);

            for (StatusItem item : activeItems) {
                View cardView = inflater.inflate(R.layout.item_status_card, cardsContainer, false);
                ImageView iconView = cardView.findViewById(R.id.cardIcon);
                TextView textView = cardView.findViewById(R.id.cardText);

                iconView.setImageResource(item.iconResId);
                textView.setText(item.title);

                cardsContainer.addView(cardView);
            }

            statusOverlay.setVisibility(View.VISIBLE);

            // Auto dismiss after 3 seconds
            autoDismissHandler.postDelayed(autoDismissRunnable, 3000);
        } else {
            statusOverlay.setVisibility(View.GONE);
        }
    }

    private boolean isInternetOn() {
        // 1. Check Wi-Fi state toggle
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null && wm.isWifiEnabled()) {
                return true;
            }
        } catch (Exception ignored) {}

        // 2. Check Mobile Data state toggle
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null && tm.isDataEnabled()) {
                return true;
            }
        } catch (Exception ignored) {}

        // 3. Check active network capabilities
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Network activeNetwork = cm.getActiveNetwork();
                    if (activeNetwork != null) {
                        NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                        if (caps != null && (
                                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        )) {
                            return true;
                        }
                    }
                } else {
                    @SuppressWarnings("deprecation")
                    android.net.NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
                    if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    private boolean isSimCardOn() {
        // 1. Check TelephonyManager SIM State
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                int simState = tm.getSimState();
                if (simState == TelephonyManager.SIM_STATE_READY) {
                    return true;
                }
            }
        } catch (Exception ignored) {}

        // 2. Check SubscriptionManager active SIM list
        try {
            SubscriptionManager sm = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (sm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                List<SubscriptionInfo> activeList = sm.getActiveSubscriptionInfoList();
                if (activeList != null && !activeList.isEmpty()) {
                    return true;
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    private void dismissOverlay() {
        autoDismissHandler.removeCallbacks(autoDismissRunnable);
        if (statusOverlay != null) {
            statusOverlay.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        // Clear handler callbacks
        autoDismissHandler.removeCallbacks(autoDismissRunnable);

        // Clear FLAG_KEEP_SCREEN_ON when activity is destroyed
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onDestroy();
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
    }
}
