package com.example.playerseba.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.playerseba.model.Musicas;
import com.example.playerseba.model.Playlist;
import com.example.playerseba.model.PlaylistSongCrossRef;
import com.example.playerseba.model.PlaylistWithSongCount;

// VERSÃO INCREMENTADA PARA 13 - Reflete a adição do método getPlaylistsWithSongCounts no DAO.
@Database(entities = {Musicas.class, Playlist.class, PlaylistSongCrossRef.class}, version = 13, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract MusicasDAO musicasDAO();
    public abstract PlaylistDAO playlistDAO();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "player_seba_database"
                            ).fallbackToDestructiveMigration() // Destroi o BD antigo na mudança de versão
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
