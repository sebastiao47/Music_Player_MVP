package com.example.playerseba;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.example.playerseba.adapter.MusicasAdapter;
import com.example.playerseba.model.Musicas;
import com.example.playerseba.presenter.Presenter;
import com.example.playerseba.view.ContratoView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ContratoView, MusicasAdapter.OnMusicaInteractionListener {

    // Códigos de requisicao para startActivityForResult e permissões.
    private static final int REQUEST_CODE_PERMISSIONS = 100;
    private static final int REQUEST_CODE_LIVRARIA = 101;
    private static final int REQUEST_CODE_LISTAS = 102;
    private static final int REQUEST_CODE_RECORD_AUDIO = 200;

    // Componentes da arquitetura MVP e UI
    private Presenter presenter;
    private MusicasAdapter musicasAdapter;
    private ObjectAnimator albumArtAnimator;

    // Referências para os componentes visuais (Views)
    private ImageView imgAlbumArt;
    private TextView textTitle, textArtist, textCurrentTime, textTotalTime;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnShuffle, btnRepeat;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa e configura todos os componentes da UI.
        inicializarViews();
        configurarToolbar();
        configurarRecyclerView();
        configurarClickListeners();
        configurarSeekBar();
        configurarAnimacao();

        // Cria o Presenter, que vai gerir a logica da UI.
        presenter = new Presenter(this, this);
        // Verifica se a aplicação tem as permissões necessárias para funcionar.
        verificarPermissoes();
    }


    private void inicializarViews() {
        imgAlbumArt = findViewById(R.id.img_album_art);
        textTitle = findViewById(R.id.text_title);
        textArtist = findViewById(R.id.text_artist);
        textCurrentTime = findViewById(R.id.text_current_time);
        textTotalTime = findViewById(R.id.text_total_time);
        seekBar = findViewById(R.id.seek_bar);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnRepeat = findViewById(R.id.btn_repeat);
    }


     // Configura a Toolbar como a barra de acção principal da Activity.

    private void configurarToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }


     // Configura o RecyclerView que vai mostrar a lista de músicas.

    private void configurarRecyclerView() {
        RecyclerView recyclerSongs = findViewById(R.id.recyclerSongs);
        recyclerSongs.setLayoutManager(new LinearLayoutManager(this));
        musicasAdapter = new MusicasAdapter(new ArrayList<>(), this);
        recyclerSongs.setAdapter(musicasAdapter);
    }


     // Configura os "ouvintes" de clique para os botões do player.

    private void configurarClickListeners() {
        btnPlayPause.setOnClickListener(v -> presenter.tocarOuPausar());
        findViewById(R.id.btn_next).setOnClickListener(v -> presenter.proximaMusica());
        findViewById(R.id.btn_previous).setOnClickListener(v -> presenter.musicaAnterior());
        btnShuffle.setOnClickListener(v -> presenter.alternarModoShuffle());
        btnRepeat.setOnClickListener(v -> presenter.alternarModoRepeticao());
    }


     // Configura a barra de progresso (SeekBar) para permitir que o usuario avance ou retroceda na musica.

    private void configurarSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Actualiza o texto do tempo actual enquanto o usuario arrasta a barra.
                if (fromUser) {
                    textCurrentTime.setText(formatarTempo(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Quando o usuário solta a barra, manda o player para a nova posicao.
                presenter.avancarPara(seekBar.getProgress());
            }
        });
    }


     // Configura a animacao de rotacao da capa do album.

    private void configurarAnimacao() {
        albumArtAnimator = ObjectAnimator.ofFloat(imgAlbumArt, "rotation", 0f, 360f);
        albumArtAnimator.setDuration(20000); // Duração da rotacao completa
        albumArtAnimator.setInterpolator(new LinearInterpolator()); // Rotacao a velocidade constante
        albumArtAnimator.setRepeatCount(ValueAnimator.INFINITE); // Repetir infinitamente
    }


     // Verifica se o aplicativo tem as permissões necessárias (ler audio e mostrar notificacoes).
     // Se nao tiver, pede ao usuário.

    private void verificarPermissoes() {
        List<String> perms = new ArrayList<>();
        String storagePerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, storagePerm) != PackageManager.PERMISSION_GRANTED) {
            perms.add(storagePerm);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!perms.isEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
        } else {
            presenter.iniciarCarregamentoDeMusicas();
        }
    }


     // Formata um tempo em milissegundos para o formato "minutos:segundos".

    private String formatarTempo(long milissegundos) {
        long totalSegundos = milissegundos / 1000;
        long minutos = totalSegundos / 60;
        long segundos = totalSegundos % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos);
    }


     //Inicia ou retoma a animacao de rotação da capa do album.

    private void startAlbumRotation() {
        if (albumArtAnimator != null && !albumArtAnimator.isStarted()) {
            albumArtAnimator.start();
        } else if (albumArtAnimator != null && albumArtAnimator.isPaused()) {
            albumArtAnimator.resume();
        }
    }


     // Pausa a animação de rotação da capa do álbum.

    private void stopAlbumRotation() {
        if (albumArtAnimator != null && albumArtAnimator.isRunning()) {
            albumArtAnimator.pause();
        }
    }

    // Metodos do ContratoView

    @Override
    public void atualizarListaMusicas(List<Musicas> musicas) {
        runOnUiThread(() -> {
            if (musicasAdapter != null) {
                musicasAdapter.updateMusicas(musicas);
            }
        });
    }

    @Override
    public void musicaMudou(Musicas musica) {
        runOnUiThread(() -> {
            TransitionManager.beginDelayedTransition(findViewById(android.R.id.content));
            if (musica != null) {
                textTitle.setText(musica.title);
                textArtist.setText(musica.artist);
                if (musica.getAlbumArt(this) != null) {
                    imgAlbumArt.setImageBitmap(musica.getAlbumArt(this));
                } else {
                    imgAlbumArt.setImageResource(R.drawable.music);
                }
                if (presenter.estaTocando()) {
                    startAlbumRotation();
                } else {
                    stopAlbumRotation();
                }
            } else {
                textTitle.setText("Nenhuma música a tocar");
                textArtist.setText("Seleccione uma música");
                imgAlbumArt.setImageResource(R.drawable.music);
                stopAlbumRotation();
            }
        });
    }

    @Override
    public void atualizarProgresso(long pos, long dur) {
        runOnUiThread(() -> {
            seekBar.setMax((int) dur);
            seekBar.setProgress((int) pos);
            textCurrentTime.setText(formatarTempo(pos));
            textTotalTime.setText(formatarTempo(dur));
        });
    }

    @Override
    public void estadoReproducaoMudou(boolean isPlaying) {
        runOnUiThread(() -> {
            btnPlayPause.setImageResource(isPlaying ? R.drawable.pause : R.drawable.play);
            if (isPlaying) {
                startAlbumRotation();
            } else {
                stopAlbumRotation();
            }
        });
    }

    @Override
    public void modoEmbaralharMudou(boolean isEnabled) {
        runOnUiThread(() -> {
            int cor = isEnabled ? ContextCompat.getColor(this, R.color.accent_color) : ContextCompat.getColor(this, android.R.color.white);
            btnShuffle.setColorFilter(cor, PorterDuff.Mode.SRC_IN);
        });
    }

    @Override
    public void modoRepetirMudou(int repeatMode) {
        runOnUiThread(() -> {
            int cor;
            int iconRes;
            if (repeatMode == Presenter.REPEAT_OFF) {
                cor = ContextCompat.getColor(this, android.R.color.white);
                iconRes = R.drawable.repeat;
            } else if (repeatMode == Presenter.REPEAT_ONE) {
                cor = ContextCompat.getColor(this, R.color.accent_color);
                iconRes = R.drawable.repeat_one;
            } else { // REPEAT_ALL
                cor = ContextCompat.getColor(this, R.color.accent_color);
                iconRes = R.drawable.repeat;
            }
            btnRepeat.setImageResource(iconRes);
            btnRepeat.setColorFilter(cor, PorterDuff.Mode.SRC_IN);
        });
    }

    @Override
    public void mensagemErro(String mensagem) {
        runOnUiThread(() -> Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show());
    }

    @Override
    public void pedirPermissaoGravacaoAudio() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_RECORD_AUDIO);
        } else {
            abrirTelaDeReconhecimento();
        }
    }
    
    @Override
    public void mostrarProgresso(boolean mostrar) {  }

    private void abrirTelaDeReconhecimento() {
        Intent intent = new Intent(this, LivrariaActivity.class);
        intent.setAction("ACTION_START_RECOGNITION");
        startActivity(intent);
    }

    //  Métodos do OnMusicaInteractionListener (cliques na lista)

    @Override
    public void onMusicaClicada(Musicas musica, int posicao) {
        if (presenter != null) {
            presenter.tocarNaPosicao(posicao);
        }
    }

    @Override
    public void onAddIconClicked(Musicas musica) {
        Intent intent = new Intent(this, LivrariaActivity.class);
        intent.putExtra("SONG_TO_ADD_PATH", musica.path);
        startActivity(intent);
    }

    // gerenciamento do  do Menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_livraria) {
            startActivityForResult(new Intent(this, LivrariaActivity.class), REQUEST_CODE_LIVRARIA);
            return true;
        } else if (itemId == R.id.action_listas) {
            startActivityForResult(new Intent(this, ListasActivity.class), REQUEST_CODE_LISTAS);
            return true;
        } else if (itemId == R.id.action_recognize) {
            presenter.iniciarReconhecimentoMusica();
            return true;
        } else if (itemId == R.id.action_details) {
            mostrarDetalhesMusica();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void mostrarDetalhesMusica() {
        if (presenter != null) {
            Musicas musicaAtual = presenter.getMusicaAtual();
            if (musicaAtual != null) {
                String detalhes = "Título: " + musicaAtual.title + "\n"
                        + "Artista: " + musicaAtual.artist + "\n"
                        + "Álbum: " + musicaAtual.album + "\n"
                        + "Duração: " + formatarTempo(musicaAtual.duration);
                new AlertDialog.Builder(this)
                        .setTitle("Detalhes da Música")
                        .setMessage(detalhes)
                        .setPositiveButton("OK", null)
                        .show();
            } else {
                Toast.makeText(this, "Nenhuma música está a tocar.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //  Respostas de Activities e Permissoes

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && presenter != null) {
            if (requestCode == REQUEST_CODE_LIVRARIA) {
                int pos = data.getIntExtra("POSICAO_MUSICA_TOCAR", -1);
                if (pos != -1) presenter.tocarNaPosicao(pos);
            } else if (requestCode == REQUEST_CODE_LISTAS) {
                if ("TOCAR_LISTA".equals(data.getAction())) {
                    long listaId = data.getLongExtra("LISTA_ID", -1);
                    int pos = data.getIntExtra("POSICAO_MUSICA", 0);
                    if (listaId != -1) presenter.tocarListaNaPosicao(listaId, pos);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null) presenter.aoRetomar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (presenter != null) presenter.aoPausar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) presenter.aoDestruir();
        if (albumArtAnimator != null) albumArtAnimator.cancel();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean allGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            Manifest.permission.POST_NOTIFICATIONS.equals(permissions[i]) &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {

                        new AlertDialog.Builder(this)
                                .setTitle("Permissão Necessária")
                                .setMessage("A permissão de notificação é crucial para os controlos de mídia. Por favor, active-a nas configurações do aplicativo.")
                                .setPositiveButton("Abrir Configurações", (dialog, which) -> {
                                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
                                    startActivity(intent);
                                })
                                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                }
            }

            if (allGranted) {
                presenter.iniciarCarregamentoDeMusicas();
            } else {
                Toast.makeText(this, "Algumas funcionalidades podem não funcionar sem as permissões.", Toast.LENGTH_SHORT).show();
            }

        } else if (requestCode == REQUEST_CODE_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirTelaDeReconhecimento();
            } else {
                Toast.makeText(this, "Permissão de gravação negada. Não é possível reconhecer músicas.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
