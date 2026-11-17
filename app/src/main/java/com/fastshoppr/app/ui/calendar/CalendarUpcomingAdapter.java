/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.fastshoppr.app.R;
import com.fastshoppr.app.domain.model.ShoppingListSummary;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class CalendarUpcomingAdapter
    extends RecyclerView.Adapter<CalendarUpcomingAdapter.Holder> {

  public interface Listener {
    void onOpen(ShoppingListSummary summary);
  }

  private final Listener listener;
  private List<ShoppingListSummary> items = new ArrayList<>();

  public CalendarUpcomingAdapter(Listener listener) {
    this.listener = listener;
  }

  public void submit(List<ShoppingListSummary> items) {
    int previous = this.items.size();
    this.items = items == null ? new ArrayList<>() : items;
    if (previous > 0) {
      notifyItemRangeRemoved(0, previous);
    }
    if (!this.items.isEmpty()) {
      notifyItemRangeInserted(0, this.items.size());
    }
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_calendar_upcoming, parent, false);
    return new Holder(v);
  }

  @Override
  public void onBindViewHolder(@NonNull Holder h, int position) {
    ShoppingListSummary s = items.get(position);
    h.title.setText(
        h.itemView
            .getContext()
            .getString(
                R.string.calendar_upcoming_row_title,
                s.getList().getName(),
                s.getList().getShoppingDate().format(DateTimeFormatter.ISO_LOCAL_DATE)));
    h.subtitle.setText(
        h.itemView
            .getContext()
            .getString(
                R.string.list_row_counts,
                h.itemView
                    .getResources()
                    .getQuantityString(R.plurals.item_count, s.getItemCount(), s.getItemCount()),
                h.itemView
                    .getResources()
                    .getQuantityString(
                        R.plurals.store_count, s.getStoreCount(), s.getStoreCount())));
    h.itemView.setOnClickListener(v -> listener.onOpen(s));
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  static final class Holder extends RecyclerView.ViewHolder {
    final TextView title;
    final TextView subtitle;

    Holder(@NonNull View itemView) {
      super(itemView);
      title = itemView.findViewById(R.id.textTitle);
      subtitle = itemView.findViewById(R.id.textSubtitle);
    }
  }
}
