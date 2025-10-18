package com.example.playerseba;

import android.app.Application;

// A classe App não é mais responsável por criar o canal de notificação.
// O MusicService agora gerencia seu próprio canal para garantir que ele sempre exista.
public class App extends Application {
    public static final String CHANNEL_ID_PLAYER = "MUSIC_PLAYER_SERVICE_CHANNEL";

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
