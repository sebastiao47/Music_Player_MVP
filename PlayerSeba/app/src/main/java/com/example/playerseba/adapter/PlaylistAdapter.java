package com.example.playerseba.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.playerseba.R;
import com.example.playerseba.model.Playlist;
import com.example.playerseba.model.PlaylistWithSongCount;

import java.util.List;
import java.util.Locale;

// O PlaylistAdapter é o "ponte" que liga os dados das nossas playlists à lista que o usuário vê (RecyclerView).
// Ele é responsável por criar e preencher cada item da lista de playlists.
public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {
    // A lista de dados que o adapter vai mostrar.
    private List<PlaylistWithSongCount> playlists;
    // O "ouvinte" (listener) para comunicar os cliques do usuário de volta para a Activity.
    private final OnPlaylistClickListener listener;

    // Interface que define os métodos que a Activity deve implementar para reagir aos cliques.
    public interface OnPlaylistClickListener {
        // Chamado quando o usuário faz um clique normal num item da playlist.
        void onPlaylistClicked(Playlist playlist);
        // Chamado quando o usuário faz um clique longo (pressionar e segurar) num item.
        void onPlaylistLongClicked(Playlist playlist);
    }

    // Construtor do adapter.
    public PlaylistAdapter(List<PlaylistWithSongCount> playlists, OnPlaylistClickListener listener) {
        this.playlists = playlists;
        this.listener = listener;
    }

    // Chamado pela RecyclerView quando precisa criar uma nova "gaveta" (ViewHolder) para um item.
    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Carrega (infla) o layout XML do item da lista para criar uma nova View.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        // Cria e retorna o nosso ViewHolder com a view do item.
        return new PlaylistViewHolder(view);
    }

    // Chamado pela RecyclerView para ligar os dados de uma playlist específica (na posição 'position') a um ViewHolder.
    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        // Usa o método 'bind' do ViewHolder para preencher as informações na tela.
        holder.bind(playlists.get(position), listener);
    }

    // Retorna o número total de itens na nossa lista de dados.
    @Override
    public int getItemCount() {
        return playlists.size();
    }

    // Método público para actualizar a lista de playlists que o adapter está a mostrar.
    public void updatePlaylists(List<PlaylistWithSongCount> newPlaylists) {
        this.playlists = newPlaylists;
        // Avisa a RecyclerView que os dados mudaram para que ela possa redesenhar a lista.
        notifyDataSetChanged();
    }

    // Classe interna que representa cada item visual da lista (cada "gaveta").
    // Ela guarda as referências para os elementos de UI (TextViews, etc.) para acesso rápido.
    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        // Referências para os componentes visuais do item.
        TextView textName, textSongCount;

        // Construtor do ViewHolder, onde encontramos as views pelo seu ID.
        PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_playlist_name);
            textSongCount = itemView.findViewById(R.id.text_song_count);
        }

        // O método 'bind' preenche as views com os dados da playlist específica.
        void bind(final PlaylistWithSongCount playlistWithCount, final OnPlaylistClickListener listener) {
            // Pega o objecto Playlist de dentro do nosso objecto combinado.
            Playlist playlist = playlistWithCount.playlist;
            // Mostra o nome da playlist.
            textName.setText(playlist.name);
            
            // Formata o texto para mostrar a contagem de músicas (ex: "5 músicas").
            String songCountText = String.format(Locale.getDefault(), "%d músicas", playlistWithCount.songCount);
            // Mostra a contagem de músicas.
            textSongCount.setText(songCountText);

            // Configura o que acontece quando o usuário clica no item.
            itemView.setOnClickListener(v -> listener.onPlaylistClicked(playlist));
            // Configura o que acontece quando o usuário faz um clique longo no item.
            itemView.setOnLongClickListener(v -> {
                listener.onPlaylistLongClicked(playlist);
                return true; // Indica que o evento foi tratado.
            });
        }
    }
}
