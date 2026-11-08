/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.listdetail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentItemEditBinding;
import com.fastshoppr.app.ui.common.DatePickerUtil;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.StorePickerDialog;
import com.fastshoppr.app.ui.common.UiError;
import com.fastshoppr.app.ui.common.UiErrorText;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

@AndroidEntryPoint
public final class ItemEditFragment extends Fragment {

  private FragmentItemEditBinding binding;
  private ItemEditViewModel viewModel;
  private boolean fieldsInitialized;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentItemEditBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(ItemEditViewModel.class);
    fieldsInitialized = false;

    binding.storeInputButton.setOnClickListener(v -> showStorePicker());
    binding.targetDateButton.setOnClickListener(v -> showDatePicker());
    binding.saveItemButton.setOnClickListener(v -> save());
    binding.cancelItemButton.setOnClickListener(
        v -> Navigation.findNavController(v).popBackStack());

    viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    viewModel.getEvents().observe(getViewLifecycleOwner(), this::handleEvent);
  }

  private void render(ItemEditUiState state) {
    if (state.getError() != null) {
      binding.saveItemButton.setEnabled(false);
      if (state.getError() == UiError.ITEM_NOT_FOUND) {
        Snackbar.make(
                binding.getRoot(),
                getString(UiErrorText.stringRes(state.getError())),
                Snackbar.LENGTH_LONG)
            .show();
      }
      return;
    }
    if (state.isLoading()) {
      binding.saveItemButton.setEnabled(false);
      return;
    }

    binding.saveItemButton.setEnabled(true);
    binding.storeInputButton.setText(
        state.getSelectedStoreName() == null
            ? getString(R.string.field_store)
            : getString(
                R.string.field_value,
                getString(R.string.field_store),
                state.getSelectedStoreName()));
    binding.targetDateButton.setText(
        state.getTargetDate() == null
            ? getString(R.string.field_target_date)
            : getString(
                R.string.field_value,
                getString(R.string.field_target_date),
                formatDate(state.getTargetDate())));

    if (!fieldsInitialized) {
      binding.rawTextInput.setText(state.getRawText());
      binding.displayNameInput.setText(state.getDisplayName());
      binding.quantityInput.setText(state.getQuantity());
      binding.aisleInput.setText(state.getAisle());
      binding.bayInput.setText(state.getBay());
      fieldsInitialized = true;
    }
  }

  private void handleEvent(SingleEvent<ItemEditUiEvent> event) {
    ItemEditUiEvent content = event.getContentIfNotHandled();
    if (content == null) {
      return;
    }
    if (content instanceof ItemEditUiEvent.Saved) {
      Navigation.findNavController(requireView()).popBackStack();
    } else if (content instanceof ItemEditUiEvent.ShowError showError) {
      UiError error = showError.error();
      if (error == UiError.RAW_TEXT_REQUIRED || error == UiError.RAW_TEXT_TOO_LONG) {
        binding.rawTextInputLayout.setError(getString(UiErrorText.stringRes(error)));
      } else {
        Snackbar.make(
                binding.getRoot(), getString(UiErrorText.stringRes(error)), Snackbar.LENGTH_SHORT)
            .show();
      }
    }
  }

  private void showStorePicker() {
    ItemEditUiState state = viewModel.getState().getValue();
    if (state == null || state.getStores().isEmpty()) {
      return;
    }
    StorePickerDialog.show(
        requireContext(),
        state.getStores(),
        state.getSelectedStoreId(),
        store -> viewModel.selectStore(store.getId()));
  }

  private void showDatePicker() {
    ItemEditUiState state = viewModel.getState().getValue();
    if (state == null || state.getTargetDate() == null) {
      return;
    }
    DatePickerUtil.showDatePicker(this, state.getTargetDate(), date -> viewModel.selectDate(date));
  }

  private void save() {
    binding.rawTextInputLayout.setError(null);
    viewModel.save(
        textOf(binding.rawTextInput.getText()),
        textOf(binding.displayNameInput.getText()),
        textOf(binding.quantityInput.getText()),
        textOf(binding.aisleInput.getText()),
        textOf(binding.bayInput.getText()),
        getString(R.string.list_name_default));
  }

  private static String textOf(CharSequence text) {
    return text == null ? "" : text.toString();
  }

  private static String formatDate(LocalDate date) {
    return date.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()));
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
