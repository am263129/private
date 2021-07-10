/*
 * This file is part of PCAPdroid.
 *
 * PCAPdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PCAPdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PCAPdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2020-21 - Emanuele Faranda
 */

package com.emanuelef.remote_capture.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.VpnService;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.internal.Constants;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.emanuelef.remote_capture.fragments.ConnectionsFragment;
import com.emanuelef.remote_capture.fragments.StatusFragment;
import com.emanuelef.remote_capture.interfaces.AppStateListener;
import com.emanuelef.remote_capture.model.AppState;
import com.emanuelef.remote_capture.CaptureService;
import com.emanuelef.remote_capture.model.GlobalSetting;
import com.emanuelef.remote_capture.model.Prefs;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import com.emanuelef.remote_capture.utils.AzureUploader;
import com.emanuelef.remote_capture.utils.S3Service;
import com.emanuelef.remote_capture.utils.Util;
import com.emanuelef.remote_capture.utils.makeZip;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.JsonObject;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipOutputStream;

import cat.ereza.customactivityoncrash.config.CaocConfig;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    private ViewPager2 mPager;
    private TabLayout mTabLayout;
    private AppState mState;
    private AppStateListener mListener;
    private Uri mPcapUri;
    private BroadcastReceiver mReceiver;
    private String mPcapFname;
    private DrawerLayout mDrawer;
    private SharedPreferences mPrefs;
    private boolean usingMediaStore;

    private static final String TAG = "Main";

    private static final int POS_STATUS = 0;
    private static final int POS_CONNECTIONS = 1;
    private static final int TOTAL_COUNT = 2;

    public static final String FILTER_EXTRA = "filter";

    public static final String TELEGRAM_GROUP_NAME = "PCAPdroid";
    public static final String GITHUB_PROJECT_URL = "https://github.com/emanuele-f/PCAPdroid";
    public static final String GITHUB_DOCS_URL = "https://emanuele-f.github.io/PCAPdroid";
    public static final String DONATE_URL = "https://emanuele-f.github.io/PCAPdroid/donate";


    //Rolland
    private static final int INDEX_NOT_CHECKED = -1;
    private static final int UPLOAD_REQUEST_CODE = 0;
    private static final int UPLOAD_IN_BACKGROUND_REQUEST_CODE = 1;
    private boolean humanServiceRun = false;
    private boolean humanDisable = false;
    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = false;
    // Whether there is a mobile connection.
    private static boolean mobileConnected = false;
    private static boolean uploadData = true;
    private NetworkReceiver receiver = new NetworkReceiver();


    private AmazonS3Client s3Client;
    private TransferUtility transferUtility;
    private ProgressDialog configProgress;
    public static MainActivity instance;
    // The SimpleAdapter adapts the data about transfers to rows in the UI
    static SimpleAdapter simpleAdapter;
    // A List of all transfers
    static List<TransferObserver> observers;
    /**
     * This map is used to provide data to the SimpleAdapter above. See the
     * fillMap() function for how it relates observers to rows in the displayed
     * activity.
     */
    static ArrayList<HashMap<String, Object>> transferRecordMaps;

    // Which row in the UI is currently checked (if any)
    static int checkedIndex;

    // Reference to the utility class
    static Util util;

    /**
     * call Upload method data on every duration time.
     * Rolland
     */
    public Handler uploadHandler = new Handler(){
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void handleMessage(@NonNull Message msg) {
            CreateUploadFile();
            switch (GlobalSetting.UT){
                case 1:
                    uploadS3();
                    break;
                case 2:
                    uploadAzure();
                    break;
                case 3:
                    uploadS3();
                    uploadAzure();
                    break;
                default:
                    break;
            }

            if (uploadData && humanServiceRun) {
                Log.e(TAG,"Start Auto Upload engine");
                uploadHandler.sendEmptyMessageDelayed(0,GlobalSetting.UD*1000);
            } else {
                Toast.makeText(MainActivity.this, "Stop Uploading", Toast.LENGTH_SHORT).show();
                Log.e(TAG,"Stop Uploading");
                uploadHandler.removeMessages(0);
            }
            super.handleMessage(msg);
        }
    };
    /**
     * when downloading is not responding with in 5
     * set config manually.
     */
    public Handler configHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            ConfigLoader configLoader = new ConfigLoader();
            configLoader.execute();
            super.handleMessage(msg);
        }
    };

    public Handler EngineHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            humanDisable = false;
            if(humanServiceRun){
                Log.e(TAG,"Resume service");
                startCaptureService();
            }
            super.handleMessage(msg);
        }
    };

    private final ActivityResultLauncher<Intent> captureServiceLauncher =
            registerForActivityResult(new StartActivityForResult(), this::captureServiceResult);
    private final ActivityResultLauncher<Intent> pcapFileLauncher =
            registerForActivityResult(new StartActivityForResult(), this::pcapFileResult);
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new RequestPermission(), isGranted ->
                Log.d(TAG, "Write permission " + (isGranted ? "granted" : "denied"))
            );

    static {
        System.loadLibrary("vpnproxy-jni");
        System.loadLibrary("pcapd");
        System.loadLibrary("ndpi");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);


        initAppState();
        checkPermissions();
        updateConnectedFlags();

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPcapUri = CaptureService.getPcapUri();
        instance = this;
        //Rolland init S3
        s3Client =   new AmazonS3Client( new BasicAWSCredentials( Utils.ACCESSKEY, Utils.SECRETKEY ),Region.getRegion(Regions.US_WEST_2));
        transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(s3Client)
                        .build();
        TransferNetworkLossHandler.getInstance(getApplicationContext());

        CaocConfig.Builder.create()
                .errorDrawable(R.drawable.ic_app_crash)
                .apply();


        //download config file from s3 and set setting.
        configProgress = new ProgressDialog(this);
        configProgress.setTitle("Updating Config");
        configProgress.setMessage("Downloading config file from server");
        configProgress.show();
        inintConfig();
