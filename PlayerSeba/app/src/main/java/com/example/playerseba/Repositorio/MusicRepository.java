package com.example.playerseba.Repositorio;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.example.playerseba.data.AppDatabase;
import com.example.playerseba.data.MusicasDAO;
import com.example.playerseba.data.PlaylistDAO;
import com.example.playerseba.model.Musicas;
import com.example.playerseba.model.Playlist;
import com.example.playerseba.model.PlaylistSongCrossRef;
import com.example.playerseba.model.PlaylistWithSongCount;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicRepository {
    private final MusicasDAO musicasDAO;
    private final PlaylistDAO playlistDAO;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Context context;

    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    public MusicRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(this.context);
        this.musicasDAO = db.musicasDAO();
        this.playlistDAO = db.playlistDAO();
    }

    public void obterTodasAsMusicas(RepositoryCallback<List<Musicas>> callback) {
        executor.execute(() -> {
            try {
                List<Musicas> musicas = musicasDAO.getAllMusicas();
                mainHandler.post(() -> callback.onSuccess(musicas));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }

    public void getPlaylistsWithSongCounts(RepositoryCallback<List<PlaylistWithSongCount>> callback) {
        executor.execute(() -> {
            try {
                List<PlaylistWithSongCount> playlists = playlistDAO.getPlaylistsWithSongCounts();
                mainHandler.post(() -> callback.onSuccess(playlists));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }

    public void obterMusicasDaLista(long playlistId, RepositoryCallback<List<Musicas>> callback) {
        executor.execute(() -> {
            try {
                List<Musicas> musicas = playlistDAO.getSongsForPlaylist(playlistId);
                mainHandler.post(() -> callback.onSuccess(musicas));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }

    public void createPlaylist(String name, RepositoryCallback<Long> callback) {
        executor.execute(() -> {
            try {
                Playlist playlist = new Playlist(name);
                long newId = playlistDAO.insertPlaylist(playlist);
                mainHandler.post(() -> callback.onSuccess(newId));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }

    public void addSongToPlaylist(long playlistId, String songPath, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                long result = playlistDAO.insertPlaylistSongCrossRef(new PlaylistSongCrossRef(playlistId, songPath));
                mainHandler.post(() -> callback.onSuccess(result != -1));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }

    public void removeSongFromPlaylist(long playlistId, String songPath, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                playlistDAO.deleteSongFromPlaylist(playlistId, songPath);
                mainHandler.post(() -> callback.onSuccess(null));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }
    
    public void deletePlaylist(long playlistId, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                playlistDAO.deletePlaylist(playlistId);
                mainHandler.post(() -> callback.onSuccess(null));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }

    public void escanearESalvarMusicas(RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                ContentResolver resolver = context.getContentResolver();
                Uri colecao = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                        ? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                        : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                String[] projecao = {
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.DATA
                };

                String selecao = MediaStore.Audio.Media.IS_MUSIC + " != 0";
                String ordenacao = MediaStore.Audio.Media.DATE_ADDED + " DESC";

                List<Musicas> novasMusicas = new ArrayList<>();

                try (Cursor cursor = resolver.query(colecao, projecao, selecao, null, ordenacao)) {
                    if (cursor != null) {
                        int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                        int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                        int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                        int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                        int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                        int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);

                        while(cursor.moveToNext()){
                            long id = cursor.getLong(idColumn);
                            String path = cursor.getString(dataColumn);
                            
                            if (TextUtils.isEmpty(path)) {
                                continue;
                            }

                            String title = cursor.getString(titleColumn);
                            String artist = cursor.getString(artistColumn);
                            String album = cursor.getString(albumColumn);
                            long duration = cursor.getLong(durationColumn);

                            Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                            
                            novasMusicas.add(new Musicas(path, title, artist, album, duration, uri.toString()));
                        }

                        if (!novasMusicas.isEmpty()) {
                            musicasDAO.insertAll(novasMusicas);
                        }
                    }
                }
                mainHandler.post(() -> callback.onSuccess(null));
            } catch (Exception e) {
                Log.e("MusicRepository", "Erro ao escanear músicas", e);
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }
}
