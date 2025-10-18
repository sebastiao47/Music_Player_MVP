package com.example.playerseba.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.playerseba.R;
import java.io.IOException;

@Entity(tableName = "musicas")
public class Musicas {

    @PrimaryKey
    @NonNull
    public String path;

    public String title;
    public String artist;
    public String album;
    public long duration;
    public String uri;

    // Empty constructor for Room
    public Musicas() {
        this.path = "";
    }

    public Musicas(@NonNull String path, String title, String artist, String album, long duration, String uri) {
        this.path = path;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.uri = uri;
    }

    public Bitmap getAlbumArt(Context context) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (this.uri != null && !this.uri.isEmpty()) {
                retriever.setDataSource(context, Uri.parse(this.uri));
                byte[] art = retriever.getEmbeddedPicture();
                if (art != null) {
                    return BitmapFactory.decodeByteArray(art, 0, art.length);
                }
            }
        } catch (Exception e) {
            // Ignore
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                // Ignore
            }
        }
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.music);
    }
}
