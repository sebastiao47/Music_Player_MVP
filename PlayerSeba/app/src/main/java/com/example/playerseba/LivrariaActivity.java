package com.example.playerseba;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.acrcloud.rec.ACRCloudClient;
import com.acrcloud.rec.ACRCloudConfig;
import com.acrcloud.rec.ACRCloudResult;
import com.acrcloud.rec.IACRCloudListener;
import com.example.playerseba.adapter.MusicasAdapter;
import com.example.playerseba.model.Musicas;
import com.example.playerseba.model.Playlist;
import com.example.playerseba.model.PlaylistWithSongCount;
import com.example.playerseba.Repositorio.MusicRepository;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A LivrariaActivity mostra todas as músicas disponíveis no dispositivo.
 * Permite ao usuário tocar uma música, adicioná-la a uma playlist, ou iniciar o reconhecimento de uma música ambiente.
 */
public class LivrariaActivity extends AppCompatActivity
        implements MusicasAdapter.OnMusicaInteractionListener, IACRCloudListener {

    private static final String TAG = "LivrariaActivity";
    private static final int REQUEST_CODE_RECORD_AUDIO_LIB = 201;

    private MusicasAdapter musicasAdapter;
    private MusicRepository repositorio;
    private List<Musicas> todasAsMusicas = new ArrayList<>();

    private ACRCloudClient acrCloudClient;
    private boolean isRecognizing = false;

    /**
     * Chamado quando a Activity é criada pela primeira vez.
     * Aqui configuramos a interface, o RecyclerView e carregamos as músicas.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_livraria);

        configurarToolbar();
        repositorio = new MusicRepository(this);
        configurarRecyclerView();
        carregarTodasAsMusicas();

        if (getIntent().hasExtra("SONG_TO_ADD_PATH")) {
            String songPath = getIntent().getStringExtra("SONG_TO_ADD_PATH");
            // A lista de músicas pode ainda não ter sido carregada, então esperamos pelo onSuccess de carregarTodasAsMusicas
        } else if ("ACTION_START_RECOGNITION".equals(getIntent().getAction())) {
            initializeAndStartRecognition();
        }
    }

    /**
     * Encontra uma música na lista `todasAsMusicas` usando o seu caminho (path).
     * @param path O caminho do arquivo da música a ser encontrada.
     * @return O objeto Musicas se encontrado, caso contrário, null.
     */
    private Musicas encontrarMusicaPeloPath(String path) {
        for (Musicas musica : todasAsMusicas) {
            if (Objects.equals(musica.path, path)) {
                return musica;
            }
        }
        return null;
    }

    /**
     * Configura a barra de ferramentas (Toolbar) da Activity.
     */
    private void configurarToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_livraria);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Biblioteca de Músicas");
        }
    }

    /**
     * Configura o RecyclerView que vai mostrar a lista de músicas.
     */
    private void configurarRecyclerView() {
        RecyclerView recyclerMusicas = findViewById(R.id.recycler_songs);
        recyclerMusicas.setLayoutManager(new LinearLayoutManager(this));
        musicasAdapter = new MusicasAdapter(new ArrayList<>(), this);
        recyclerMusicas.setAdapter(musicasAdapter);
    }

    /**
     * Pede ao repositório para carregar todas as músicas do dispositivo e atualiza a lista na tela.
     */
    private void carregarTodasAsMusicas() {
        repositorio.obterTodasAsMusicas(new MusicRepository.RepositoryCallback<List<Musicas>>() {
            @Override
            public void onSuccess(List<Musicas> musicas) {
                todasAsMusicas = musicas;
                musicasAdapter.updateMusicas(musicas);

                // Agora que as músicas foram carregadas, podemos tentar adicionar a música, se for o caso.
                if (getIntent().hasExtra("SONG_TO_ADD_PATH")) {
                    String songPath = getIntent().getStringExtra("SONG_TO_ADD_PATH");
                    Musicas musicaParaAdicionar = encontrarMusicaPeloPath(songPath);
                    if (musicaParaAdicionar != null) {
                        mostrarDialogoAdicionarNaPlaylist(musicaParaAdicionar);
                        getIntent().removeExtra("SONG_TO_ADD_PATH"); // Evita reabrir o diálogo ao rotacionar a tela
                    }
                }
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(LivrariaActivity.this, "Erro ao carregar músicas.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Inicializa o cliente ACRCloud para reconhecimento de música, pedindo permissão se necessário.
     */
    private void initializeAndStartRecognition() {
        if (isRecognizing) return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_RECORD_AUDIO_LIB);
            return;
        }

        ACRCloudConfig config = new ACRCloudConfig();
        config.acrcloudListener = this;
        config.context = this;
        config.host = "identify-eu-west-1.acrcloud.com";
        config.accessKey = "da324faab890783b1604af92beddece2"; // Substitua com sua chave
        config.accessSecret = "4PX3iQXyexPi7VACklFFl9gXGDgMhx4Pc4Ynb3Xl"; // Substitua com seu segredo
        config.recorderConfig.rate = 44100;
        config.recorderConfig.channels = 1;

        acrCloudClient = new ACRCloudClient();
        if (!acrCloudClient.initWithConfig(config)) {
            Toast.makeText(this, "Falha ao inicializar ACRCloud", Toast.LENGTH_SHORT).show();
            return;
        }
        
        startRecognitionFlow();
    }

    /**
     * Inicia o processo de reconhecimento de áudio.
     */
    private void startRecognitionFlow() {
        try {
            if (!isRecognizing) {
                isRecognizing = acrCloudClient.startRecognize();
                if (isRecognizing) {
                    Toast.makeText(this, "🎧 Ouvindo para identificar...", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "❌ Falha ao iniciar reconhecimento", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro no reconhecimento ACRCloud", e);
            Toast.makeText(this, "Erro no serviço de reconhecimento", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Para o processo de reconhecimento de áudio.
     */
    private void stopRecognition() {
        if (acrCloudClient != null && isRecognizing) {
            acrCloudClient.cancel();
            isRecognizing = false;
        }
    }

    /**
     * Callback do ACRCloud com o resultado do reconhecimento.
     */
    @Override
    public void onResult(ACRCloudResult result) {
        stopRecognition();
        String dialogTitle = "Erro";
        String dialogMessage = "Falha ao processar resposta.";

        try {
            JSONObject json = new JSONObject(result.getResult());
            JSONObject status = json.getJSONObject("status");
            int code = status.getInt("code");

            if (code == 0) {
                JSONObject music = json.getJSONObject("metadata").getJSONArray("music").getJSONObject(0);
                String title = music.getString("title");
                String artist = music.getJSONArray("artists").getJSONObject(0).getString("name");
                dialogTitle = "Música Reconhecida";
                dialogMessage = "Título: " + title + "\nArtista: " + artist;
            } else if (code == 1001) {
                dialogTitle = "Sem Resultado";
                dialogMessage = "Nenhuma música encontrada.";
            } else {
                dialogTitle = "Erro no Reconhecimento";
                dialogMessage = "Código: " + code + " - " + status.getString("msg");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao processar resultado ACRCloud", e);
        } finally {
            showRecognitionResultDialog(dialogTitle, dialogMessage);
        }
    }

    @Override
    public void onVolumeChanged(double volume) {}

    /**
     * Mostra o resultado do reconhecimento numa caixa de diálogo.
     */
    private void showRecognitionResultDialog(String title, String message) {
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show());
    }

    /**
     * Chamado quando o usuário clica numa música da lista. Retorna a música selecionada para a MainActivity tocar.
     */
    @Override
    public void onMusicaClicada(Musicas musica, int posicao) {
        int realPos = todasAsMusicas.indexOf(musica);
        if (realPos == -1) realPos = posicao;

        Intent result = new Intent();
        result.putExtra("POSICAO_MUSICA_TOCAR", realPos);
        setResult(RESULT_OK, result);
        finish();
    }

    /**
     * Chamado quando o usuário clica no ícone '+' para adicionar uma música.
     */
    @Override
    public void onAddIconClicked(Musicas musica) {
        mostrarDialogoAdicionarNaPlaylist(musica);
    }

    /**
     * Mostra um diálogo com a lista de playlists disponíveis para adicionar a música.
     */
    private void mostrarDialogoAdicionarNaPlaylist(Musicas musica) {
        repositorio.getPlaylistsWithSongCounts(new MusicRepository.RepositoryCallback<List<PlaylistWithSongCount>>() {
            @Override
            public void onSuccess(List<PlaylistWithSongCount> playlistsWithCount) {
                if (playlistsWithCount == null || playlistsWithCount.isEmpty()) {
                    Toast.makeText(LivrariaActivity.this, "Nenhuma playlist criada. Vá a 'Playlists' para criar uma.", Toast.LENGTH_LONG).show();
                    return;
                }

                String[] nomes = new String[playlistsWithCount.size()];
                for (int i = 0; i < playlistsWithCount.size(); i++) {
                    nomes[i] = playlistsWithCount.get(i).playlist.name;
                }

                new AlertDialog.Builder(LivrariaActivity.this)
                        .setTitle("Adicionar à Playlist")
                        .setItems(nomes, (dialog, which) -> {
                            Playlist playlistSelecionada = playlistsWithCount.get(which).playlist;
                            adicionarMusicaAPlaylist(musica, playlistSelecionada);
                        })
                        .show();
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(LivrariaActivity.this, "Erro ao carregar playlists.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Adiciona a música selecionada à playlist escolhida, usando o repositório.
     */
    private void adicionarMusicaAPlaylist(Musicas musica, Playlist playlist) {
        repositorio.addSongToPlaylist(playlist.id, musica.path, new MusicRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                String msg = success ? "'" + musica.title + "' adicionado a '" + playlist.name + "'" 
                                     : "Música já existe na playlist '" + playlist.name + "'";
                Toast.makeText(LivrariaActivity.this, msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(LivrariaActivity.this, "Erro ao adicionar música.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_livraria, menu);
        MenuItem itemBusca = menu.findItem(R.id.action_search_livraria);
        SearchView searchView = (SearchView) itemBusca.getActionView();
        if (searchView != null) {
            searchView.setQueryHint("Buscar em toda a biblioteca...");
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String query) { return false; }
                @Override public boolean onQueryTextChange(String newText) {
                    if (musicasAdapter != null) musicasAdapter.getFilter().filter(newText);
                    return true;
                }
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_recognize_livraria) {
            initializeAndStartRecognition();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_RECORD_AUDIO_LIB) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeAndStartRecognition();
            } else {
                Toast.makeText(this, "Permissão de áudio necessária para o reconhecimento.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRecognition();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (acrCloudClient != null) {
            acrCloudClient.release();
            acrCloudClient = null;
        }
    }
}
