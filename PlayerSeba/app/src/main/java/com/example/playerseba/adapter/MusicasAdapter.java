package com.example.playerseba.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.playerseba.model.Musicas;
import com.example.playerseba.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MusicasAdapter extends RecyclerView.Adapter<MusicasAdapter.MusicasViewHolder> implements Filterable {
    private List<Musicas> listaMusicas;
    private List<Musicas> listaCompleta;
    private final OnMusicaInteractionListener listener;

    // Interface corrigida para refletir a ação exata do usuário
    public interface OnMusicaInteractionListener {
        void onMusicaClicada(Musicas musica, int posicao);
        void onAddIconClicked(Musicas musica);
    }

    public MusicasAdapter(List<Musicas> musicas, OnMusicaInteractionListener listener) {
        this.listaMusicas = new ArrayList<>(musicas);
        this.listaCompleta = new ArrayList<>(musicas);
        this.listener = listener;
    }

    public void updateMusicas(List<Musicas> novasMusicas) {
        listaMusicas.clear();
        listaCompleta.clear();
        listaMusicas.addAll(novasMusicas);
        listaCompleta.addAll(novasMusicas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MusicasViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new MusicasViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicasViewHolder holder, int position) {
        holder.bind(listaMusicas.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return listaMusicas != null ? listaMusicas.size() : 0;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Musicas> filtrada = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtrada.addAll(listaCompleta);
                } else {
                    String padrao = constraint.toString().toLowerCase(Locale.getDefault()).trim();
                    for (Musicas m : listaCompleta) {
                        if (m.title.toLowerCase(Locale.getDefault()).contains(padrao) ||
                                m.artist.toLowerCase(Locale.getDefault()).contains(padrao)) {
                            filtrada.add(m);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filtrada;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                listaMusicas.clear();
                listaMusicas.addAll((List<Musicas>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    static class MusicasViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textArtist, textDuration;
        ImageButton btnAddToList;

        MusicasViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_song_title);
            textArtist = itemView.findViewById(R.id.text_song_artist);
            textDuration = itemView.findViewById(R.id.text_song_duration);
            btnAddToList = itemView.findViewById(R.id.btn_add_to_playlist);
        }

        void bind(final Musicas musica, final OnMusicaInteractionListener listener) {
            textTitle.setText(musica.title);
            textArtist.setText(musica.artist);
            textDuration.setText(formatTime(musica.duration));

            // Clique no item inteiro para tocar a música
            itemView.setOnClickListener(v -> listener.onMusicaClicada(musica, getAdapterPosition()));
            
            // Clique APENAS no ícone para adicionar à playlist, como solicitado.
            btnAddToList.setOnClickListener(v -> listener.onAddIconClicked(musica));
        }

        private String formatTime(long millis) {
            long min = (millis / 1000) / 60;
            long sec = (millis / 1000) % 60;
            return String.format(Locale.getDefault(), "%02d:%02d", min, sec);
        }
    }
}
