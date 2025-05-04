package com.example.xytattoo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.ViewHolder> {

    public interface DateSelectionListener {
        void onDateSelected(LocalDate date);
    }

    private final List<LocalDate> dates;
    private final DateSelectionListener listener;
    private int selectedPosition = -1;

    public DateAdapter(List<LocalDate> dates, DateSelectionListener listener) {
        this.dates = dates;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_date, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocalDate date = dates.get(position);

        holder.dayOfWeek.setText(date.getDayOfWeek().toString().substring(0, 3));
        holder.date.setText(date.format(DateTimeFormatter.ofPattern("MMM dd")));

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

            listener.onDateSelected(date);
        });
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView dayOfWeek;
        TextView date;

        ViewHolder(View view) {
            super(view);
            card = view.findViewById(R.id.dateCard);
            dayOfWeek = view.findViewById(R.id.dayOfWeek);
            date = view.findViewById(R.id.dateText);
        }
    }
}