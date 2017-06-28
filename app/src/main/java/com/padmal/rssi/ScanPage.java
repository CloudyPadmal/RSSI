package com.padmal.rssi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class ScanPage extends AppCompatActivity {

    private TextView scanResults;
    private EditText fileName, turnCount;
    private FloatingActionButton startScan;
    private WifiManager WIFI;
    private int TURNS;
    private Timer wifiTimer;
    private Boolean scanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initiate Layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_page);
        // Initiate tool bars
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Initiate views
        initiateViews();
        // Set Click listeners
        setListeners();
        // Setup scanners
        setupWifi();
    }

    private void setupWifi() {
        // Get wifi manager
        WIFI = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        // Check if wifi is on
        if (!WIFI.isWifiEnabled()) {
            Toast.makeText(getBaseContext(), "Enabling WIFI!", Toast.LENGTH_SHORT).show();
            WIFI.setWifiEnabled(true);
        }
    }

    private void setListeners() {
        // Start scan / Stop scan
        startScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the file name
                if (!scanning) {
                    TURNS = (turnCount.getText().toString().isEmpty()
                            ? 500
                            : Integer.valueOf(turnCount.getText().toString()));
                    scanning = true;
                    scanUntilInterrupted();
                    startScan.setImageResource(R.drawable.icon_stop);
                    Snackbar.make(view, "Scan Started!", Snackbar.LENGTH_SHORT).show();
                } else {
                    scanning = false;
                    startScan.setImageResource(R.drawable.icon_start);
                    wifiTimer.cancel();
                    Snackbar.make(view, "Scan Stopped!", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void scanUntilInterrupted() {

        wifiTimer = new Timer();
        wifiTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                new scanAndWrite().execute();
            }
        }, 0, TURNS);
    }

    private class scanAndWrite extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            String result = processScanner();
            writeToFile(result);
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            // Clear the screen
            scanResults.setText("");
            // Display current scan results
            scanResults.setText(result);
            AlphaAnimation anim = new AlphaAnimation(0.85f, 1.0f);
            int FADE_DURATION = TURNS;
            anim.setDuration(FADE_DURATION);
            scanResults.startAnimation(anim);
        }

        private void writeToFile(String result) {
            File RSSIFolder = new File(Environment.getExternalStorageDirectory().getPath() + "/All_Records");
            if (!RSSIFolder.exists()) {RSSIFolder.mkdirs();}
            File recorder = new File(RSSIFolder.getAbsoluteFile() + "/" + fileName.getText().toString() + ".txt");
            recorder.setWritable(true);
            if (!recorder.exists()) {try {recorder.createNewFile();} catch (IOException e) {/**/}}
            try {
                FileWriter fileWriter = new FileWriter(recorder, true);
                fileWriter.append(result);
                fileWriter.append("\n#-------------------------#\n");
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @NonNull
    private String processScanner() {
        // Start scanning
        WIFI.startScan();
        // Build up scan result set
        StringBuilder builder = new StringBuilder();
        // Append to the builder
        for (ScanResult result : WIFI.getScanResults()) {
            builder.append(result.SSID);
            builder.append(":");
            builder.append(result.level);
            builder.append("(");
            builder.append(DateFormat.format("dd-MM-yyyy hh:mm:ss a", (Calendar.getInstance().getTime())));
            builder.append(")");
        }
        return builder.toString();
    }

    private void initiateViews() {
        // Initiating views
        scanResults = (TextView) findViewById(R.id.rssi_list);
        fileName = (EditText) findViewById(R.id.file_name);
        startScan = (FloatingActionButton) findViewById(R.id.fab);
        turnCount = (EditText) findViewById(R.id.turns);
        // Set icons
        startScan.setImageResource(R.drawable.icon_start);
    }
}
