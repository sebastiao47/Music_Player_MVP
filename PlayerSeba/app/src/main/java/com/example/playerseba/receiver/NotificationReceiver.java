package com.example.playerseba.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.playerseba.Servico.MusicService;

public class NotificationReceiver extends BroadcastReceiver {
    public static final String ACTION_PLAY_PAUSE = "com.example.playerseba.ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.example.playerseba.ACTION_NEXT";
    public static final String ACTION_PREV = "com.example.playerseba.ACTION_PREV";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, MusicService.class);
        if (intent.getAction() != null) {
            serviceIntent.setAction(intent.getAction());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }

    public static PendingIntent getPendingIntent(Context context, String action) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(action);

        int requestCode;
        switch (action) {
            case ACTION_PLAY_PAUSE:
                requestCode = 1;
                break;
            case ACTION_NEXT:
                requestCode = 2;
                break;
            case ACTION_PREV:
                requestCode = 3;
                break;
            default:
                requestCode = 0;
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }
}
