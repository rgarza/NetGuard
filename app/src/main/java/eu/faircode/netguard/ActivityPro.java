package eu.faircode.netguard;

/*
    This file is part of NetGuard.

    NetGuard is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    NetGuard is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with NetGuard.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2015-2016 by Marcel Bokhorst (M66B)
*/

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActivityPro extends AppCompatActivity {
    private static final String TAG = "NetGuard.Pro";

    private IAB iab;

    // adb shell pm clear com.android.vending
    // android.test.purchased
    public static final String SKU_LOG = "log";
    public static final String SKU_NOTIFY = "notify";
    public static final String SKU_SPEED = "speed";
    public static final String SKU_THEME = "theme";
    public static final String SKU_MULTI = "multi";
    public static final String SKU_DONATION = "donation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Create");
        Util.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pro);

        getSupportActionBar().setTitle(R.string.title_pro);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // Initial state
        updateState();

        TextView tvLogTitle = (TextView) findViewById(R.id.tvLogTitle);
        TextView tvNotifyTitle = (TextView) findViewById(R.id.tvNotifyTitle);
        TextView tvSpeedTitle = (TextView) findViewById(R.id.tvSpeedTitle);
        TextView tvThemeTitle = (TextView) findViewById(R.id.tvThemeTitle);
        TextView tvMultiTitle = (TextView) findViewById(R.id.tvMultiTitle);

        Linkify.TransformFilter filter = new Linkify.TransformFilter() {
            @Override
            public String transformUrl(Matcher match, String url) {
                return "";
            }
        };

        Linkify.addLinks(tvLogTitle, Pattern.compile(".*"), "http://www.netguard.me/#log", null, filter);
        Linkify.addLinks(tvNotifyTitle, Pattern.compile(".*"), "http://www.netguard.me/#notify", null, filter);
        Linkify.addLinks(tvSpeedTitle, Pattern.compile(".*"), "http://www.netguard.me/#speed", null, filter);
        Linkify.addLinks(tvThemeTitle, Pattern.compile(".*"), "http://www.netguard.me/#theme", null, filter);
        Linkify.addLinks(tvMultiTitle, Pattern.compile(".*"), "http://www.netguard.me/#multi", null, filter);

        // Challenge
        TextView tvChallenge = (TextView) findViewById(R.id.tvChallenge);
        tvChallenge.setText(Build.SERIAL);

        // Response
        try {
            final String response = Util.md5(Build.SERIAL, "NetGuard");
            EditText etResponse = (EditText) findViewById(R.id.etResponse);
            etResponse.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Do nothing
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Do nothing
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (response.equals(editable.toString().toUpperCase())) {
                        IAB.setBought(SKU_DONATION, ActivityPro.this);
                        updateState();
                    }
                }
            });
        } catch (Throwable ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }

        try {
            iab = new IAB(new IAB.Delegate() {
                @Override
                public void onReady(final IAB iab) {
                    Log.i(TAG, "IAB ready");
                    try {
                        iab.updatePurchases();
                        updateState();

                        final Button btnLog = (Button) findViewById(R.id.btnLog);
                        final Button btnNotify = (Button) findViewById(R.id.btnNotify);
                        final Button btnSpeed = (Button) findViewById(R.id.btnSpeed);
                        final Button btnTheme = (Button) findViewById(R.id.btnTheme);
                        final Button btnMulti = (Button) findViewById(R.id.btnMulti);

                        View.OnClickListener listener = new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                try {
                                    if (view == btnLog)
                                        startIntentSenderForResult(iab.getBuyIntent(SKU_LOG).getIntentSender(), view.getId(), new Intent(), 0, 0, 0);
                                    else if (view == btnNotify)
                                        startIntentSenderForResult(iab.getBuyIntent(SKU_NOTIFY).getIntentSender(), view.getId(), new Intent(), 0, 0, 0);
                                    else if (view == btnSpeed)
                                        startIntentSenderForResult(iab.getBuyIntent(SKU_SPEED).getIntentSender(), view.getId(), new Intent(), 0, 0, 0);
                                    else if (view == btnTheme)
                                        startIntentSenderForResult(iab.getBuyIntent(SKU_THEME).getIntentSender(), view.getId(), new Intent(), 0, 0, 0);
                                    else if (view == btnMulti)
                                        startIntentSenderForResult(iab.getBuyIntent(SKU_MULTI).getIntentSender(), view.getId(), new Intent(), 0, 0, 0);
                                } catch (Throwable ex) {
                                    Log.i(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                                    Util.sendCrashReport(ex, ActivityPro.this);
                                }
                            }
                        };

                        btnLog.setOnClickListener(listener);
                        btnNotify.setOnClickListener(listener);
                        btnSpeed.setOnClickListener(listener);
                        btnTheme.setOnClickListener(listener);
                        btnMulti.setOnClickListener(listener);

                        btnLog.setEnabled(true);
                        btnNotify.setEnabled(true);
                        btnSpeed.setEnabled(true);
                        btnTheme.setEnabled(true);
                        btnMulti.setEnabled(true);

                    } catch (Throwable ex) {
                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                    }
                }
            }, this);
            iab.bind();
        } catch (Throwable ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "Destroy");
        iab.unbind();
        super.onDestroy();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case R.id.btnLog:
                    IAB.setBought(SKU_LOG, this);
                    updateState();
                    break;
                case R.id.btnNotify:
                    IAB.setBought(SKU_NOTIFY, this);
                    updateState();
                    break;
                case R.id.btnSpeed:
                    IAB.setBought(SKU_SPEED, this);
                    updateState();
                    break;
                case R.id.btnTheme:
                    IAB.setBought(SKU_THEME, this);
                    updateState();
                    break;
                case R.id.btnMulti:
                    IAB.setBought(SKU_MULTI, this);
                    updateState();
                    break;
            }
        }
    }

    private void updateState() {
        Button btnLog = (Button) findViewById(R.id.btnLog);
        Button btnNotify = (Button) findViewById(R.id.btnNotify);
        Button btnSpeed = (Button) findViewById(R.id.btnSpeed);
        Button btnTheme = (Button) findViewById(R.id.btnTheme);
        Button btnMulti = (Button) findViewById(R.id.btnMulti);
        TextView tvLog = (TextView) findViewById(R.id.tvLog);
        TextView tvNotify = (TextView) findViewById(R.id.tvNotify);
        TextView tvSpeed = (TextView) findViewById(R.id.tvSpeed);
        TextView tvTheme = (TextView) findViewById(R.id.tvTheme);
        TextView tvMulti = (TextView) findViewById(R.id.tvMulti);
        LinearLayout llChallenge = (LinearLayout) findViewById(R.id.llChallenge);

        btnLog.setVisibility(IAB.isPurchased(SKU_LOG, this) ? View.GONE : View.VISIBLE);
        btnNotify.setVisibility(IAB.isPurchased(SKU_NOTIFY, this) ? View.GONE : View.VISIBLE);
        btnSpeed.setVisibility(IAB.isPurchased(SKU_SPEED, this) ? View.GONE : View.VISIBLE);
        btnTheme.setVisibility(IAB.isPurchased(SKU_THEME, this) ? View.GONE : View.VISIBLE);
        btnMulti.setVisibility(IAB.isPurchased(SKU_MULTI, this) ? View.GONE : View.VISIBLE);

        tvLog.setVisibility(IAB.isPurchased(SKU_LOG, this) ? View.VISIBLE : View.GONE);
        tvNotify.setVisibility(IAB.isPurchased(SKU_NOTIFY, this) ? View.VISIBLE : View.GONE);
        tvSpeed.setVisibility(IAB.isPurchased(SKU_SPEED, this) ? View.VISIBLE : View.GONE);
        tvTheme.setVisibility(IAB.isPurchased(SKU_THEME, this) ? View.VISIBLE : View.GONE);
        tvMulti.setVisibility(IAB.isPurchased(SKU_MULTI, this) ? View.VISIBLE : View.GONE);

        llChallenge.setVisibility(IAB.isPurchased(SKU_DONATION, this) ? View.GONE : View.VISIBLE);
    }
}
