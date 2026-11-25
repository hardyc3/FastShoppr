/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.listdetail;

import android.content.Context;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.ItemAddItemRowBinding;
import com.fastshoppr.app.databinding.ItemAttentionHeaderBinding;
import com.fastshoppr.app.databinding.ItemListItemRowBinding;
import com.fastshoppr.app.databinding.ItemStoreSectionHeaderBinding;
import com.fastshoppr.app.domain.model.ListItem;
import com.fastshoppr.app.domain.model.LookupSource;
import com.fastshoppr.app.domain.model.LookupStatus;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.domain.sort.AisleComparator;
import com.fastshoppr.app.ui.common.StoreFormatter;
import com.google.android.material.color.MaterialColors;
import java.util.Objects;

public final class ListDetailAdapter extends ListAdapter<ListDetailRow, RecyclerView.ViewHolder> {

  private static final int TYPE_STORE_HEADER = 0;
  private static final int TYPE_ITEM = 1;
  private static final int TYPE_ATTENTION = 2;
  private static final int TYPE_ADD = 3;

  public interface Listener {
    void onItemChecked(String itemId, boolean checked);

    void onItemEdit(ListItem item);

    void onItemDefer(ListItem item);

    void onItemSetAisle(ListItem item);

    void onItemNotRight(ListItem item);

    void onItemRefreshLookup(ListItem item);

    void onItemDelete(ListItem item);

    void onAddItem(String storeId, String text);

    void onDraftChanged(String storeId, String text);
  }

  private static final DiffUtil.ItemCallback<ListDetailRow> DIFF =
      new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(
            @NonNull ListDetailRow oldItem, @NonNull ListDetailRow newItem) {
          return key(oldItem).equals(key(newItem));
        }

