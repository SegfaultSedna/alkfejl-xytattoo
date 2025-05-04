package com.example.xytattoo;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ViewHolder> {

    public interface ArtistSelectionListener {
        void onArtistSelected(Artist artist);
    }

    private final List<Artist> artists;
    private final ArtistSelectionListener listener;
    private int selectedPosition = -1;

    public ArtistAdapter(List<Artist> artists, ArtistSelectionListener listener) {
        this.artists = artists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_artist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Artist artist = artists.get(position);

        holder.name.setText(artist.getName());
        holder.specialty.setText(artist.getSpecialty());

        if (artist.getPhotoUrl() != null && !artist.getPhotoUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(artist.getPhotoUrl())
                    .placeholder(R.drawable.ic_person)
                    .into(holder.photo);
        }

        boolean isSelected = position == selectedPosition;
        holder.card.setChecked(isSelected);

        // Set different stroke width based on selection state
        holder.card.setStrokeWidth(isSelected ? 6 : 1);

        holder.card.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            if (previousPosition != -1) {
                notifyItemChanged(previousPosition);
            }
            notifyItemChanged(selectedPosition);

            listener.onArtistSelected(artist);
        });
    }

    @Override
    public int getItemCount() {
        return artists.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView photo;
        TextView name;
        TextView specialty;

        ViewHolder(View view) {
            super(view);
            card = view.findViewById(R.id.artistCard);
            photo = view.findViewById(R.id.artistImage);
            name = view.findViewById(R.id.artistName);
            specialty = view.findViewById(R.id.artistSpecialty);
        }
    }
}