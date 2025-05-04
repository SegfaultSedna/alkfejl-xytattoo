package com.example.xytattoo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {

    public interface TimeSlotSelectionListener {
        void onTimeSlotSelected(TimeSlot timeSlot);
    }

    private final List<TimeSlot> timeSlots;
    private final TimeSlotSelectionListener listener;
    private int selectedPosition = -1;

    public TimeSlotAdapter(List<TimeSlot> timeSlots, TimeSlotSelectionListener listener) {
        this.timeSlots = timeSlots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time_slot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimeSlot slot = timeSlots.get(position);

        String time = slot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                slot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));

        holder.time.setText(time);

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

            listener.onTimeSlotSelected(slot);
        });
    }

    @Override
    public int getItemCount() {
        return timeSlots.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView time;

        ViewHolder(View view) {
            super(view);
            card = view.findViewById(R.id.timeCard);
            time = view.findViewById(R.id.timeText);
        }
    }
}