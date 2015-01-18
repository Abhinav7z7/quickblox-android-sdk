package com.quickblox.sample.chat.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.quickblox.core.QBEntityCallbackImpl;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialog;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.quickblox.sample.chat.ApplicationSingleton;
import com.quickblox.sample.chat.R;
import com.quickblox.sample.chat.core.ChatManager;
import com.quickblox.sample.chat.core.GroupChatManagerImpl;
import com.quickblox.sample.chat.core.PrivateChatManagerImpl;
import com.quickblox.sample.chat.ui.adapters.ChatAdapter;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ChatActivity extends Activity {

    private static final String TAG = ChatActivity.class.getSimpleName();

    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_DIALOG = "dialog";
    private final String PROPERTY_SAVE_TO_HISTORY = "save_to_history";

    private EditText messageEditText;
    private ListView messagesContainer;
    private Button sendButton;
    private ProgressBar progressBar;

    private Mode mode = Mode.PRIVATE;
    private ChatManager chat;
    private ChatAdapter adapter;
    private QBDialog dialog;

    private ArrayList<QBChatMessage> history;

    private MyBroadCastReceiver myBroadCastReceiver = new MyBroadCastReceiver();

    public static void start(Context context, Bundle bundle) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Log.d("FIX", "turn on screen");
        Window wind = this.getWindow();
        wind.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        wind.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        wind.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        initViews();
   }

    @Override
    protected void onResume() {
        registerReceiver(myBroadCastReceiver,new IntentFilter());
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (myBroadCastReceiver != null)
        unregisterReceiver(myBroadCastReceiver);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        try {
            chat.release();
        } catch (XMPPException e) {
            Log.e(TAG, "failed to release chat", e);
        }
        super.onBackPressed();
    }

    private void initViews() {
        messagesContainer = (ListView) findViewById(R.id.messagesContainer);
        messageEditText = (EditText) findViewById(R.id.messageEdit);
        sendButton = (Button) findViewById(R.id.chatSendButton);

        TextView meLabel = (TextView) findViewById(R.id.meLabel);
        TextView companionLabel = (TextView) findViewById(R.id.companionLabel);
        RelativeLayout container = (RelativeLayout) findViewById(R.id.container);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);




        Intent intent = getIntent();

        // Get chat dialog
        //
        dialog = (QBDialog)intent.getSerializableExtra(EXTRA_DIALOG);

        mode = (Mode) intent.getSerializableExtra(EXTRA_MODE);
        switch (mode) {
            case GROUP:
                chat = new GroupChatManagerImpl(this);
                container.removeView(meLabel);
                container.removeView(companionLabel);


                // Join group chat
                //
                progressBar.setVisibility(View.VISIBLE);
                //
                ((GroupChatManagerImpl) chat).joinGroupChat(dialog, new QBEntityCallbackImpl() {
                    @Override
                    public void onSuccess() {

                        // Load Chat history
                        //
                        loadChatHistory();
                    }

                    @Override
                    public void onError(List list) {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(ChatActivity.this);
                        dialog.setMessage("error when join group chat: " + list.toString()).create().show();
                    }
                });

                break;
            case PRIVATE:
                Integer opponentID = ((ApplicationSingleton)getApplication()).getOpponentIDForPrivateDialog(dialog);
                chat = new PrivateChatManagerImpl(ChatActivity.this, opponentID);
                companionLabel.setText(((ApplicationSingleton)getApplication()).getDialogsUsers().get(opponentID).getLogin());

                // Load CHat history
                //
                loadChatHistory();
                break;
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageEditText.getText().toString();
                if (TextUtils.isEmpty(messageText)) {
                    return;
                }

                // Send chat message
                //
                QBChatMessage chatMessage = new QBChatMessage();
                chatMessage.setBody(messageText);
                chatMessage.setProperty(PROPERTY_SAVE_TO_HISTORY, "1");

                try {
                    chat.sendMessage(chatMessage);
                } catch (XMPPException e) {
                    Log.e(TAG, "failed to send a message", e);
                } catch (SmackException sme){
                    Log.e(TAG, "failed to send a message", sme);
                }

                messageEditText.setText("");

                if(mode == Mode.PRIVATE) {
                    showMessage(chatMessage);
                }
            }
        });
    }

    private void loadChatHistory(){
        QBRequestGetBuilder customObjectRequestBuilder = new QBRequestGetBuilder();
        customObjectRequestBuilder.setPagesLimit(100);

        QBChatService.getDialogMessages(dialog, customObjectRequestBuilder, new QBEntityCallbackImpl<ArrayList<QBChatMessage>>() {
            @Override
            public void onSuccess(ArrayList<QBChatMessage> messages, Bundle args) {
                history = messages;

                adapter = new ChatAdapter(ChatActivity.this, new ArrayList<QBChatMessage>());
                messagesContainer.setAdapter(adapter);

                for(QBChatMessage msg : messages) {
                    showMessage(msg);
                }

                progressBar.setVisibility(View.GONE);

                new ChatTestThread(ChatActivity.this).start();
            }

            @Override
            public void onError(List<String> errors) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(ChatActivity.this);
                dialog.setMessage("load chat history errors: " + errors).create().show();
            }
        });
    }

    public void showMessage(QBChatMessage message) {
        adapter.add(message);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
                scrollDown();
            }
        });
    }

    private void scrollDown() {
        messagesContainer.setSelection(messagesContainer.getCount() - 1);
    }

    public static enum Mode {PRIVATE, GROUP}


    class ChatTestThread extends Thread {


        private final Activity activity;
        public WifiManager wifiManager;
        private int messageNum = 0;

        ChatTestThread(Activity activity) {
            this.activity = activity;
            this.wifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        }

        @Override
        public void run() {
            super.run();
            while (true) {

                turnOnWifi();

                int sizeBefore = adapter.getCount();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        messageEditText.setText("Test message " + messageNum);
                        Log.d("FIX", "add message");
                        messageNum ++;
                        sendButton.performClick();
                        Log.d("FIX", "send message");
                    }
                });

                waitForMessagePosting(sizeBefore);


//                activity.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
                        turnOffScreen();
                        Log.d("FIX", "turn off screen");
//                    }
//                });

                switchWifiState();

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startService(new Intent(activity, MyTurnOnService.class));
                    }
                });

            }
        }



        private void turnOffScreen() {

            DevicePolicyManager mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
            mDPM.lockNow();
            startService(new Intent(ChatActivity.this, MyTurnOnService.class));
//            KeyguardManager.KeyguardLock key;
//            KeyguardManager km = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
//            key = KeyguardManager.KeyguardLock("IN");
//            key.disableKeyguard();

        }

        private void waitForMessagePosting(int sizeBefore) {
            while (sizeBefore >= adapter.getCount()) {
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d("FIX", "message posted");
        }

        private void turnOnWifi() {

            if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED){
                switchWifiState();
                while (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED){
                    try {
                        Thread.currentThread().sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void switchWifiState() {
            while (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING
                    || wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING
                    || wifiManager.getWifiState() == WifiManager.WIFI_STATE_UNKNOWN) {
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                wifiManager.setWifiEnabled(false);
                Log.d("FIX", "turn off wi-fi");
                while (wifiManager.getWifiState() != WifiManager.WIFI_STATE_DISABLED){
                    try {
                        Thread.currentThread().sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                wifiManager.setWifiEnabled(true);
                Log.d("FIX", "turn on wi-fi");
            }
        }
    }
}
