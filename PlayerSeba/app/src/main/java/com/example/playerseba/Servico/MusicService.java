package com.example.playerseba.Servico;

import static android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ALL;
import static android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ONE;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.example.playerseba.App;
import com.example.playerseba.MainActivity;
import com.example.playerseba.R;
import com.example.playerseba.model.Musicas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


 // O MusicService é um serviço que corre em background para tocar música.
 // Ele continua a tocar mesmo que o usuário feche a aplicação.
 // Também é responsável por mostrar e gerir a notificação com os controlos de mídia.

public class MusicService extends Service implements
        AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener {

    private static final String TAG = "MusicService";
    public static final String ACTION_PLAY_PAUSE = "com.example.playerseba.ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.example.playerseba.ACTION_NEXT";
    public static final String ACTION_PREV = "com.example.playerseba.ACTION_PREV";
    private static final int NOTIFICATION_ID = 1;

    private MediaPlayer player;
    private MediaSessionCompat mediaSessionCompat;

    private List<Musicas> currentPlaylist = new ArrayList<>();
    private int currentIndex = -1;
    private boolean shuffleEnabled = false;
    private int repeatMode = REPEAT_OFF;
    private MusicServiceCallback callback;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    public static final int REPEAT_OFF = 0;
    public static final int REPEAT_ONE = 1;
    public static final int REPEAT_ALL = 2;

    // Interface para comunicar eventos de volta para o Presenter.
    public interface MusicServiceCallback {
        void onMusicChanged(Musicas musica);
        void onProgressUpdate(long posicao, long duracao);
        void onPlaybackStateChanged(boolean isTocando);
        void onShuffleModeChanged(boolean isAtivado);
        void onRepeatModeChanged(int modoRepeticao);
        void onError(String mensagem);
    }

    // Binder para permitir que o Presenter se conecte a este servico.
    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mediaSessionCompat = new MediaSessionCompat(this, "PlayerSebaMediaSession");
        mediaSessionCompat.setActive(true);
        initializePlayer();
        Log.d(TAG, "MusicService criado e canal de notificação garantido.");
    }


     // Garante que o canal de notificação para o player exista.
     // Essencial para serviços em primeiro plano (foreground service).

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) return;

            NotificationChannel serviceChannel = new NotificationChannel(
                    App.CHANNEL_ID_PLAYER,
                    "Serviço de Música",
                    NotificationManager.IMPORTANCE_LOW // A importância deve ser pelo menos LOW para não ser morto.
            );
            serviceChannel.setDescription("Canal para a notificação persistente do player de música");
            manager.createNotificationChannel(serviceChannel);
        }
    }


     // Inicializa o objecto MediaPlayer se ele ainda não existir.

    private void initializePlayer() {
        if (player == null) {
            player = new MediaPlayer();
            player.setOnCompletionListener(this);
            player.setOnErrorListener(this);
            player.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
        }
    }


     // Chamado quando o serviço recebe um comando (ex: dos botões da notificação).

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handleNotificationAction(intent.getAction());
        }
        return START_NOT_STICKY; // O serviço não será recriado automaticamente se for morto.
    }


     // Processa as acções recebidas da notificação.

    private void handleNotificationAction(String action) {
        switch (action) {
            case ACTION_PLAY_PAUSE: playOrPause(); break;
            case ACTION_NEXT: nextSong(); break;
            case ACTION_PREV: prevSong(); break;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }


     // Define a lista de músicas que o serviço deve tocar.

    public void setPlaylist(List<Musicas> musicas) {
        this.currentPlaylist = new ArrayList<>(musicas);
        this.currentIndex = -1; // Reseta o índice para a nova playlist.
    }


     // Inicia a reprodução de uma música a partir de um índice na playlist.

    public void playSongAtIndex(int index) {
        if (index < 0 || index >= currentPlaylist.size()) {
            stopSelf();
            return;
        }

        Musicas musica = currentPlaylist.get(index);
        
        if (musica == null || TextUtils.isEmpty(musica.uri)) {
            if (callback != null) callback.onError("Faixa inválida, a pular...");
            nextSong();
            return;
        }

        // Mostra a notificação imediatamente para evitar o crash "Bad notification".
        startForeground(NOTIFICATION_ID, createMediaNotification(null, false));

        if (player == null) initializePlayer();

        if (requestAudioFocus()) {
            try {
                player.reset();
                player.setDataSource(this, Uri.parse(musica.uri));
                player.prepareAsync();
                player.setOnPreparedListener(mp -> {
                    mp.start();
                    currentIndex = index;
                    startProgressUpdate();
                    if (callback != null) {
                        callback.onMusicChanged(musica);
                        callback.onPlaybackStateChanged(true);
                    }
                    updateNotification();
                });
            } catch (Exception e) {
                Log.e(TAG, "Erro fatal ao preparar MediaPlayer", e);
                if (callback != null) callback.onError("Erro ao reproduzir música");
                stopForeground(true);
            }
        } else {
            if (callback != null) callback.onError("Não foi possível obter o foco do áudio.");
            stopForeground(true);
        }
    }
    

     // Actualiza a notificação com as informações da música actual.

    @SuppressLint("MissingPermission")
    private void updateNotification() {
        if(currentIndex != -1){
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, createMediaNotification(getCurrentSong(), isPlaying()));
        }
    }


     // Cria a notificação de mídia, com capa, título e botões de acção.

    private Notification createMediaNotification(Musicas musica, boolean isPlaying) {
        String title = (musica != null) ? musica.title : "Player Seba";
        String artist = (musica != null) ? musica.artist : "A carregar...";
        Bitmap albumArt = (musica != null) ? musica.getAlbumArt(this) : null;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Cria os PendingIntents para os botões da notificação, comunicando diretamente com o serviço.
        PendingIntent prevIntent = getServicePendingIntent(this, ACTION_PREV, 1);
        PendingIntent playPauseIntent = getServicePendingIntent(this, ACTION_PLAY_PAUSE, 2);
        PendingIntent nextIntent = getServicePendingIntent(this, ACTION_NEXT, 3);

        int playPauseIcon = isPlaying ? R.drawable.pause : R.drawable.play;
        String playPauseTitle = isPlaying ? "Pausar" : "Tocar";

        MediaStyle mediaStyle = new MediaStyle()
                .setMediaSession(mediaSessionCompat.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2);

        return new NotificationCompat.Builder(this, App.CHANNEL_ID_PLAYER)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(R.drawable.music)
                .setLargeIcon(albumArt)
                .setContentIntent(contentIntent)
                .setOngoing(isPlaying)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(R.drawable.previous, "Anterior", prevIntent)
                .addAction(playPauseIcon, playPauseTitle, playPauseIntent)
                .addAction(R.drawable.next, "Próxima", nextIntent)
                .setStyle(mediaStyle)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }


     // Cria um PendingIntent que envia um comando directamente para este serviço.

    private PendingIntent getServicePendingIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getService(context, requestCode, intent, flags);
    }

    //  Comandos do Player

    public void playOrPause() {
        if (player == null) return;
        if (currentIndex == -1 && !currentPlaylist.isEmpty()) {
            playSongAtIndex(0);
            return;
        }
        try {
            if (player.isPlaying()) {
                player.pause();
                stopProgressUpdate();
                stopForeground(false); 
                if (callback != null) callback.onPlaybackStateChanged(false);
            } else {
                if (requestAudioFocus()) {
                    player.start();
                    startProgressUpdate();
                    startForeground(NOTIFICATION_ID, createMediaNotification(getCurrentSong(), true));
                    if (callback != null) callback.onPlaybackStateChanged(true);
                }
            }
            updateNotification();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Erro de estado em playOrPause, a resetar o player.", e);
            playSongAtIndex(currentIndex);
        }
    }

    public void nextSong() { if (currentPlaylist.isEmpty()) return; if (shuffleEnabled) { currentIndex = new Random().nextInt(currentPlaylist.size()); } else { currentIndex = (currentIndex + 1) % currentPlaylist.size(); } playSongAtIndex(currentIndex); }
    public void prevSong() { if (currentPlaylist.isEmpty()) return; if (shuffleEnabled) { currentIndex = new Random().nextInt(currentPlaylist.size()); } else { currentIndex = (currentIndex - 1 + currentPlaylist.size()) % currentPlaylist.size(); } playSongAtIndex(currentIndex); }
    public void seekTo(long position) { if (player != null) try { player.seekTo((int) position); } catch (Exception e) { Log.e(TAG, "Seek fail", e); } }
    public boolean isShuffleEnabled() { return shuffleEnabled; }
    public int getRepeatMode() { return repeatMode; }

    public boolean toggleShuffleMode() { shuffleEnabled = !shuffleEnabled; if (callback != null) callback.onShuffleModeChanged(shuffleEnabled); return shuffleEnabled; }
    public int toggleRepeatMode() {
        repeatMode = (repeatMode + 1) % 3;
        if (player != null) {
            player.setLooping(repeatMode == REPEAT_ONE);
        }
        if (callback != null) {
            callback.onRepeatModeChanged(repeatMode);
        }
        return repeatMode;
    }
    public Musicas getCurrentSong() { return (currentIndex >= 0 && currentIndex < currentPlaylist.size()) ? currentPlaylist.get(currentIndex) : null; }
    public long getCurrentPosition() { try { return (player != null && player.isPlaying()) ? player.getCurrentPosition() : 0; } catch (IllegalStateException e) { return 0; } }
    public long getDuration() { try { return (player != null) ? player.getDuration() : 0; } catch (IllegalStateException e) { return 0; } }
    public boolean isPlaying() { return player != null && player.isPlaying(); }
    public List<Musicas> getCurrentPlaylist() { return new ArrayList<>(currentPlaylist); }
    public boolean hasActivePlaylist() { return !currentPlaylist.isEmpty(); }
    
    private boolean requestAudioFocus() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { AudioAttributes attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(); audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes).setAcceptsDelayedFocusGain(true).setOnAudioFocusChangeListener(this).build(); return audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED; } else { return audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED; } }
    @Override public void onAudioFocusChange(int focusChange) { switch (focusChange) { case AudioManager.AUDIOFOCUS_GAIN: if (player != null && !player.isPlaying()) player.start(); player.setVolume(1.0f, 1.0f); break; case AudioManager.AUDIOFOCUS_LOSS: case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: if (player != null && player.isPlaying()) player.pause(); break; case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: if (player != null && player.isPlaying()) player.setVolume(0.1f, 0.1f); break; } }
    private void startProgressUpdate() { stopProgressUpdate(); progressRunnable = new Runnable() { @Override public void run() { if (isPlaying() && callback != null) { callback.onProgressUpdate(getCurrentPosition(), getDuration()); handler.postDelayed(this, 1000); } } }; handler.post(progressRunnable); }
    private void stopProgressUpdate() { if (progressRunnable != null) handler.removeCallbacks(progressRunnable); }
    public void setCallback(MusicServiceCallback callback) { this.callback = callback; }
    
    // Callbacks do MediaPlayer

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (repeatMode == REPEAT_ONE) {
            playSongAtIndex(currentIndex);
            return;
        }
        if (repeatMode == REPEAT_ALL) {
            nextSong();
            return;
        }
        if (currentIndex < currentPlaylist.size() - 1) {
            nextSong();
        } else {
            stopProgressUpdate();
            if(player != null) {
                player.seekTo(0);
                player.pause();
            }
            if (callback != null) callback.onPlaybackStateChanged(false);
            updateNotification();
            stopForeground(false);
        }
    }

    @Override public boolean onError(MediaPlayer mp, int what, int extra) { 
        Log.e(TAG, "MediaPlayer Error! What: " + what + ", Extra: " + extra);
        if (callback != null) callback.onError("Ocorreu um erro no player."); 
        if(currentPlaylist.size() > 1){
            nextSong(); 
        }
        return true; 
    }

    @Override public void onDestroy() { 
        super.onDestroy(); 
        stopProgressUpdate(); 
        if (mediaSessionCompat != null) mediaSessionCompat.release(); 
        if (player != null) { 
            if(player.isPlaying()) player.stop();
            player.release(); 
            player = null; 
        }
        if (audioManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest != null) audioManager.abandonAudioFocusRequest(audioFocusRequest);
            } else {
                audioManager.abandonAudioFocus(this);
            }
        }
        stopForeground(true); 
        Log.d(TAG, "MusicService destruído."); 
    }
}
