package org.kuunika.kugawaserver;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("node");
    }

    public static boolean _startedNodeAlready = false;
    private static final String TAG = MainActivity.class.getSimpleName();

    private FloatingActionButton fab;
    private TextView txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(this);

        txtStatus = findViewById(R.id.txt_status);

        Timer timer = new Timer();

        TimerTask delayedThreadStartTask = new TimerTask() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startNodeServer();
                    }
                }).start();
            }
        };

        timer.schedule(delayedThreadStartTask, 4000); //4 second delay
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                break;

            case R.id.action_stop_node:
                stopNodeServer();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void stopNodeServer() {
        Log.i(TAG, "stopNodeServer()");
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "onClick()");
        new StartNodeServer(this).execute();
    }

    private void startNodeServer() {
        Log.i(TAG, "startNodeServer()");
        if (!_startedNodeAlready) {
            _startedNodeAlready = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "startNodeServer() thread run()");
                    Integer nodeStatus = startNodeWithArguments(new String[]{"node", "-e",
                            "var http = require('http'); " +
                                    "var versions_server = http.createServer( (request, response) => { " +
                                    "  response.end('Versions: ' + JSON.stringify(process.versions)); " +
                                    "}); " +
                                    "versions_server.listen(3000);"
                    });

                    Log.i(TAG, "startNodeServer() status code -> " + nodeStatus);
                }
            }).start();
        }
    }

    public native Integer startNodeWithArguments(String[] arguments);

    private class StartNodeServer extends AsyncTask<Void, Void, String> {

        private MainActivity mActivity;

        public StartNodeServer(MainActivity activity) {
            mActivity = activity;//assign context
        }

        @Override
        protected void onPostExecute(String result) {
            mActivity.txtStatus.setText(result);//set text to status text view
        }

        @Override
        protected String doInBackground(Void... voids) {
            String nodeResponse = "";
            try {
                URL localNodeServer = new URL("http://localhost:3000/");
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(localNodeServer.openStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                    nodeResponse = nodeResponse + inputLine;
                in.close();
            } catch (Exception ex) {
                nodeResponse = ex.toString();
            }
            return nodeResponse;
        }
    }
}
