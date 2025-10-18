package com.example.playerseba.view;

import com.example.playerseba.model.Musicas;
import java.util.List;

// Interface limpa, contendo apenas os métodos realmente utilizados pela MainActivity.
public interface ContratoView {
    void atualizarListaMusicas(List<Musicas> musicas);
    void musicaMudou(Musicas musica);
    void atualizarProgresso(long posicaoAtual, long duracao);
    void estadoReproducaoMudou(boolean isTocando);
    void modoEmbaralharMudou(boolean isAtivado);
    void modoRepetirMudou(int modoRepeticao); // 0 = OFF, 1 = ONE, 2 = ALL
    void mensagemErro(String mensagem);
    void pedirPermissaoGravacaoAudio();
    void mostrarProgresso(boolean mostrar);
}
