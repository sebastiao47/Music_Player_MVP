package com.example.playerseba;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playerseba.adapter.PlaylistAdapter;
import com.example.playerseba.model.Playlist;
import com.example.playerseba.model.PlaylistWithSongCount;
import com.example.playerseba.Repositorio.MusicRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;


 // ListasActivity é a tela responsável por mostrar e gerir as playlists do usuário.
 // Permite criar novas playlists, ver as existentes e apagar playlists.

public class ListasActivity extends AppCompatActivity implements PlaylistAdapter.OnPlaylistClickListener {

    private static final int REQUEST_CODE_PLAYLIST_DETAIL = 1;
    private MusicRepository repositorio;
    private PlaylistAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listas);

        configurarToolbar();
        repositorio = new MusicRepository(this);
        configurarRecyclerView();

        FloatingActionButton fab = findViewById(R.id.fab_add_playlist);
        fab.setOnClickListener(v -> mostrarDialogoCriarPlaylist());
    }

     // O onResume e chamado toda vez que a tela volta a ser visível.
     // Garantimos que a lista de playlists esteja sempre actualizada, movendo o carregamento para cá.

    @Override
    protected void onResume() {
        super.onResume();
        carregarPlaylists();
    }


     // Configura a barra de ferramentas (Toolbar) da Activity.

    private void configurarToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_listas);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Minhas Playlists");
        }
    }


     // Configura o RecyclerView que vai mostrar a lista de playlists.

    private void configurarRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_listas);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlaylistAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
    }


     // Pede ao repositorio para carregar as playlists com a contagem de musicas e actualiza a UI.

    private void carregarPlaylists() {
        repositorio.getPlaylistsWithSongCounts(new MusicRepository.RepositoryCallback<List<PlaylistWithSongCount>>() {
            @Override
            public void onSuccess(List<PlaylistWithSongCount> playlists) {
                if (playlists != null) {
                    adapter.updatePlaylists(playlists);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ListasActivity.this, "Erro ao carregar playlists.", Toast.LENGTH_SHORT).show();
            }
        });
    }


     // Mostra uma caixa de dialogo para o usuario inserir o nome da nova playlist.

    private void mostrarDialogoCriarPlaylist() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nova Playlist");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Nome da Playlist");
        builder.setView(input);

        builder.setPositiveButton("Criar", (dialog, which) -> {
            String nomePlaylist = input.getText().toString().trim();
            if (!nomePlaylist.isEmpty()) {
                criarPlaylist(nomePlaylist);
            } else {
                Toast.makeText(this, "O nome não pode ser vazio.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }


     //Chama o repositório para criar uma nova playlist no banco de dados.

    private void criarPlaylist(String nome) {
        repositorio.createPlaylist(nome, new MusicRepository.RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long newId) {
                Toast.makeText(ListasActivity.this, "Playlist '" + nome + "' criada.", Toast.LENGTH_SHORT).show();
                carregarPlaylists(); // Recarrega a lista para mostrar a nova playlist.
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ListasActivity.this, "Erro ao criar playlist.", Toast.LENGTH_SHORT).show();
            }
        });
    }


     //Mostra um diálogo de confirmacao antes de apagar uma playlist.

    private void mostrarDialogoDeletarPlaylist(Playlist playlist) {
        new AlertDialog.Builder(this)
                .setTitle("Apagar Playlist")
                .setMessage("Tem certeza que quer apagar a playlist '" + playlist.name + "'?")
                .setPositiveButton("Apagar", (dialog, which) -> deletarPlaylist(playlist))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Chama o repositório para apagar uma playlist do banco de dados.
     */
    private void deletarPlaylist(Playlist playlist) {
        repositorio.deletePlaylist(playlist.id, new MusicRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(ListasActivity.this, "Playlist '" + playlist.name + "' apagada.", Toast.LENGTH_SHORT).show();
                carregarPlaylists(); // Recarrega a lista para remover a playlist apagada.
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ListasActivity.this, "Erro ao apagar a playlist.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Chamado quando o usuário clica numa playlist. Abre a tela de detalhes dessa playlist.
     */
    @Override
    public void onPlaylistClicked(Playlist playlist) {
        Intent intent = new Intent(this, PlaylistDetailActivity.class);
        intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID, playlist.id);
        intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_NAME, playlist.name);
        startActivityForResult(intent, REQUEST_CODE_PLAYLIST_DETAIL);
    }

    /**
     * Chamado quando o usuário faz um clique longo numa playlist. Mostra a opção para apagar.
     */
    @Override
    public void onPlaylistLongClicked(Playlist playlist) {
        mostrarDialogoDeletarPlaylist(playlist);
    }

    /**
     * Recebe o resultado de outra Activity. Usado para iniciar a reprodução de uma playlist
     * a partir da tela de detalhes.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PLAYLIST_DETAIL && resultCode == RESULT_OK && data != null) {
            // Repassa o resultado para a MainActivity, que está à espera para tocar a música.
            setResult(RESULT_OK, data);
            finish();
        }
    }

    /**
     * Lida com cliques nos itens do menu, como o botão de voltar.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
