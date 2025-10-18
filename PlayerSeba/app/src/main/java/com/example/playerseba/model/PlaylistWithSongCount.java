package com.example.playerseba.model;

import androidx.room.Embedded;

/*
  POJO (Plain Old Java Object) para agrupar uma entidade Playlist
  com a contagem de músicas associadas a ela.
 */
public class PlaylistWithSongCount {
    @Embedded
    public Playlist playlist;

    public int songCount;
}
