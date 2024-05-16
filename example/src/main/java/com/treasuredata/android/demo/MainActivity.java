package com.treasuredata.android.demo;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import com.treasuredata.android.TDCallback;
import com.treasuredata.android.TreasureData;
import com.treasuredata.android.cdp.FetchUserSegmentsCallback;
import com.treasuredata.android.cdp.Profile;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private String eventTable = "event_table";
    private String eventDatabase = "event_db";
    private String recordUUIDColumn;
    private String aaidColumn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // For default callback, optional.
        TreasureData.sharedInstance().setAddEventCallBack(addEventCallback);

        EditText eventTableTextView = findViewById(R.id.eventTableTextView);
        eventTableTextView.setText(eventTable);
        eventTableTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                eventTable = s.toString();
            }
        });

        EditText eventDatabaseTextView = findViewById(R.id.eventDatabaseTextView);
        eventDatabaseTextView.setText(eventDatabase);
        eventDatabaseTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                eventDatabase = s.toString();
            }
        });

        findViewById(R.id.addEventButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addEventCallback.eventName = "add";
                Map<String, Object> event = new HashMap<String, Object>();
                event.put("key", "value");
                // Use default callback
                TreasureData.sharedInstance().addEvent(eventDatabase, eventTable, event);
            }
        });

        findViewById(R.id.uploadEventButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().uploadEventsWithCallback(uploadEventsCallback);
            }
        });

        findViewById(R.id.getUUIDButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, TreasureData.sharedInstance().getUUID(), Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.enableUUIDButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().enableAutoAppendUniqId();
            }
        });

        findViewById(R.id.disableUUIDButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().disableAutoAppendUniqId();
            }
        });

        findViewById(R.id.resetUUIDButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().resetUniqId();
            }
        });

        findViewById(R.id.enableModelInfoButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().enableAutoAppendModelInformation();
            }
        });

        findViewById(R.id.disableModelInfoButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().disableAutoAppendModelInformation();
            }
        });

        findViewById(R.id.enableAppInfoButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().enableAutoAppendAppInformation();
            }
        });

        findViewById(R.id.disableAppInfoButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().disableAutoAppendAppInformation();
            }
        });

        findViewById(R.id.enableLocalInfoButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().enableAutoAppendLocaleInformation();
            }
        });

        findViewById(R.id.disableLocalInfoButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().disableAutoAppendLocaleInformation();
            }
        });

        final EditText recordUUIDColumnTextView = findViewById(R.id.recordUUIDColumn);
        recordUUIDColumnTextView.setText(recordUUIDColumn);
        recordUUIDColumnTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                recordUUIDColumn = s.toString();
            }
        });
        findViewById(R.id.enableRecordUUIDButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recordUUIDColumn == null) {
                    TreasureData.sharedInstance().enableAutoAppendRecordUUID();
                } else {
                    TreasureData.sharedInstance().enableAutoAppendRecordUUID(recordUUIDColumn);
                }
            }
        });
        findViewById(R.id.disableRecordUUIDButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().disableAutoAppendRecordUUID();
            }
        });

        final EditText aaidTextView = findViewById(R.id.aaidColumn);
        aaidTextView.setText(aaidColumn);
        aaidTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                aaidColumn = s.toString();
            }
        });
        findViewById(R.id.enableAutoAppendAdvertisingIdButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (aaidColumn == null) {
                    TreasureData.sharedInstance().enableAutoAppendAdvertisingIdentifier();
                } else {
                    TreasureData.sharedInstance().enableAutoAppendAdvertisingIdentifier(aaidColumn);
                }
            }
        });
        findViewById(R.id.disableAutoAppendAdvertisingIdButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().disableAutoAppendAdvertisingIdentifier();
            }
        });

        final String sessionTable = "";
        final String sessionDatabase = "";
        findViewById(R.id.getSessionIdButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String sessionId = TreasureData.sharedInstance().getSessionId();
                Toast.makeText(MainActivity.this, sessionId, Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.startSessionButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sessionDatabase == "") {
                    TreasureData.sharedInstance().startSession(sessionTable);
                } else {
                    TreasureData.sharedInstance().startSession(sessionDatabase, sessionTable);
                }
            }
        });
        findViewById(R.id.endSessionButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String sessionId = TreasureData.sharedInstance().getSessionId();
                Toast.makeText(MainActivity.this, sessionId, Toast.LENGTH_SHORT).show();
                if (sessionDatabase == "") {
                    TreasureData.sharedInstance().endSession(sessionTable);
                } else {
                    TreasureData.sharedInstance().endSession(sessionDatabase, sessionTable);
                }
            }
        });
        findViewById(R.id.getGlobalSessionId).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String sessionId = TreasureData.sharedInstance().getSessionId();
                Toast.makeText(MainActivity.this, sessionId, Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.startGlobalSession).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.startSession(getApplicationContext());
            }
        });
        findViewById(R.id.endGlobalSession).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.endSession(getApplicationContext());
            }
        });
        findViewById(R.id.setTimeOutMilliButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.setSessionTimeoutMilli(20000);
            }
        });

        findViewById(R.id.enableCustomEventButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().enableCustomEvent();
            }
        });

        findViewById(R.id.disableCustomEventButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().disableCustomEvent();
            }
        });

        findViewById(R.id.isCustomEventEnabledButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean isCustomEventEnabled = TreasureData.sharedInstance().isCustomEventEnabled();
                Toast.makeText(MainActivity.this, isCustomEventEnabled ? "Custom event enabled" : "Custom event not enabled", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.enableAppLifecycleEventButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().enableAppLifecycleEvent();
            }
        });

        findViewById(R.id.disableAppLifecycleEventButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().disableAppLifecycleEvent();
            }
        });

        findViewById(R.id.isAppLifecycleEventEnabledButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean isAppLifecycleEventEnabled = TreasureData.sharedInstance().isAppLifecycleEventEnabled();
                Toast.makeText(MainActivity.this, isAppLifecycleEventEnabled ? "App Lifecycle event enabled" : "App Lifecycle event not enabled", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.fetchUserSegmentsButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().fetchUserSegments(Arrays.asList("<your_profile_api_tokens>"),
                        Collections.singletonMap("<your_key_column>", "<value>"),
                        new FetchUserSegmentsCallback() {
                            @Override
                            public void onSuccess(List<Profile> profiles) {
                                System.out.println(profiles);
                            }
                            @Override
                            public void onError(Exception e) {
                                System.err.println(e);
                            }
                        });
            }
        });


        findViewById(R.id.enableRetryUploadingButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().enableAutoRetryUploading();
            }
        });

        findViewById(R.id.disableRetryUploadingButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().disableAutoRetryUploading();
            }
        });

        findViewById(R.id.enableEventCompressionButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.enableEventCompression();
            }
        });

        findViewById(R.id.disableEventCompressionButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.disableEventCompression();
            }
        });

        findViewById(R.id.enableLoggingButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.enableLogging();
            }
        });

        findViewById(R.id.disableLoggingButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.disableLogging();
            }
        });

        findViewById(R.id.isFirstRunButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean isFirstRun = TreasureData.sharedInstance().isFirstRun(getApplicationContext());
                Toast.makeText(MainActivity.this, isFirstRun ? "Is first run" : "Is not first run", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.clearFirstRunButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TreasureData.sharedInstance().clearFirstRun(getApplicationContext());
            }
        });
    }

    public void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        TreasureData.startSession(this);
        Log.i(TAG, "onStart(): Session ID=" + TreasureData.getSessionId(this));
    }

    @Override
    protected void onStop() {
        super.onStop();
        TreasureData.endSession(this);
        Log.i(TAG, "onStop(): Session ID=" + TreasureData.getSessionId(this));
        TreasureData.sharedInstance().uploadEvents();
    }

    /**
     * These are only for callback, Optional.
     */
    class AddEventCallback implements TDCallback {
        String eventName;

        @Override
        public void onSuccess() {
            String message = "TreasureData.addEvent:onSuccess[" + eventName + "]";
            Log.d(TAG, message);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(String errorCode, Exception e) {
            String message = "TreasureData.addEvent:onError[" + eventName + ": errorCode=" + errorCode + ", ex=" + e + "]";
            Log.d(TAG, message);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    }

    class UploadEventsCallback implements TDCallback {
        @Override
        public void onSuccess() {
            final String message = "TreasureData.uploadEvents:onSuccess";
            Log.d(TAG, message);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onError(String errorCode, Exception e) {
            final String message = "TreasureData.uploadEvents:onError[" + errorCode + ": errorCode=" + errorCode + ", ex=" + e + "]";
            Log.d(TAG, message);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private AddEventCallback addEventCallback = new AddEventCallback();

    private UploadEventsCallback uploadEventsCallback = new UploadEventsCallback();

    @Override
    protected void onDestroy() {
        TreasureData.sharedInstance().uploadEvents();
        super.onDestroy();
    }
}
