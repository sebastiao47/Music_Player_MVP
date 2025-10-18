package com.example.playerseba;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playerseba.adapter.MusicasAdapter;
import com.example.playerseba.model.Musicas;
import com.example.playerseba.Repositorio.MusicRepository;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity implements MusicasAdapter.OnMusicaInteractionListener {

    public static final String EXTRA_PLAYLIST_ID = "PLAYLIST_ID";
    public static final String EXTRA_PLAYLIST_NAME = "PLAYLIST_NAME";

    private MusicRepository repositorio;
    private MusicasAdapter musicasAdapter;
    private long playlistId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlistId = getIntent().getLongExtra(EXTRA_PLAYLIST_ID, -1);
        String playlistName = getIntent().getStringExtra(EXTRA_PLAYLIST_NAME);

        if (playlistId == -1) {
            Toast.makeText(this, "ID da Playlist inválido.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        configurarToolbar(playlistName);
        
        repositorio = new MusicRepository(this);
        configurarRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarMusicasDaPlaylist();
    }

    private void configurarToolbar(String title) {
        Toolbar toolbar = findViewById(R.id.toolbar_playlist_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(title != null ? title : "Playlist");
        }
    }

    private void configurarRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_playlist_songs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        musicasAdapter = new MusicasAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(musicasAdapter);
    }

    private void carregarMusicasDaPlaylist() {
        repositorio.obterMusicasDaLista(playlistId, new MusicRepository.RepositoryCallback<List<Musicas>>() {
            @Override
            public void onSuccess(List<Musicas> musicas) {
                if (musicas != null) {
                    musicasAdapter.updateMusicas(musicas);
                    if (musicas.isEmpty()) {
                        Toast.makeText(PlaylistDetailActivity.this, "Para adicionar músicas, vá para a Biblioteca, clique no ícone \"+\" em uma música e escolha esta playlist.", Toast.LENGTH_LONG).show();
                    }
                } 
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(PlaylistDetailActivity.this, "Erro ao carregar as músicas da playlist.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMusicaClicada(Musicas musica, int posicao) {
        Intent result = new Intent();
        result.setAction("TOCAR_LISTA");
        result.putExtra("LISTA_ID", playlistId);
        result.putExtra("POSICAO_MUSICA", posicao);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onAddIconClicked(Musicas musica) {
        // O ícone '+' nesta tela nao tem acao, pois as musicas são removidas com clique longo.
        // A acao de adicionar é feita a partir da Biblioteca.
        mostrarDialogoRemoverMusica(musica);
    }


    public void onMusicaLongClicked(Musicas musica) {
        mostrarDialogoRemoverMusica(musica);
    }

    private void mostrarDialogoRemoverMusica(final Musicas musica) {
        new AlertDialog.Builder(this)
                .setTitle("Remover Música")
                .setMessage("Tem certeza que deseja remover '" + musica.title + "' desta playlist?")
                .setPositiveButton("Remover", (dialog, which) -> {
                    removerMusicaDaPlaylist(musica);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void removerMusicaDaPlaylist(Musicas musica) {
        if (repositorio == null) return;
        repositorio.removeSongFromPlaylist(playlistId, musica.path, new MusicRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(PlaylistDetailActivity.this, "'" + musica.title + "' removido da playlist.", Toast.LENGTH_SHORT).show();
                carregarMusicasDaPlaylist(); // Refresh the list
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(PlaylistDetailActivity.this, "Erro ao remover música.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
