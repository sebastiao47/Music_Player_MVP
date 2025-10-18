package com.example.playerseba.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.playerseba.model.Musicas;
import java.util.List;

@Dao
public interface MusicasDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Musicas song);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Musicas> novasMusicas);

    @Query("SELECT * FROM musicas WHERE path = :path LIMIT 1")
    Musicas getMusicaByPath(String path);

    @Query("SELECT * FROM musicas ORDER BY title ASC")
    List<Musicas> getAllMusicas();

    @Query("SELECT * FROM musicas WHERE title LIKE :query OR artist LIKE :query")
    List<Musicas> searchMusicas(String query);
}
