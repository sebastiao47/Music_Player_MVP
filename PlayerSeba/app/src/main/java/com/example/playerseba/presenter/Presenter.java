package com.example.playerseba.presenter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.example.playerseba.Repositorio.MusicRepository;
import com.example.playerseba.Servico.MusicService;
import com.example.playerseba.model.Musicas;
import com.example.playerseba.view.ContratoView;

import java.util.ArrayList;
import java.util.List;

/*
  O Presenter é o cérebro da nossa arquitetura MVP (Model-View-Presenter).
  Ele age como um intermediário: recebe os eventos da View (a Activity), processa a lógica de negócio
  (como pedir ao repositório para carregar músicas), e depois manda a View se actualizar.
 */
public class Presenter implements MusicService.MusicServiceCallback {
    private static final String TAG = "Presenter";

    private ContratoView view;
    private final Context context;
    private final MusicRepository repositorio;
    private MusicService musicService;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Constantes para os modos de repetição, para evitar números "mágicos" no código.
    public static final int REPEAT_OFF = 0;
    public static final int REPEAT_ONE = 1;
    public static final int REPEAT_ALL = 2;

    private boolean servicoConectado = false;
    private List<Musicas> listaDeMusicasPrincipal = new ArrayList<>();

    // "Ouvinte" para a conexão com o MusicService.
    private final ServiceConnection conexaoServico = new ServiceConnection() {
        // Chamado quando a conexão com o serviço é estabelecida.
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            servicoConectado = true;
            musicService.setCallback(Presenter.this); // O Presenter passa a ouvir os eventos do Serviço.
            sincronizarUICompleta();
            Log.d(TAG, "Serviço de música conectado.");
        }

