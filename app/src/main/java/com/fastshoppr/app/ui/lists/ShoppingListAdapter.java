/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lists;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.ItemListRowBinding;
import com.fastshoppr.app.databinding.ItemSectionHeaderBinding;
import com.fastshoppr.app.domain.model.ShoppingListSummary;
import com.fastshoppr.app.ui.common.DateText;

public final class ShoppingListAdapter extends ListAdapter<ListRow, RecyclerView.ViewHolder> {

  private static final int TYPE_HEADER = 0;
  private static final int TYPE_ENTRY = 1;

  private static final DiffUtil.ItemCallback<ListRow> DIFF =
      new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull ListRow oldItem, @NonNull ListRow newItem) {
          if (oldItem instanceof ListRow.SectionHeader oldHeader
              && newItem instanceof ListRow.SectionHeader newHeader) {
            return oldHeader.section().equals(newHeader.section());
          }
          if (oldItem instanceof ListRow.ListEntry oldEntry
              && newItem instanceof ListRow.ListEntry newEntry) {
            return oldEntry
                .summary()
                .getList()
                .getId()
                .equals(newEntry.summary().getList().getId());
          }
          return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ListRow oldItem, @NonNull ListRow newItem) {
          if (oldItem instanceof ListRow.SectionHeader oldHeader
              && newItem instanceof ListRow.SectionHeader newHeader) {
            return oldHeader.section().equals(newHeader.section());
          }
          if (oldItem instanceof ListRow.ListEntry oldEntry
              && newItem instanceof ListRow.ListEntry newEntry) {
            ShoppingListSummary oldSummary = oldEntry.summary();
            ShoppingListSummary newSummary = newEntry.summary();
            return oldSummary.getList().getId().equals(newSummary.getList().getId())
                && oldSummary.getList().getName().equals(newSummary.getList().getName())
                && oldSummary
                    .getList()
                    .getShoppingDate()
                    .equals(newSummary.getList().getShoppingDate())
                && oldSummary.getItemCount() == newSummary.getItemCount()
                && oldSummary.getStoreCount() == newSummary.getStoreCount();
          }
          return false;
        }
      };

  public interface Listener {
    void onOpenList(String listId);

    void onArchiveList(String listId);
  }

  private final Listener listener;

  public ShoppingListAdapter(Listener listener) {
    super(DIFF);
    this.listener = listener;
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position) instanceof ListRow.SectionHeader ? TYPE_HEADER : TYPE_ENTRY;
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    if (viewType == TYPE_HEADER) {
      return new HeaderViewHolder(ItemSectionHeaderBinding.inflate(inflater, parent, false));
    }
    return new EntryViewHolder(ItemListRowBinding.inflate(inflater, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    ListRow row = getItem(position);
    if (holder instanceof HeaderViewHolder && row instanceof ListRow.SectionHeader header) {
      ((HeaderViewHolder) holder).bind(header);
    } else if (holder instanceof EntryViewHolder && row instanceof ListRow.ListEntry entry) {
      ((EntryViewHolder) holder).bind(entry.summary());
    }
  }

  private final class HeaderViewHolder extends RecyclerView.ViewHolder {

    private final ItemSectionHeaderBinding binding;

    private HeaderViewHolder(ItemSectionHeaderBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    private void bind(ListRow.SectionHeader header) {
      int titleRes =
          switch (header.section()) {
            case UPCOMING -> R.string.upcoming_lists;
            case PAST -> R.string.past_lists;
            case ARCHIVED -> R.string.archived_lists;
          };
      binding.getRoot().setText(titleRes);
      ViewCompat.setAccessibilityHeading(binding.getRoot(), true);
    }
  }

  private static String formatDate(java.time.LocalDate date) {
    return DateText.formatMedium(date);
  }

  private final class EntryViewHolder extends RecyclerView.ViewHolder {

    private final ItemListRowBinding binding;

    private EntryViewHolder(ItemListRowBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    private void bind(ShoppingListSummary summary) {
      binding.listRowDate.setText(formatDate(summary.getList().getShoppingDate()));
      binding.listRowName.setText(summary.getList().getName());
      android.content.Context context = binding.getRoot().getContext();
      String itemText =
          context
              .getResources()
              .getQuantityString(
                  R.plurals.item_count, summary.getItemCount(), summary.getItemCount());
      String storeText =
          context
              .getResources()
              .getQuantityString(
                  R.plurals.store_count, summary.getStoreCount(), summary.getStoreCount());
      binding.listRowCounts.setText(
          context.getString(R.string.list_row_counts, itemText, storeText));
      binding
          .getRoot()
          .setContentDescription(
              context.getString(
                  R.string.a11y_open_list_card,
                  summary.getList().getName(),
                  formatDate(summary.getList().getShoppingDate()),
                  binding.listRowCounts.getText()));
      binding.getRoot().setOnClickListener(v -> listener.onOpenList(summary.getList().getId()));
      binding.listRowOverflow.setOnClickListener(
          v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.inflate(R.menu.menu_list_actions);
            popup.setOnMenuItemClickListener(
                item -> {
                  if (item.getItemId() == R.id.action_archive_list) {
                    listener.onArchiveList(summary.getList().getId());
                    return true;
                  }
                  return false;
                });
            popup.show();
          });
    }
  }
}