//        ConfigLoader setting = new ConfigLoader();
//        setting.execute();

        mTabLayout = findViewById(R.id.tablayout);
        mPager = findViewById(R.id.pager);

        setupTabs();

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, filter);

        /* Register for service status */
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String status = intent.getStringExtra(CaptureService.SERVICE_STATUS_KEY);

                if (status != null) {
                    if (status.equals(CaptureService.SERVICE_STATUS_STARTED)) {
                        appStateRunning();
                    } else if (status.equals(CaptureService.SERVICE_STATUS_STOPPED)) {
                        // The service may still be active (on premature native termination)
                        if (CaptureService.isServiceActive())
                            CaptureService.stopService();

                        if((mPcapUri != null) && (Prefs.getDumpMode(mPrefs) == Prefs.DumpMode.PCAP_FILE)) {
                            showPcapActionDialog(mPcapUri);
                            mPcapUri = null;
                            mPcapFname = null;
                        }

                        appStateReady();
                    }
                }
            }
        };

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mReceiver, new IntentFilter(CaptureService.ACTION_SERVICE_STATUS));
    }


    /**
     * Download config file from AWS server.
     */
    private void inintConfig(){
        configHandler.sendEmptyMessageDelayed(0,5000);
        File file = new File(getApplicationContext().getFilesDir(), "configure.json");
        TransferObserver downloadObserver =
                transferUtility.download("sniffer-app","Config/configure.json", file);
        downloadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    configHandler.removeMessages(0);
                    Log.d(TAG,"Download config file finished");
                    ConfigLoader setting = new ConfigLoader();
                    setting.execute();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float)bytesCurrent/(float)bytesTotal) * 100;
                int percentDone = (int)percentDonef;
                Log.e("Progress",percentDone +"");
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e("Error", ex.toString());
                configHandler.removeMessages(0);
                ConfigLoader setting = new ConfigLoader();
                setting.execute();
                if(configProgress.isShowing())
                    configProgress.dismiss();
            }

        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mReceiver != null)
            LocalBroadcastManager.getInstance(this)
                    .unregisterReceiver(mReceiver);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupNavigationDrawer();
    }

    private void setupNavigationDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navView = findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(this);
        View header = navView.getHeaderView(0);

        TextView appVer = header.findViewById(R.id.app_version);
        String verStr = Utils.getAppVersion(this);
        appVer.setText(verStr);
        appVer.setOnClickListener((ev) -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_PROJECT_URL + "/tree/" + verStr));
            startActivity(browserIntent);
        });

        if(Prefs.isRootCaptureEnabled(mPrefs)) {
            Menu navMenu = navView.getMenu();
            navMenu.findItem(R.id.open_root_log).setVisible(true);
        }
    }

    @Override
    public void onBackPressed() {
        if(mDrawer.isDrawerOpen(GravityCompat.START))
            mDrawer.closeDrawer(GravityCompat.START, true);
        else {
            if(mPager.getCurrentItem() == POS_CONNECTIONS) {
                Fragment fragment = getFragment(ConnectionsFragment.class);

                if((fragment != null) && ((ConnectionsFragment)fragment).onBackPressed())
                    return;
            }

            super.onBackPressed();
        }
    }

    private Fragment getFragment(Class targetClass) {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();

        for(Fragment fragment : fragments) {
            if(targetClass.isInstance(fragment))
                return fragment;
        }

        return null;
    }

    private void checkPermissions() {
        String fname = "test.pcap";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, fname);

        if(!Utils.supportsFileDialog(this, intent)) {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Needed to write file on devices which do not support ACTION_CREATE_DOCUMENT
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                }
            }
        }
    }

    private static class MainStateAdapter extends FragmentStateAdapter {
        MainStateAdapter(final FragmentActivity fa) { super(fa); }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Log.d(TAG, "createFragment");

            switch (position) {
                default: // Deliberate fall-through to status tab
                case POS_STATUS:
                    return new StatusFragment();
                case POS_CONNECTIONS:
                    return new ConnectionsFragment();
            }
        }

        @Override
        public int getItemCount() {  return TOTAL_COUNT;  }

        public int getPageTitle(final int position) {
            switch (position) {
                default: // Deliberate fall-through to status tab
                case POS_STATUS:
                    return R.string.status;
                case POS_CONNECTIONS:
                    return R.string.connections_view;
            }
        }
    }

    private void setupTabs() {
        final MainStateAdapter stateAdapter = new MainStateAdapter(this);
        mPager.setAdapter(stateAdapter);

        new TabLayoutMediator(mTabLayout, mPager, (tab, position) ->
                tab.setText(getString(stateAdapter.getPageTitle(position)))
        ).attach();

        checkFilterIntent(getIntent());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // This is required to properly handle the DPAD down press on Android TV, to properly
        // focus the tab content
        if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            View view = getCurrentFocus();

            Log.d(TAG, "onKeyDown focus " + view.getClass().getName());

            if(view instanceof TabLayout.TabView) {
                int pos = mPager.getCurrentItem();
                View focusOverride = null;

                Log.d(TAG, "TabLayout.TabView focus pos " + pos);

                if(pos == POS_STATUS)
                    focusOverride = findViewById(R.id.main_screen);
                else if(pos == POS_CONNECTIONS)
                    focusOverride = findViewById(R.id.connections_view);

                if(focusOverride != null) {
                    focusOverride.requestFocus();
                    return true;
                }
            }
        } else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            // Clicking "right" from the connections view goes to the fab down item
            if(mPager.getCurrentItem() == POS_CONNECTIONS) {
                RecyclerView rview = findViewById(R.id.connections_view);

                if(rview.getFocusedChild() != null) {
                    Log.d(TAG, "onKeyDown (right) focus " + rview.getFocusedChild());

                    View fab = findViewById(R.id.fabDown);

                    if(fab != null) {
                        fab.requestFocus();
                        return true;
                    }
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkFilterIntent(intent);
    }

    private void checkFilterIntent(Intent intent) {
        if(intent != null) {
            String filter = intent.getStringExtra(FILTER_EXTRA);

            if((filter != null) && (!filter.isEmpty())) {
                // Move to the connections tab
                Log.d(TAG, "FILTER_EXTRA " + filter);

                mPager.setCurrentItem(POS_CONNECTIONS, false);
            }
        }
    }

    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.item_apps) {
            if(CaptureService.getConnsRegister() != null) {
                Intent intent = new Intent(MainActivity.this, AppsActivity.class);
                startActivity(intent);
            } else
                Utils.showToast(this, R.string.capture_not_started);
        } else if(id == R.id.edit_whitelist) {
            Intent intent = new Intent(MainActivity.this, WhitelistActivity.class);
            startActivity(intent);
        } else if(id == R.id.open_root_log) {
            Intent intent = new Intent(MainActivity.this, LogviewActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_donate) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DONATE_URL));
            startActivity(browserIntent);
        } else if (id == R.id.action_open_telegram) {
            openTelegram();
        } else if (id == R.id.action_open_user_guide) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_DOCS_URL));
            startActivity(browserIntent);
        } else if (id == R.id.action_rate_app) {
            rateApp();
        } else if (id == R.id.action_stats) {
            if(mState == AppState.running) {
                Intent intent = new Intent(MainActivity.this, StatsActivity.class);
                startActivity(intent);
            } else
                Utils.showToast(this, R.string.capture_not_running);
        } else if (id == R.id.action_about) {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_share_app) {
            String description = getString(R.string.about_text);
            String getApp = getString(R.string.get_app);
            String url = "http://play.google.com/store/apps/details?id=" + this.getPackageName();

            Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(android.content.Intent.EXTRA_TEXT, description + "\n" + getApp + "\n" + url);

            startActivity(Intent.createChooser(intent, getResources().getString(R.string.share)));
        }

        return false;
    }

    public void setAppStateListener(AppStateListener listener) {
        mListener = listener;
    }

    private void notifyAppState() {
        if(mListener != null)
            mListener.appStateChanged(mState);
    }

    public void appStateReady() {
        mState = AppState.ready;
        notifyAppState();
    }

    public void appStateStarting() {
        mState = AppState.starting;
        notifyAppState();
    }

    public void appStateRunning() {
        mState = AppState.running;
        notifyAppState();
    }

    public void appStateStopping() {
        mState = AppState.stopping;
        notifyAppState();
    }

    private void openTelegram() {
        Intent intent;

        try {
            getPackageManager().getPackageInfo("org.telegram.messenger", 0);

            // Open directly into the telegram app
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=" + TELEGRAM_GROUP_NAME));
        } catch (Exception e) {
            // Telegram not found, open in the browser
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://t.me/" + TELEGRAM_GROUP_NAME));
        }

        startActivity(intent);
    }

    private void rateApp() {
        try {
            /* If playstore is installed */
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + this.getPackageName())));
        } catch (android.content.ActivityNotFoundException e) {
            /* If playstore is not available */
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_start) {
            humanServiceRun = !CaptureService.isServiceActive();
            if(humanServiceRun && GlobalSetting.UA){
                uploadHandler.sendEmptyMessageDelayed(0,GlobalSetting.UD * 1000);
            }
            if(!humanDisable || CaptureService.isServiceActive())
                toggleService();
            else {
                Toast.makeText(this,"Please wait for a while",Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void captureServiceResult(final ActivityResult result) {
        if(result.getResultCode() == RESULT_OK) {
            captureServiceOk();
        } else {
            Log.w(TAG, "VPN request failed");
            appStateReady();
        }
    }

    private void captureServiceOk() {
        final Intent intent = new Intent(MainActivity.this, CaptureService.class);
        final Bundle bundle = new Bundle();

        if((mPcapUri != null) && (Prefs.getDumpMode(mPrefs) == Prefs.DumpMode.PCAP_FILE))
            bundle.putString(Prefs.PREF_PCAP_URI, mPcapUri.toString());

        intent.putExtra("settings", bundle);

        Log.d(TAG, "onActivityResult -> start CaptureService");

        ContextCompat.startForegroundService(this, intent);
    }

    private void pcapFileResult(final ActivityResult result) {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            startWithPcapFile(result.getData().getData());
        } else {
            mPcapUri = null;
        }
    }

    private void startWithPcapFile(Uri uri) {
        mPcapUri = uri;
        mPcapFname = null;

        Log.d(TAG, "PCAP to write: " + mPcapUri.toString());
        toggleService();
    }

    private void initAppState() {
        boolean is_active = CaptureService.isServiceActive();

        if (!is_active)
            appStateReady();
        else
            appStateRunning();
    }

    public void startCaptureService() {
        appStateStarting();
        if(Prefs.isRootCaptureEnabled(mPrefs)) {
            captureServiceOk();
            return;
        }

        Intent vpnPrepareIntent = VpnService.prepare(MainActivity.this);
        if (vpnPrepareIntent != null)
            captureServiceLauncher.launch(vpnPrepareIntent);
        else
            captureServiceOk();
    }

    public void toggleService() {

        if (CaptureService.isServiceActive()) {
            appStateStopping();
            CaptureService.stopService();
        } else {

            if((mPcapUri == null) && (Prefs.getDumpMode(mPrefs) == Prefs.DumpMode.PCAP_FILE)) {
                openFileSelector();
                return;
            }
            if(!Prefs.isRootCaptureEnabled(mPrefs) && Utils.hasVPNRunning(this)) {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.existing_vpn_confirm)
                        .setPositiveButton(R.string.yes, (dialog, whichButton) -> startCaptureService())
                        .setNegativeButton(R.string.no, (dialog, whichButton) -> {})
                        .show();
            } else
                startCaptureService();
        }
    }

    public void openFileSelector() {
        boolean noFileDialog = false;
        String fname = Utils.getUniquePcapFileName(this);
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, fname);

        if(Utils.supportsFileDialog(this, intent)) {
            try {
                pcapFileLauncher.launch(intent);
            } catch (ActivityNotFoundException e) {
                noFileDialog = true;
            }
        } else
            noFileDialog = true;

        if(noFileDialog) {
            Log.w(TAG, "No app found to handle file selection");

            // Pick default path
            Uri uri = Utils.getInternalStorageFile(this, fname);

            if(uri != null) {
                usingMediaStore = true;
                startWithPcapFile(uri);
            } else
                Utils.showToastLong(this, R.string.no_activity_file_selection);
        }
    }

    public void showPcapActionDialog(Uri pcapUri) {
        Cursor cursor;

        Log.d(TAG, "showPcapActionDialog: " + pcapUri.toString());

        try {
            cursor = getContentResolver().query(pcapUri, null, null, null, null);
        } catch (Exception e) {
            return;
        }

        if((cursor == null) || !cursor.moveToFirst())
            return;

        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
        long file_size = !cursor.isNull(sizeIndex) ? cursor.getLong(sizeIndex) : -1;
        String fname = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
        cursor.close();

        // If file is empty, delete it
        if(file_size == 0) {
            Log.d(TAG, "PCAP file is empty, deleting");

            try {
                if(usingMediaStore)
                    getContentResolver().delete(pcapUri, null, null);
                else
                    DocumentsContract.deleteDocument(getContentResolver(), pcapUri);
            } catch (FileNotFoundException | UnsupportedOperationException e) {
                e.printStackTrace();
            }

            return;
        }

        String message = String.format(getResources().getString(R.string.pcap_file_action), fname, Utils.formatBytes(file_size));

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(message);

        builder.setPositiveButton(R.string.share, (dialog, which) -> {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("application/cap");
            sendIntent.putExtra(Intent.EXTRA_STREAM, pcapUri);
            startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.share)));
        });
        builder.setNegativeButton(R.string.delete, (dialog, which) -> {
            Log.d(TAG, "Deleting PCAP file" + pcapUri.getPath());
            boolean deleted = false;

            try {
                if(usingMediaStore)
                    deleted = (getContentResolver().delete(pcapUri, null, null) == 1);
                else
                    deleted = DocumentsContract.deleteDocument(getContentResolver(), pcapUri);
            } catch (FileNotFoundException | UnsupportedOperationException e) {
                e.printStackTrace();
            }

            if(!deleted)
                Utils.showToast(MainActivity.this, R.string.delete_error);

            dialog.cancel();
        });
        builder.setNeutralButton(R.string.ok, (dialog, which) -> dialog.cancel());

        builder.create().show();
    }

    public AppState getState() {
        return(mState);
    }

    public String getPcapFname() {
        if((mState == AppState.running) && (mPcapUri != null)) {
            if(mPcapFname != null)
                return mPcapFname;

            Cursor cursor;

            try {
                cursor = getContentResolver().query(mPcapUri, null, null, null, null);
            } catch (Exception e) {
                return null;
            }

            if((cursor == null) || !cursor.moveToFirst())
                return null;

            String fname = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            cursor.close();

            mPcapFname = fname;
            return fname;
        }

        return null;
    }

    public void startUplaodEngine(){
        uploadHandler.sendEmptyMessage(0);
    }

    public void uploadS3(){

        File file = new File(getApplicationContext().getFilesDir(), "s3_buffer.zip");
        if(file.exists()) {
            if (file.length() == 0){
                Log.e(TAG,"Skip: Empty File ");
            }
            else {
                if(CaptureService.isServiceActive()) {
                    CaptureService.stopService();
                }
                UploadProof();
                String filename = Utils.getDeviceId(MainActivity.this) + "/" + Utils.getUniqueUploadFileName(MainActivity.this);
                Log.e("FIle name", filename);
                TransferObserver uploadObserver =
                        transferUtility.upload("sniffer-app", "PCAP/" + filename, file);
                EngineHandler.sendEmptyMessageDelayed(0,20000);
                humanDisable = true;
                uploadObserver.setTransferListener(new TransferListener() {

                    @Override
                    public void onStateChanged(int id, TransferState state) {

                        if (TransferState.COMPLETED == state) {
                            EngineHandler.removeMessages(0);
                            EngineHandler.sendEmptyMessage(0);
                            file.delete();//delete zip file when upload complete.
                        }
                    }
                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                        int percentDone = (int) percentDonef;
                        Log.e("Progress", percentDone + "");
                    }
                    @Override
                    public void onError(int id, Exception ex) {
                        Log.e("Error", ex.toString());
                        EngineHandler.removeMessages(0);
                        EngineHandler.sendEmptyMessage(0);
                    }
                });
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void uploadAzure(){

        File file = new File(getApplicationContext().getFilesDir(), "azure_buffer.zip");

        if(file.exists()){
            if (file.length() == 0){
                Log.e(TAG,"Skip: Empty File ");
            }
            else {
                if(CaptureService.isServiceActive()) {
                    Log.e(TAG,"Stop service");
                    CaptureService.stopService();
                }
                UploadProof();
                AzureUploader uploader = new AzureUploader(this, file);
                uploader.execute();
                EngineHandler.sendEmptyMessageDelayed(0,20000);
                humanDisable = true;
            }
        }

    }

    /**
     * Upload proof file to
     */
    private void UploadProof(){
        File file = new File(getApplicationContext().getFilesDir(), "proof");
        if(!file.exists()){
            try {
                FileOutputStream FOS = new FileOutputStream(file);
                FOS.write("DEVICE ID: ".getBytes());
                FOS.write(Utils.getDeviceId(this).getBytes());
                FOS.write("\n".getBytes());
                FOS.write("LAST UPDATE TIME: ".getBytes());
                DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm");
                String date = df.format(Calendar.getInstance().getTime());
                FOS.write(date.getBytes());
                FOS.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        String filename = "PROOF_DEVICE_ID:"+Utils.getDeviceId(MainActivity.this);
        Log.e("FIle name", filename);
        TransferObserver uploadObserver =
                transferUtility.upload("sniffer-app", "PCAP/PROOF/" + filename, file);
        uploadObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    Log.e(TAG,"Uploading proof completed");
                }
            }
            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            }
            @Override
            public void onError(int id, Exception ex) {
                Log.e("Uploading proof Error", ex.toString());
            }
        });
    }

    /**
     * copy pcap file to double file-for s3 and azure.
     * and delete original pcap file
     */
    private void CreateUploadFile(){
        File pcapFile = new File(getApplicationContext().getFilesDir(), "upload_buffer.pcap");
        File s3buffer = new File(getApplicationContext().getFilesDir(), "s3_buffer.zip");
        File azurebuffer = new File(getApplicationContext().getFilesDir(), "azure_buffer.zip");
        makeZip s3zip = new makeZip(s3buffer.getPath());
        makeZip azure3zip = new makeZip(azurebuffer.getPath());
        s3buffer.deleteOnExit();
        azurebuffer.deleteOnExit();
        try {
            if(pcapFile.exists() && pcapFile.length() > 0) {
                s3zip.addZipFile("s3_buffer.pcap", pcapFile.getPath());
                azure3zip.addZipFile("azure_buffer.pcap", pcapFile.getPath());
                s3zip.closeZip();
                azure3zip.closeZip();
                pcapFile.delete();
                CaptureService.mBufferFirstStreamWrite = true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    /**
     * set initial value to flags
     */
    public void updateConnectedFlags() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
        } else {
            wifiConnected = false;
            mobileConnected = false;
        }
    }


    /**
     * Broadcast receiver to monitoring network status change.
     */

    public class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conn =  (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = conn.getActiveNetworkInfo();
            // Checks the user prefs and the network connection. Based on the result, decides whether
            // to refresh the display or keep the current display.
            // If the userpref is Wi-Fi only, checks to see if the device has a Wi-Fi connection.
            if ((!Prefs.isEnabledUseCell(mPrefs)) && networkInfo != null
                    && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                // If device has its Wi-Fi connection, start upload engine

                uploadData = true;
                if(GlobalSetting.UA)
                    uploadHandler.sendEmptyMessage(0);
//                Toast.makeText(context, "WIFI enabled", Toast.LENGTH_SHORT).show();
                Log.e(TAG,"WIFI enabled");

                // If the setting is ANY network and there is a network connection, start upload engine
            } else if (Prefs.isEnabledUseCell(mPrefs) && networkInfo != null) {
                uploadData = true;
                if(GlobalSetting.UA)
                    uploadHandler.sendEmptyMessage(0);
                // Otherwise, the app can't download content--either because there is no network
                // connection (mobile or Wi-Fi), or because the pref setting is WIFI, and there
                // is no Wi-Fi connection.
                // Sets uploadData to false.
            } else {
                uploadData = false;
                uploadHandler.removeMessages(0);
                Log.e(TAG,"Connection has been Lost.");
//                Toast.makeText(context, "Connection has been Lost.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**
     * set Global setting from downloaded config file.
     * or from shared preference.
     */
    public class ConfigLoader extends AsyncTask<Void,Void,Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            File file = new File(getApplicationContext().getFilesDir(), "configure.json");
            if(file.exists()){
                try {
                    String baseconfig = "";
                    FileInputStream FIS = new FileInputStream(file.getPath());
                    BufferedReader myReader = new BufferedReader(new InputStreamReader(FIS));
                    String aDataRow = "";
                    while ((aDataRow = myReader.readLine()) != null) {
                        baseconfig += aDataRow;
                    }
                    myReader.close();
                    JSONObject config = new JSONObject(baseconfig);
                    GlobalSetting.setUD(config.getInt("UD"));
                    GlobalSetting.setUT(1);
                    GlobalSetting.setUA(config.getBoolean("UA"));
                    JSONArray filter = config.getJSONArray("FILTER");
                    String filter_role = "";
                    for(int i = 0;i<filter.length(); i++){
                        JSONObject filterset = new JSONObject(filter.get(i).toString());
                        if (i !=0)filter_role+=",";
                        filter_role+=filterset.getInt("PORT")+":"+filterset.getString("PROTOCOL");
                    }
                    GlobalSetting.setFILTER(filter_role);
                    GlobalSetting.SaveGlobalSetting(MainActivity.this);
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            else{
                GlobalSetting.LoadGlobalSetting(MainActivity.this);
            }

            Log.e("UD",GlobalSetting.UD+"");
            Log.e("UT",GlobalSetting.UT+"");
            Log.e("FILTER",GlobalSetting.FILTER+"");
            Log.e("UA",GlobalSetting.UA+"");

            if(configProgress.isShowing())
                configProgress.dismiss();
            return null;
        }
    }
}
