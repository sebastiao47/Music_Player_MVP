package com.example.playerseba.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(tableName = "playlist_songs", 
        primaryKeys = {"playlistId", "songPath"},
        foreignKeys = {
            @ForeignKey(entity = Playlist.class,
                        parentColumns = "id",
                        childColumns = "playlistId",
                        onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = Musicas.class,
                        parentColumns = "path",
                        childColumns = "songPath",
                        onDelete = ForeignKey.CASCADE)
        }
)
public class PlaylistSongCrossRef {
    public long playlistId;
    @NonNull
    public String songPath;

    public PlaylistSongCrossRef(long playlistId, @NonNull String songPath) {
        this.playlistId = playlistId;
        this.songPath = songPath;
    }
}
