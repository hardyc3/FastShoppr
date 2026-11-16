/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.calendar;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.fastshoppr.app.R;
import java.util.ArrayList;
import java.util.List;

public final class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.Holder> {

  public interface Listener {
    void onDayClick(CalendarDay day);
  }

  private final Listener listener;
  private List<CalendarDay> days = new ArrayList<>();

  public CalendarDayAdapter(Listener listener) { this.listener = listener; }

  public void submit(List<CalendarDay> days) {
    this.days = days == null ? new ArrayList<>() : days;
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
    return new Holder(v);
  }

  @Override
  public void onBindViewHolder(@NonNull Holder h, int position) {
    CalendarDay d = days.get(position);
    if (d.date == null || !d.inMonth) {
      h.text.setText("");
      h.dot.setVisibility(View.GONE);
      h.itemView.setEnabled(false);
      h.itemView.setAlpha(0f);
      h.itemView.setOnClickListener(null);
      h.itemView.setContentDescription(null);
      return;
    }
    h.itemView.setEnabled(true);
    h.itemView.setAlpha(d.inMonth ? 1f : 0.3f);
    h.text.setText(String.valueOf(d.date.getDayOfMonth()));
    h.text.setTypeface(null, d.isToday ? Typeface.BOLD : Typeface.NORMAL);
    boolean hasList = d.summary != null;
    h.dot.setVisibility(hasList ? View.VISIBLE : View.GONE);
    String desc = d.date.toString() + (d.isToday ? ", today" : "") + (hasList ? ", has shopping list" : ", no list");
    h.itemView.setContentDescription(desc);
    h.itemView.setOnClickListener(v -> listener.onDayClick(d));
  }

  @Override
  public int getItemCount() { return days.size(); }

  static final class Holder extends RecyclerView.ViewHolder {
    final TextView text;
    final View dot;
    Holder(@NonNull View itemView) {
      super(itemView);
      text = itemView.findViewById(R.id.textDay);
      dot = itemView.findViewById(R.id.dot);
    }
  }
}