        // Chamado quando a conexão com o serviço é perdida.
        @Override
        public void onServiceDisconnected(ComponentName name) {
            servicoConectado = false;
            musicService = null;
            Log.d(TAG, "Serviço de música desconectado.");
        }
    };

    // Construtor do Presenter.
    public Presenter(ContratoView view, Context context) {
        this.view = view;
        this.context = context.getApplicationContext();
        this.repositorio = new MusicRepository(this.context);

        // Inicia e liga-se ao MusicService.
        Intent serviceIntent = new Intent(this.context, MusicService.class);
        this.context.bindService(serviceIntent, conexaoServico, Context.BIND_AUTO_CREATE);
    }

    /*
      Inicia o processo de procurar por músicas no dispositivo e salvá-las no banco de dados.
     */
    public void iniciarCarregamentoDeMusicas() {
        if (view != null) view.mostrarProgresso(true);

        repositorio.escanearESalvarMusicas(new MusicRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Depois de escanear, carrega todas as músicas do banco para a UI.
                carregarTodasAsMusicasDoBanco();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Falha ao escanear, carregando do banco mesmo assim.", e);
                carregarTodasAsMusicasDoBanco(); // Tenta carregar do banco mesmo se o scan falhar.
            }
        });
    }

    /*
      Pede ao repositório para buscar todas as músicas do banco de dados.
     */
    private void carregarTodasAsMusicasDoBanco() {
        repositorio.obterTodasAsMusicas(new MusicRepository.RepositoryCallback<List<Musicas>>() {
            @Override
            public void onSuccess(List<Musicas> musicas) {
                listaDeMusicasPrincipal = musicas;

                if (view != null) {
                    view.atualizarListaMusicas(musicas);
                    view.mostrarProgresso(false);
                }

                // Se o serviço já está conectado e não tem uma playlist activa, define a lista principal.
                if (servicoConectado && musicService != null && !musicService.hasActivePlaylist()) {
                    musicService.setPlaylist(listaDeMusicasPrincipal);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (view != null) {
                    view.mensagemErro("Erro ao carregar músicas do banco.");
                    view.mostrarProgresso(false);
                }
            }
        });
    }

    /*
      Manda o serviço tocar a música numa posição específica da lista principal.
     */
    public void tocarNaPosicao(int posicao) {
        if (!servicoConectado || musicService == null || listaDeMusicasPrincipal == null || listaDeMusicasPrincipal.isEmpty()) {
            if (view != null) view.mensagemErro("Serviço de música não disponível ou lista vazia.");
            return;
        }

        musicService.setPlaylist(listaDeMusicasPrincipal);
        musicService.playSongAtIndex(posicao);
    }

    /*
      Carrega as músicas de uma playlist específica e manda o serviço tocar.
     */
    public void tocarListaNaPosicao(long listaId, int posicao) {
        repositorio.obterMusicasDaLista(listaId, new MusicRepository.RepositoryCallback<List<Musicas>>() {
            @Override
            public void onSuccess(List<Musicas> musicas) {
                if (musicas == null || musicas.isEmpty()) {
                    if (view != null) view.mensagemErro("Playlist vazia.");
                    return;
                }
                if (!servicoConectado || musicService == null) {
                    if (view != null) view.mensagemErro("Serviço não está pronto.");
                    return;
                }
                if (view != null) view.atualizarListaMusicas(musicas);
                musicService.setPlaylist(musicas);
                musicService.playSongAtIndex(posicao);
            }

            @Override
            public void onFailure(Exception e) {
                if (view != null) view.mensagemErro("Erro ao carregar a playlist.");
            }
        });
    }

    // Comandos do Player (delegados para o MusicService)

    public void tocarOuPausar() {
        if (servicoConectado && musicService != null) {
            musicService.playOrPause();
        }
    }

    public void proximaMusica() {
        if (servicoConectado && musicService != null) {
            musicService.nextSong();
        }
    }

    public void musicaAnterior() {
        if (servicoConectado && musicService != null) {
            musicService.prevSong();
        }
    }

    public void avancarPara(long posicao) {
        if (servicoConectado && musicService != null) {
            musicService.seekTo(posicao);
        }
    }

    public void alternarModoShuffle() {
        if (servicoConectado && musicService != null) {
            musicService.toggleShuffleMode();
        }
    }

    public void alternarModoRepeticao() {
        if (servicoConectado && musicService != null) {
            musicService.toggleRepeatMode();
        }
    }

    public boolean estaTocando() {
        return servicoConectado && musicService != null && musicService.isPlaying();
    }

    /*
      Retorna a música que está a tocar actualmente no serviço.
     */
    public Musicas getMusicaAtual() {
        if (servicoConectado && musicService != null) {
            return musicService.getCurrentSong();
        }
        return null;
    }

    /*
      Pede à View para iniciar o fluxo de permissão para gravar áudio.
     */
    public void iniciarReconhecimentoMusica() {
        if (view != null) {
            view.pedirPermissaoGravacaoAudio();
        }
    }

    /*
      Força a sincronização de toda a UI com o estado atual do MusicService.
      Útil quando a Activity volta ao primeiro plano.
     */
    private void sincronizarUICompleta() {
        if (servicoConectado && musicService != null && view != null) {
            view.musicaMudou(musicService.getCurrentSong());
            view.estadoReproducaoMudou(musicService.isPlaying());

            Musicas song = musicService.getCurrentSong();
            if (song != null) {
                view.atualizarProgresso(musicService.getCurrentPosition(), musicService.getDuration());
            } else {
                view.atualizarProgresso(0, 0);
            }

            view.modoEmbaralharMudou(musicService.isShuffleEnabled());
            view.modoRepetirMudou(musicService.getRepeatMode());

            if (musicService.hasActivePlaylist()) {
                view.atualizarListaMusicas(musicService.getCurrentPlaylist());
            }
        }
    }

    // Métodos de ciclo de vida chamados pela Activity

    public void aoRetomar() {
        if (servicoConectado && musicService != null) {
            musicService.setCallback(this);
            sincronizarUICompleta();
        }
    }

    public void aoPausar() {
        if (servicoConectado && musicService != null) {
            musicService.setCallback(null); // Evita memory leaks
        }
    }

    public void aoDestruir() {
        handler.removeCallbacksAndMessages(null);
        if (servicoConectado) {
            try {
                context.unbindService(conexaoServico);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao desvincular serviço", e);
            }
            servicoConectado = false;
        }
        musicService = null;
        view = null; // Evita memory leaks
    }

    //  Callbacks do MusicService (respostas do Serviço)

    @Override
    public void onMusicChanged(Musicas musica) {
        if (view != null) view.musicaMudou(musica);
    }

    @Override
    public void onProgressUpdate(long pos, long dur) {
        if (view != null) view.atualizarProgresso(pos, dur);
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (view != null) view.estadoReproducaoMudou(isPlaying);
    }

    @Override
    public void onShuffleModeChanged(boolean isEnabled) {
        if (view != null) view.modoEmbaralharMudou(isEnabled);
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        if (view != null) {
            view.modoRepetirMudou(repeatMode);
        }
    }

    @Override
    public void onError(String mensagem) {
        if (view != null) view.mensagemErro(mensagem);
    }
}
