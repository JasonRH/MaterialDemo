package com.rh.materialdemo.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.rh.materialdemo.MainActivity;
import com.rh.materialdemo.R;
import com.rh.materialdemo.Util.ActivityCollector;
import com.rh.materialdemo.activity.ChatActivity;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;
import de.tavendo.autobahn.WebSocketOptions;

/**
 * @author RH
 */
public class WebSocketService extends Service {
    private static final String TAG = "WebSocketService";
    public static WebSocketConnection webSocketConnection;
    private static String webSocketHost = "ws://10.203.147.113:8080/MyServlet/WebSocketCommunication";
    private static WebSocketOptions options = new WebSocketOptions();
    private BroadcastReceiver connectionReceiver;
    private static boolean isClosed = true;
    public static Messenger mMessenger;
   // private ChatActivity.MyHandler mHandler = new ChatActivity.MyHandler(this);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mMessenger == null) {
            mMessenger = (Messenger) intent.getExtras().get("messenger");
        }

        webSocketConnect();
        MonitorNetworkStatus();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onDestroy() {
        if (webSocketConnection != null && webSocketConnection.isConnected()) {
            webSocketConnection.disconnect();
            webSocketConnection = null;
        }
        if (connectionReceiver != null) {
            unregisterReceiver(connectionReceiver);
        }
        Log.e(TAG, "onDestroy: ");
        super.onDestroy();
    }

    private void webSocketConnect() {
        if (webSocketConnection == null) {
            webSocketConnection = new WebSocketConnection();
        }
        if (!webSocketConnection.isConnected()) {
            try {
                webSocketConnection.connect(webSocketHost, new WebSocketHandler() {
                    //websocket启动时候的回调
                    @Override
                    public void onOpen() {
                        isClosed = false;
                    }

                    //websocket接收到消息后的回调
                    @Override
                    public void onTextMessage(String payload) {

                        if (ActivityCollector.isActivityExist(ChatActivity.class)){
                            Message message = new Message();
                            message.what = 0x0001;
                            message.obj = payload;
                            try {
                                mMessenger.send(message);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }else {
                           // mMessenger = null;
                            getNotificationManager().notify(1,getNotification(payload));
                            Log.e(TAG, "onTextMessage: " + payload);
                        }

                    }

                    //websocket关闭时候的回调
                    @Override
                    public void onClose(int code, String reason) {
                        isClosed = true;
                        Log.e(TAG, "onClose: code:" + code + " reason:" + reason);
                        switch (code) {
                            case 1:
                                Log.e(TAG, "code 1");
                                break;
                            case 2:
                                Log.e(TAG, "code 2，服务器连接不上");
                                break;
                            case 3:
                                Log.e(TAG, "code 3 App关闭");
                                break;
                            case 4:
                                Log.e(TAG, "code 4");
                                break;
                            case 5:
                                Log.e(TAG, "code 5 服务器关闭");
                                break;
                            default:
                        }
                    }
                }, options);
            } catch (WebSocketException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendMsg(String s) {
        if (!TextUtils.isEmpty(s)) {
            if (webSocketConnection != null) {
                webSocketConnection.sendTextMessage(s);
            }
        }
    }

    public static void closeWebsocket() {
        if (webSocketConnection != null && webSocketConnection.isConnected()) {
            webSocketConnection.disconnect();
            webSocketConnection = null;
        }
    }

    private void MonitorNetworkStatus() {
        if (connectionReceiver == null) {
            connectionReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

                    if (networkInfo == null || !networkInfo.isAvailable()) {
                        Toast.makeText(getApplicationContext(), "网络已断开，请重新连接", Toast.LENGTH_SHORT).show();
                    } else {
                        if (webSocketConnection != null) {
                            webSocketConnection.disconnect();
                        }
                        if (isClosed) {
                            webSocketConnect();
                        }
                    }

                }
            };

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(connectionReceiver, intentFilter);
        }

    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }
    private Notification getNotification(String contentText) {
        Intent intent = new Intent(this, ChatActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        Notification builder = new NotificationCompat.Builder(this)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setSmallIcon(R.mipmap.message)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.message))
                .setFullScreenIntent(pi, false)
                .setContentTitle("新消息提醒")
                .setContentText(contentText)
                .build();
        return builder;
    }

}
