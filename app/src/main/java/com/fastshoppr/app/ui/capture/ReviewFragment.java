/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentReviewBinding;
import com.fastshoppr.app.domain.model.ItemSource;
import com.fastshoppr.app.domain.model.Store;
import com.fastshoppr.app.ui.common.DatePickerUtil;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.StorePickerDialog;
import com.fastshoppr.app.ui.common.UiErrorText;
import com.fastshoppr.app.ui.common.UiMessageText;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

@AndroidEntryPoint
public final class ReviewFragment extends Fragment {

  private FragmentReviewBinding binding;
  private ReviewViewModel viewModel;
  private ReviewAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentReviewBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(ReviewViewModel.class);

    binding.toolbar.setTitle(R.string.capture_review_toolbar_title);
    binding.toolbar.setNavigationContentDescription(R.string.action_back);
    binding.toolbar.setNavigationOnClickListener(
        v -> Navigation.findNavController(v).popBackStack());

    adapter = new ReviewAdapter(this, viewModel);
    binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.recyclerView.setAdapter(adapter);

    binding.checkSelectAll.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          if (isChecked) {
            viewModel.selectAll();
          } else {
            viewModel.deselectAll();
          }
        });
    binding.btnDeleteSelected.setOnClickListener(v -> viewModel.deleteSelected());
    binding.btnMergeSelected.setOnClickListener(v -> viewModel.mergeSelected());
    binding.btnAddLine.setOnClickListener(v -> viewModel.addLine(""));
    binding.btnSplit.setOnClickListener(v -> onSplitClicked());
    binding.btnSave.setOnClickListener(v -> viewModel.saveSelected());
    binding.btnDiscard.setOnClickListener(v -> viewModel.discard());

    viewModel.getEvents().observe(getViewLifecycleOwner(), this::handleEvent);
    viewModel.loadCapture();
  }

  private void onSplitClicked() {
    List<ReviewViewModel.ReviewLine> lines = viewModel.getLines();
    for (int index = 0; index < lines.size(); index++) {
      if (lines.get(index).selected) {
        viewModel.splitLine(index, cursorForPosition(index));
        return;
      }
    }
  }

  private int cursorForPosition(int position) {
    RecyclerView.ViewHolder holder =
        binding.recyclerView.findViewHolderForAdapterPosition(position);
    if (holder instanceof ReviewAdapter.ViewHolder viewHolder) {
      EditText field = viewHolder.textField;
      if (field.isFocused()) {
        return field.getSelectionStart();
      }
    }
    return -1;
  }

  private void handleEvent(SingleEvent<ReviewViewModel.ReviewUiEvent> event) {
    ReviewViewModel.ReviewUiEvent content = event.getContentIfNotHandled();
    if (content == null || binding == null) {
      return;
    }
    if (content instanceof ReviewViewModel.ReviewUiEvent.CaptureLoaded) {
      onCaptureLoaded();
    } else if (content instanceof ReviewViewModel.ReviewUiEvent.LoadFailed loadFailed) {
      Snackbar.make(
              binding.getRoot(), UiErrorText.stringRes(loadFailed.error()), Snackbar.LENGTH_SHORT)
          .show();
      Navigation.findNavController(requireView()).popBackStack();
    } else if (content instanceof ReviewViewModel.ReviewUiEvent.LinesChanged) {
      adapter.notifyDataSetChanged();
      syncSelectAll();
    } else if (content instanceof ReviewViewModel.ReviewUiEvent.SelectionChanged) {
      adapter.notifyDataSetChanged();
      syncSelectAll();
    } else if (content instanceof ReviewViewModel.ReviewUiEvent.SaveCompleted
        || content instanceof ReviewViewModel.ReviewUiEvent.Discarded) {
      navigateBackAfterReview();
    } else if (content instanceof ReviewViewModel.ReviewUiEvent.ShowError error) {
      Snackbar.make(binding.getRoot(), UiErrorText.stringRes(error.error()), Snackbar.LENGTH_SHORT)
          .show();
    } else if (content instanceof ReviewViewModel.ReviewUiEvent.ShowMessage message) {
      Snackbar.make(
              binding.getRoot(), UiMessageText.stringRes(message.message()), Snackbar.LENGTH_SHORT)
          .show();
    }
  }

  private void onCaptureLoaded() {
    if (binding == null) {
      return;
    }
    adapter.notifyDataSetChanged();
    syncSelectAll();
    ItemSource source = viewModel.getSource();
    boolean photoSource = source == ItemSource.CAMERA_OCR || source == ItemSource.GALLERY_OCR;
    binding.reviewNote.setVisibility(photoSource ? View.VISIBLE : View.GONE);
  }

  private void syncSelectAll() {
    if (binding == null) {
      return;
    }
    binding.checkSelectAll.setVisibility(viewModel.getLines().isEmpty() ? View.GONE : View.VISIBLE);
    boolean allSelected =
        !viewModel.getLines().isEmpty()
            && viewModel.getSelectedCount() == viewModel.getLines().size();
    if (binding.checkSelectAll.isChecked() != allSelected) {
      binding.checkSelectAll.setOnCheckedChangeListener(null);
      binding.checkSelectAll.setChecked(allSelected);
      binding.checkSelectAll.setOnCheckedChangeListener(
          (buttonView, isChecked) -> {
            if (isChecked) {
              viewModel.selectAll();
            } else {
              viewModel.deselectAll();
            }
          });
    }
  }

  private void navigateBackAfterReview() {
    Navigation.findNavController(requireView()).popBackStack();
  }

  void showStorePicker(int position) {
    ReviewViewModel.ReviewLine line = lineAt(position);
    if (line == null) {
      return;
    }
    StorePickerDialog.show(
        requireContext(),
        viewModel.getStores(),
        line.storeId,
        store -> viewModel.setLineStore(position, store.getId()));
  }

  void showDatePicker(int position) {
    ReviewViewModel.ReviewLine line = lineAt(position);
    if (line == null) {
      return;
    }
    DatePickerUtil.showDatePicker(
        this, parseDate(line.targetDate), date -> viewModel.setLineDate(position, date));
  }

  String storeLabel(@Nullable String storeId) {
    for (Store store : viewModel.getStores()) {
      if (store.getId().equals(storeId)) {
        return store.getName();
      }
    }
    return getString(R.string.select_store);
  }

  String dateLabel(@Nullable String value) {
    if (value == null || value.isBlank()) {
      return getString(R.string.field_target_date);
    }
    try {
      LocalDate date = LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
      return date.format(
          DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()));
    } catch (RuntimeException exception) {
      return getString(R.string.field_target_date);
    }
  }

  private ReviewViewModel.ReviewLine lineAt(int position) {
    List<ReviewViewModel.ReviewLine> lines = viewModel.getLines();
    if (position < 0 || position >= lines.size()) {
      return null;
    }
    return lines.get(position);
  }

  private LocalDate parseDate(@Nullable String value) {
    if (value != null && !value.isBlank()) {
      try {
        return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
      } catch (RuntimeException exception) {
        return LocalDate.now();
      }
    }
    return LocalDate.now();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
    adapter = null;
  }

  public static final class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private final ReviewFragment fragment;
    private final ReviewViewModel viewModel;

    ReviewAdapter(ReviewFragment fragment, ReviewViewModel viewModel) {
      this.fragment = fragment;
      this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view =
          LayoutInflater.from(parent.getContext())
              .inflate(R.layout.item_review_line, parent, false);
      return new ViewHolder(view, viewModel);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
      ReviewViewModel.ReviewLine line = viewModel.getLines().get(position);

      holder.checkBox.setOnCheckedChangeListener(null);
      holder.checkBox.setChecked(line.selected);
      holder.checkBox.setOnCheckedChangeListener(
          (buttonView, isChecked) -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
              viewModel.setLineSelected(adapterPosition, isChecked);
            }
          });

      holder.bindText(line);
      bindConfidence(holder, line);

      holder.storeButton.setText(fragment.storeLabel(line.storeId));
      holder.storeButton.setOnClickListener(
          v -> fragment.showStorePicker(holder.getBindingAdapterPosition()));
      holder.dateButton.setText(fragment.dateLabel(line.targetDate));
      holder.dateButton.setOnClickListener(
          v -> fragment.showDatePicker(holder.getBindingAdapterPosition()));
    }

    private void bindConfidence(ViewHolder holder, ReviewViewModel.ReviewLine line) {
      if (line.manual) {
        holder.confidenceText.setVisibility(View.GONE);
        return;
      }
      holder.confidenceText.setVisibility(View.VISIBLE);
      if (viewModel.isLowConfidence(line)) {
        holder.confidenceText.setText(R.string.low_confidence);
        holder.confidenceText.setTextColor(
            resolveColor(holder.context, androidx.appcompat.R.attr.colorError));
      } else {
        holder.confidenceText.setTextColor(
            resolveColor(holder.context, com.google.android.material.R.attr.colorOnSurface));
        holder.confidenceText.setText(
            String.format(Locale.getDefault(), "%.0f%%", line.confidence * 100f));
      }
    }

    private static int resolveColor(android.content.Context context, int attr) {
      TypedValue value = new TypedValue();
      if (context.getTheme().resolveAttribute(attr, value, true) && value.resourceId != 0) {
        return context.getColor(value.resourceId);
      }
      return value.data;
    }

    @Override
    public int getItemCount() {
      return viewModel.getLines().size();
    }

    public static final class ViewHolder extends RecyclerView.ViewHolder {

      final android.content.Context context;
      final android.widget.CheckBox checkBox;
      final EditText textField;
      final android.widget.TextView confidenceText;
      final com.google.android.material.button.MaterialButton storeButton;
      final com.google.android.material.button.MaterialButton dateButton;
      private final ReviewViewModel viewModel;
      @Nullable private TextWatcher watcher;

      ViewHolder(@NonNull View view, ReviewViewModel viewModel) {
        super(view);
        this.viewModel = viewModel;
        context = view.getContext();
        checkBox = view.findViewById(R.id.checkBox);
        textField = view.findViewById(R.id.textField);
        confidenceText = view.findViewById(R.id.confidenceText);
        storeButton = view.findViewById(R.id.storeButton);
        dateButton = view.findViewById(R.id.dateButton);
      }

      void bindText(ReviewViewModel.ReviewLine line) {
        if (watcher != null) {
          textField.removeTextChangedListener(watcher);
        }
        textField.setText(line.text);
        watcher =
            new TextWatcher() {
              @Override
              public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

              @Override
              public void onTextChanged(CharSequence s, int start, int before, int count) {}

              @Override
              public void afterTextChanged(Editable s) {
                int adapterPosition = getBindingAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) {
                  return;
                }
                List<ReviewViewModel.ReviewLine> lines = viewModel.getLines();
                if (adapterPosition < lines.size()) {
                  lines.get(adapterPosition).text = s.toString();
                }
              }
            };
        textField.addTextChangedListener(watcher);
      }
    }
  }
}