        @Override
        public boolean areContentsTheSame(
            @NonNull ListDetailRow oldItem, @NonNull ListDetailRow newItem) {
          if (oldItem instanceof ListDetailRow.StoreHeader oldHeader
              && newItem instanceof ListDetailRow.StoreHeader newHeader) {
            return oldHeader.store().getId().equals(newHeader.store().getId())
                && oldHeader.itemCount() == newHeader.itemCount();
          }
          if (oldItem instanceof ListDetailRow.Item oldRow
              && newItem instanceof ListDetailRow.Item newRow) {
            return sameItem(oldRow.item(), newRow.item());
          }
          if (oldItem instanceof ListDetailRow.AddItem oldAdd
              && newItem instanceof ListDetailRow.AddItem newAdd) {
            return Objects.equals(oldAdd.draftText(), newAdd.draftText())
                && oldAdd.store().getId().equals(newAdd.store().getId())
                && oldAdd.generation() == newAdd.generation();
          }
          return true;
        }
      };

  private final Listener listener;

  public ListDetailAdapter(Listener listener) {
    super(DIFF);
    this.listener = listener;
  }

  @Override
  public int getItemViewType(int position) {
    ListDetailRow row = getItem(position);
    if (row instanceof ListDetailRow.StoreHeader) {
      return TYPE_STORE_HEADER;
    }
    if (row instanceof ListDetailRow.Item) {
      return TYPE_ITEM;
    }
    if (row instanceof ListDetailRow.AttentionHeader) {
      return TYPE_ATTENTION;
    }
    return TYPE_ADD;
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    return switch (viewType) {
      case TYPE_STORE_HEADER ->
          new StoreHeaderViewHolder(ItemStoreSectionHeaderBinding.inflate(inflater, parent, false));
      case TYPE_ITEM -> new ItemViewHolder(ItemListItemRowBinding.inflate(inflater, parent, false));
      case TYPE_ATTENTION ->
          new AttentionHeaderViewHolder(
              ItemAttentionHeaderBinding.inflate(inflater, parent, false));
      default -> new AddItemViewHolder(ItemAddItemRowBinding.inflate(inflater, parent, false));
    };
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    ListDetailRow row = getItem(position);
    if (holder instanceof StoreHeaderViewHolder storeHeaderViewHolder
        && row instanceof ListDetailRow.StoreHeader storeHeader) {
      storeHeaderViewHolder.bind(storeHeader);
    } else if (holder instanceof ItemViewHolder itemViewHolder
        && row instanceof ListDetailRow.Item item) {
      itemViewHolder.bind(item.item());
    } else if (holder instanceof AddItemViewHolder addItemViewHolder
        && row instanceof ListDetailRow.AddItem addItem) {
      addItemViewHolder.bind(addItem);
    }
  }

  public int positionOfStore(String storeId) {
    for (int index = 0; index < getItemCount(); index++) {
      ListDetailRow row = getItem(index);
      if (row instanceof ListDetailRow.StoreHeader header
          && header.store().getId().equals(storeId)) {
        return index;
      }
    }
    return RecyclerView.NO_POSITION;
  }

  private final class StoreHeaderViewHolder extends RecyclerView.ViewHolder {

    private final ItemStoreSectionHeaderBinding binding;

    private StoreHeaderViewHolder(ItemStoreSectionHeaderBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    private void bind(ListDetailRow.StoreHeader header) {
      Store store = header.store();
      binding.storeSectionName.setText(store.getName());
      String address = StoreFormatter.formatAddress(store);
      binding.storeSectionAddress.setText(address == null ? "" : address);
      binding.storeSectionAddress.setVisibility(address == null ? View.GONE : View.VISIBLE);
      binding.storeSectionCount.setText(
          binding
              .getRoot()
              .getContext()
              .getResources()
              .getQuantityString(
                  R.plurals.store_section_count, header.itemCount(), header.itemCount()));
      ViewCompat.setAccessibilityHeading(binding.getRoot(), true);
    }
  }

  private final class ItemViewHolder extends RecyclerView.ViewHolder {

    private final ItemListItemRowBinding binding;

    private ItemViewHolder(ItemListItemRowBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    private void bind(ListItem item) {
      binding.itemRawText.setText(item.getRawText());
      setStrikeThrough(binding.itemRawText, item.isChecked());

      boolean showDisplayName = !item.getDisplayName().equals(item.getRawText());
      binding.itemDisplayName.setText(item.getDisplayName());
      binding.itemDisplayName.setVisibility(showDisplayName ? View.VISIBLE : View.GONE);

      boolean hasMatch = item.getMatchedProductName() != null;
      binding.itemMatchedProduct.setText(
          hasMatch
              ? binding
                  .getRoot()
                  .getContext()
                  .getString(R.string.matched_product, item.getMatchedProductName())
              : "");
      binding.itemMatchedProduct.setVisibility(hasMatch ? View.VISIBLE : View.GONE);

      binding.itemAisleChip.setText(aisleText(binding.getRoot().getContext(), item));
      bindLookup(item);

      binding.itemCheckbox.setOnCheckedChangeListener(null);
      binding.itemCheckbox.setChecked(item.isChecked());
      binding.itemCheckbox.setContentDescription(
          binding
              .getRoot()
              .getContext()
              .getString(
                  item.isChecked()
                      ? R.string.item_unchecked_description
                      : R.string.item_checked_description,
                  item.getRawText()));
      binding.itemCheckbox.setOnCheckedChangeListener(
          (button, checked) -> listener.onItemChecked(item.getId(), checked));

      binding.getRoot().setOnClickListener(v -> listener.onItemEdit(item));
      binding.itemOverflow.setOnClickListener(v -> showItemMenu(v, item));
      binding.itemNotRight.setContentDescription(
          binding.getRoot().getContext().getString(R.string.a11y_not_right, item.getRawText()));
      binding.itemNotRight.setOnClickListener(v -> listener.onItemNotRight(item));
    }

    private void bindLookup(ListItem item) {
      Context context = binding.getRoot().getContext();
      LookupStatus status = item.getLookupStatus();
      String statusText = lookupStatusText(context, status);
      boolean showStatus = status != LookupStatus.PENDING || item.getLookupError() != null;
      binding.itemLookupStatus.setText(statusText);
      binding.itemLookupStatus.setVisibility(showStatus ? View.VISIBLE : View.GONE);

      String error = item.getLookupError();
      boolean stale = item.getLookupSource() == LookupSource.CACHE && error != null;
      // Show stale indicator subtly in status/error area; blocked/errors in error color.
      if (error != null && !error.trim().isEmpty()) {
        binding.itemLookupError.setText(
            stale ? context.getString(R.string.a11y_cached_error, error) : error);
        binding.itemLookupError.setVisibility(View.VISIBLE);
        binding.itemLookupError.setTextColor(
            stale
                ? MaterialColors.getColor(
                    binding.itemLookupError,
                    com.google.android.material.R.attr.colorOnSurfaceVariant)
                : MaterialColors.getColor(
                    binding.itemLookupError, androidx.appcompat.R.attr.colorError));
      } else {
        binding.itemLookupError.setVisibility(View.GONE);
      }

      boolean showNotRight =
          status == LookupStatus.FOUND
              || status == LookupStatus.NEEDS_REVIEW
              || status == LookupStatus.NOT_FOUND
              || status == LookupStatus.ERROR;
      binding.itemNotRight.setVisibility(showNotRight ? View.VISIBLE : View.GONE);
    }

    private static String lookupStatusText(Context context, LookupStatus status) {
      return switch (status) {
        case PENDING -> context.getString(R.string.lookup_status_pending);
        case QUEUED -> context.getString(R.string.lookup_status_queued);
        case IN_PROGRESS -> context.getString(R.string.lookup_status_in_progress);
        case FOUND -> context.getString(R.string.lookup_status_found);
        case NOT_FOUND -> context.getString(R.string.lookup_status_not_found);
        case NEEDS_REVIEW -> context.getString(R.string.lookup_status_needs_review);
        case ERROR -> context.getString(R.string.lookup_status_error);
      };
    }

    private void showItemMenu(View anchor, ListItem item) {
      PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
      popup.inflate(R.menu.menu_item_actions);
      popup.setOnMenuItemClickListener(
          menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.action_edit_item) {
              listener.onItemEdit(item);
              return true;
            }
            if (itemId == R.id.action_defer_item) {
              listener.onItemDefer(item);
              return true;
            }
            if (itemId == R.id.action_set_aisle) {
              listener.onItemSetAisle(item);
              return true;
            }
            if (itemId == R.id.action_refresh_lookup) {
              listener.onItemRefreshLookup(item);
              return true;
            }
            if (itemId == R.id.action_delete_item) {
              listener.onItemDelete(item);
              return true;
            }
            return false;
          });
      popup.show();
    }

    private void setStrikeThrough(TextView view, boolean strikeThrough) {
      int flags = view.getPaintFlags();
      if (strikeThrough) {
        flags |= Paint.STRIKE_THRU_TEXT_FLAG;
      } else {
        flags &= ~Paint.STRIKE_THRU_TEXT_FLAG;
      }
      view.setPaintFlags(flags);
    }
  }

  private final class AttentionHeaderViewHolder extends RecyclerView.ViewHolder {

    private AttentionHeaderViewHolder(ItemAttentionHeaderBinding binding) {
      super(binding.getRoot());
      ViewCompat.setAccessibilityHeading(binding.getRoot(), true);
    }
  }

  private final class AddItemViewHolder extends RecyclerView.ViewHolder {

    private final ItemAddItemRowBinding binding;
    private final TextWatcher watcher;
    private String currentStoreId;

    private AddItemViewHolder(ItemAddItemRowBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
      this.watcher =
          new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
              if (currentStoreId != null) {
                listener.onDraftChanged(currentStoreId, editable.toString());
              }
            }
          };
      binding.addItemButton.setOnClickListener(v -> addCurrentText());
      binding.addItemInput.setOnEditorActionListener(
          (v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                || (event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
              addCurrentText();
              return true;
            }
            return false;
          });
    }

    private void bind(ListDetailRow.AddItem addItem) {
      currentStoreId = addItem.store().getId();
      String hint =
          binding
              .getRoot()
              .getContext()
              .getString(R.string.add_item_hint, addItem.store().getName());
      binding.addItemInputLayout.setHint(hint);
      binding.addItemInput.setContentDescription(hint);
      binding.addItemInput.removeTextChangedListener(watcher);
      binding.addItemInput.setText(addItem.draftText());
      binding.addItemInput.addTextChangedListener(watcher);
    }

    private void addCurrentText() {
      if (currentStoreId == null) {
        return;
      }
      CharSequence text = binding.addItemInput.getText();
      listener.onAddItem(currentStoreId, text == null ? "" : text.toString());
    }
  }

  private static String aisleText(Context context, ListItem item) {
    String aisle = item.getAisle();
    String bay = item.getBay();
    boolean aisleUnresolved = AisleComparator.isUnresolved(aisle);
    boolean bayUnresolved = AisleComparator.isUnresolved(bay);
    if (aisleUnresolved && bayUnresolved) {
      return context.getString(R.string.no_aisle);
    }
    if (!aisleUnresolved && !bayUnresolved) {
      return context.getString(R.string.aisle_bay, aisle, bay);
    }
    if (!aisleUnresolved) {
      return context.getString(R.string.aisle_only, aisle);
    }
    return context.getString(R.string.bay_only, bay);
  }

  private static String key(ListDetailRow row) {
    if (row instanceof ListDetailRow.StoreHeader header) {
      return "header:" + header.store().getId();
    }
    if (row instanceof ListDetailRow.Item item) {
      return "item:" + item.item().getId();
    }
    if (row instanceof ListDetailRow.AttentionHeader attention) {
      return "attention:" + attention.storeId();
    }
    return "add:" + ((ListDetailRow.AddItem) row).store().getId();
  }

  private static boolean sameItem(ListItem left, ListItem right) {
    return left.getId().equals(right.getId())
        && left.getRawText().equals(right.getRawText())
        && left.getDisplayName().equals(right.getDisplayName())
        && Objects.equals(left.getMatchedProductName(), right.getMatchedProductName())
        && Objects.equals(left.getMatchedProductUrl(), right.getMatchedProductUrl())
        && Objects.equals(left.getAisle(), right.getAisle())
        && Objects.equals(left.getBay(), right.getBay())
        && Objects.equals(left.getMatchConfidence(), right.getMatchConfidence())
        && left.getLookupStatus() == right.getLookupStatus()
        && left.getLookupSource() == right.getLookupSource()
        && Objects.equals(left.getLookupError(), right.getLookupError())
        && left.isChecked() == right.isChecked()
        && left.getSortPosition() == right.getSortPosition();
  }
}
