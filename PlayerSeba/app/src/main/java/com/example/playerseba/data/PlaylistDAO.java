package com.example.playerseba.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.playerseba.model.Musicas;
import com.example.playerseba.model.Playlist;
import com.example.playerseba.model.PlaylistSongCrossRef;
import com.example.playerseba.model.PlaylistWithSongCount;

import java.util.List;

@Dao
public interface PlaylistDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPlaylist(Playlist playlist);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertPlaylistSongCrossRef(PlaylistSongCrossRef crossRef);

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songPath = :songPath")
    void deleteSongFromPlaylist(long playlistId, String songPath);

    /**
     * NOVA CONSULTA: Busca todas as playlists e, para cada uma, conta quantas músicas estão associadas a ela.
     * Usa um LEFT JOIN para garantir que mesmo playlists com 0 músicas sejam retornadas.
     */
    @Query("SELECT p.*, COUNT(ps.songPath) as songCount FROM playlists p LEFT JOIN playlist_songs ps ON p.id = ps.playlistId GROUP BY p.id ORDER BY p.name ASC")
    List<PlaylistWithSongCount> getPlaylistsWithSongCounts();

    @Query("SELECT m.* FROM musicas m INNER JOIN playlist_songs ps ON m.path = ps.songPath WHERE ps.playlistId = :playlistId")
    List<Musicas> getSongsForPlaylist(long playlistId);

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    void deletePlaylist(long playlistId);
}
