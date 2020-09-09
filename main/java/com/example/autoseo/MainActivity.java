package com.example.autoseo;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.IDN;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AutoAirplaneModeService";
    private static final String COMMAND_FLIGHT_MODE_1 = "settings put global airplane_mode_on ";
    private static final String COMMAND_FLIGHT_MODE_2 = "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state ";
    private String type = "google";
    private int page = 1;
    private int min_delay = 10;
    private int max_delay = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText keywordEdit = (EditText)findViewById(R.id.keyword);
        final EditText minDelayEdit = (EditText)findViewById(R.id.delay_min);
        final EditText maxDelayEdit = (EditText)findViewById(R.id.delay_max);
        final EditText urlEdit = (EditText)findViewById(R.id.url);
        final RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        final RadioButton r_google = (RadioButton) findViewById(R.id.r_google);
        final RadioButton r_naver = (RadioButton) findViewById(R.id.r_naver);
        int running = 0;
        boolean isGoogle = true;


        SQLiteDatabase db = this.openOrCreateDatabase("config.db", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS config (id INTEGER, keyword VARCHAR(50), min_delay INTEGER, max_delay INTEGER, url TEXT, type VARCHAR(20), running INTEGER );");
        Cursor cursor = db.rawQuery("SELECT * FROM config", null);
        if (cursor.moveToFirst()) {
            String _mindelay = cursor.getString(cursor.getColumnIndex("min_delay"));
            String _maxdelay = cursor.getString(cursor.getColumnIndex("max_delay"));
            min_delay = Integer.parseInt(_mindelay);
            max_delay = Integer.parseInt(_maxdelay);

            keywordEdit.setText(cursor.getString(cursor.getColumnIndex("keyword")));
            minDelayEdit.setText(_mindelay);
            maxDelayEdit.setText(_maxdelay);
            // 퓨니코드 변환
            String _url = cursor.getString(cursor.getColumnIndex("url"));
            String korean = ".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*";
            if (_url.matches(korean)) {
                String punyUrl = IDN.toASCII(_url);
                _url = punyUrl;
            }
            urlEdit.setText(_url);
            type = cursor.getString(cursor.getColumnIndex("type"));
            running = cursor.getInt(cursor.getColumnIndex("running"));

            if (type.equalsIgnoreCase("google")) {
                radioGroup.check(R.id.r_google);
            } else {
                radioGroup.check(R.id.r_naver);
            }
        } else {
            db.execSQL("INSERT INTO config (id, keyword, min_delay, max_delay, url, type, running) VALUES (0, '인싸포커', 10, 10, 'inssaplay.com', 'google', 0)");
        }
        db.close();

        RadioGroup.OnCheckedChangeListener radioGroupButtonChangeListener = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                if(i == R.id.r_google) {
                    type = "google";
                    SQLiteDatabase db = openOrCreateDatabase("config.db", MODE_PRIVATE, null);
                    db.execSQL("UPDATE config SET type = 'google' WHERE id = 0;");
                    db.close();
                } else {
                    type = "naver";
                    SQLiteDatabase db = openOrCreateDatabase("config.db", MODE_PRIVATE, null);
                    db.execSQL("UPDATE config SET type = 'naver' WHERE id = 0;");
                    db.close();
                }
            }
        };
        radioGroup.setOnCheckedChangeListener(radioGroupButtonChangeListener);
        keywordEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String _keyword = s.toString();
                SQLiteDatabase db = openOrCreateDatabase("config.db", MODE_PRIVATE, null);
                db.execSQL("UPDATE config SET keyword = '"+_keyword+"' WHERE id = 0;");
                db.close();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        minDelayEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String _delay = s.toString();
                SQLiteDatabase db = openOrCreateDatabase("config.db", MODE_PRIVATE, null);
                db.execSQL("UPDATE config SET min_delay = '"+_delay+"' WHERE id = 0;");
                db.close();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        maxDelayEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String _delay = s.toString();
                SQLiteDatabase db = openOrCreateDatabase("config.db", MODE_PRIVATE, null);
                db.execSQL("UPDATE config SET max_delay = '"+_delay+"' WHERE id = 0;");
                db.close();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        urlEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String _url = s.toString();
                SQLiteDatabase db = openOrCreateDatabase("config.db", MODE_PRIVATE, null);
                db.execSQL("UPDATE config SET url = '"+_url+"' WHERE id = 0;");
                db.close();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        // 웹뷰 시작
        final WebView mWebView = (WebView)findViewById(R.id.webView);

        WebSettings mWebSettings = mWebView.getSettings(); //세부 세팅 등록
        mWebSettings.setJavaScriptEnabled(true); // 웹페이지 자바스클비트 허용 여부
        mWebSettings.setSupportMultipleWindows(true); // 새창 띄우기 허용 여부
        mWebSettings.setJavaScriptCanOpenWindowsAutomatically(true); // 자바스크립트 새창 띄우기(멀티뷰) 허용 여부
        mWebSettings.setLoadWithOverviewMode(true); // 메타태그 허용 여부
        mWebSettings.setUseWideViewPort(true); // 화면 사이즈 맞추기 허용 여부
        mWebSettings.setSupportZoom(false); // 화면 줌 허용 여부
        mWebSettings.setBuiltInZoomControls(false); // 화면 확대 축소 허용 여부
        mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL); // 컨텐츠 사이즈 맞추기
        mWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE); // 브라우저 캐시 허용 여부
        mWebSettings.setDomStorageEnabled(true); // 로컬저장소 허용 여부
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public Bitmap getDefaultVideoPoster(){
                return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
            }
        });

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, final WebResourceRequest request, WebResourceError error) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        String host = request.getUrl().getHost();
                        if (host.contains(type))
                            try {
                                toggleAirplaneMode(false);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            triggerRebirth(getApplicationContext());
                    }
                }, 3000);
            }

            @Override
            public void onPageFinished(final WebView view, String url) {
                TextView ip = (TextView)findViewById(R.id.ip);
                ip.setText(Utils.getIPAddress(true));
                String googleSrc = "localStorage.clear();" +
                        "var s = document.getElementsByName('q');" +
                        "setTimeout(function() {s[0].value = '"+keywordEdit.getText().toString()+"';}, 1000);" +
                        "setTimeout(function() {s[0].dispatchEvent(new Event('input'));}, 2000);" +
                        "var f = document.querySelector('form[role=search]');" +
                        "setTimeout(function() {f.submit();}, 3000);";
                String naverSrc = "localStorage.clear();" +
                        "var s = document.querySelector('input[type=search]');" +
                        "setTimeout(function() {s.value = '"+keywordEdit.getText().toString()+"';}, 1000);" +
                        "setTimeout(function() {s.dispatchEvent(new Event('input'));}, 2000);" +
                        "var f = document.querySelector('form[role=search]');" +
                        "setTimeout(function() {f.submit();}, 3000);";
                String endUrl = type + ".com/"; // "google.com/"
                if (url.endsWith(endUrl)) {
                    if (type.equalsIgnoreCase("google")) {
                        view.evaluateJavascript(googleSrc, null);
                    } else {
                        view.evaluateJavascript(naverSrc, null);
                    }
                } else if (url.contains("search")) {
                    if (type.equalsIgnoreCase("google")) {
                        view.evaluateJavascript("function recursive(i) { if (i < 10) { setTimeout(function() { var a = document.querySelector('a[href*=\""+urlEdit.getText().toString()+"\"]'); if (!a) { document.querySelector('a[aria-label=\"결과 더보기\"]').click(); } else { a.click(); } i++; recursive(i); }, i == 0 ? 0 : 3000)} } recursive(0);", null);
                    } else {
                        view.evaluateJavascript("function recursive(i) { if (i < 10) { setTimeout(function() { var a = document.querySelector('a[href*=\""+urlEdit.getText().toString()+"\"]'); if (!a) { document.querySelector('a[href*=\"page="+page+"\"]').click(); } else { a.click(); } i++; recursive(i); }, 3000)} } recursive(0);", null);
                        page++;
                    }
                } else if (url.contains(urlEdit.getText().toString())) {
                        int delay = getDelay();
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                try {
                                    toggleAirplaneMode(true);
                                    //toggleAirplaneMode(false);

                                    clearCookies(getApplicationContext());
                                    WebStorage.getInstance().deleteAllData();
                                    view.clearHistory();
                                    view.clearCache(true);
                                    triggerRebirth(getApplicationContext());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                /*view.clearHistory();
                                view.clearCache(true);
                                triggerRebirth(getApplicationContext());*/
                            }
                        }, delay * 1000);
                }

            }
        });

        if (running == 1) {
            mWebView.loadUrl("https://www."+type+".com/");
        }

        Button startButton = (Button)findViewById(R.id.start);
        startButton.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        String _url = urlEdit.getText().toString();
                        String korean = ".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*";
                        if (_url.matches(korean)) {
                            String punyUrl = IDN.toASCII(_url);
                            _url = punyUrl;
                        }
                        urlEdit.setText(_url);

                        SQLiteDatabase db = openOrCreateDatabase("config.db", MODE_PRIVATE, null);
                        db.execSQL("UPDATE config SET running = 1 WHERE id = 0;");
                        db.close();
                        mWebView.loadUrl("https://www."+type+".com/");
                    }
                }
        );
        Button endButton = (Button)findViewById(R.id.end);
        endButton.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        SQLiteDatabase db = openOrCreateDatabase("config.db", MODE_PRIVATE, null);
                        db.execSQL("UPDATE config SET running = 0 WHERE id = 0;");
                        db.close();
                        mWebView.loadUrl("about:blank");
                    }
                }
        );
    }

    private int getDelay() {
        if (min_delay == max_delay) {
            return min_delay;
        } else {
            Random r = new Random();
            return r.nextInt((max_delay - min_delay) + 1) + min_delay;
        }
    }

    @SuppressWarnings("deprecation")
    public static void clearCookies(Context context)
    {
            CookieSyncManager cookieSyncMgr = CookieSyncManager.createInstance(context);
            cookieSyncMgr.startSync();
            CookieManager cookieManager=CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMgr.stopSync();
            cookieSyncMgr.sync();
    }
    public static void triggerRebirth(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }
    public boolean checkInternetConnection() {
        Context context = getApplicationContext();
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }
    private void executeCommandWithoutWait(String command) throws IOException {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
    }
    private boolean toggleAirplaneMode(boolean enable) throws IOException {
        String v = enable ? "1" : "0";
        String command = COMMAND_FLIGHT_MODE_1 + v;
        try {
            executeCommandWithoutWait(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String command2 = COMMAND_FLIGHT_MODE_2 + enable;
        executeCommandWithoutWait(command2);
        Settings.Global.putInt(getApplicationContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, enable ? 1 : 0);
        return enable;
    }

}