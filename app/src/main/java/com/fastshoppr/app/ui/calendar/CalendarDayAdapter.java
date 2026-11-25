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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.fastshoppr.app.R;
import com.fastshoppr.app.domain.model.ShoppingListSummary;
import java.util.Objects;

public final class CalendarDayAdapter extends ListAdapter<CalendarDay, CalendarDayAdapter.Holder> {

  static final DiffUtil.ItemCallback<CalendarDay> DIFF_CALLBACK =
      new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull CalendarDay oldItem, @NonNull CalendarDay newItem) {
          if (oldItem.date == null || newItem.date == null) {
            return oldItem == newItem;
          }
          return oldItem.date.equals(newItem.date);
        }

        @Override
        public boolean areContentsTheSame(
            @NonNull CalendarDay oldItem, @NonNull CalendarDay newItem) {
          return oldItem.inMonth == newItem.inMonth
              && oldItem.isToday == newItem.isToday
              && sameSummary(oldItem.summary, newItem.summary);
        }
      };

  public interface Listener {
    void onDayClick(CalendarDay day);
  }

  private final Listener listener;

  public CalendarDayAdapter(Listener listener) {
    super(DIFF_CALLBACK);
    this.listener = listener;
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
    return new Holder(v);
  }

  @Override
  public void onBindViewHolder(@NonNull Holder h, int position) {
    CalendarDay d = getItem(position);
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
    String desc =
        d.date.toString()
            + (d.isToday ? h.itemView.getContext().getString(R.string.calendar_day_today) : "")
            + (hasList
                ? h.itemView.getContext().getString(R.string.calendar_day_has_list)
                : h.itemView.getContext().getString(R.string.calendar_day_no_list));
    h.itemView.setContentDescription(desc);
    h.itemView.setOnClickListener(v -> listener.onDayClick(d));
  }

  private static boolean sameSummary(
      ShoppingListSummary oldSummary, ShoppingListSummary newSummary) {
    if (oldSummary == newSummary) {
      return true;
    }
    if (oldSummary == null || newSummary == null) {
      return false;
    }
    return oldSummary.getItemCount() == newSummary.getItemCount()
        && oldSummary.getStoreCount() == newSummary.getStoreCount()
        && Objects.equals(oldSummary.getList().getId(), newSummary.getList().getId())
        && Objects.equals(oldSummary.getList().getName(), newSummary.getList().getName())
        && Objects.equals(
            oldSummary.getList().getShoppingDate(), newSummary.getList().getShoppingDate());
  }

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
