/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import android.app.Dialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.DialogAiCleanupBinding;
import com.fastshoppr.app.domain.model.AiAvailability;
import com.fastshoppr.app.domain.model.AiCleanupSource;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Sheet that shows before/after cleanup suggestions and requires explicit confirmation. */
public final class AiCleanupDialogFragment extends DialogFragment
    implements AiCleanupAdapter.Listener {

  public static final String TAG = "ai_cleanup";

  private DialogAiCleanupBinding binding;
  private ReviewViewModel viewModel;
  private AiCleanupAdapter adapter;
  private boolean cleanupRequested;

  public static AiCleanupDialogFragment newInstance() {
    return new AiCleanupDialogFragment();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    binding = DialogAiCleanupBinding.inflate(getLayoutInflater());
    viewModel = new ViewModelProvider(requireParentFragment()).get(ReviewViewModel.class);

    adapter = new AiCleanupAdapter(this);
    binding.suggestionList.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.suggestionList.setAdapter(adapter);
    binding.btnAcceptAll.setOnClickListener(v -> confirmAcceptAll());
    binding.btnCloseCleanup.setOnClickListener(v -> closeSheet());

    Dialog dialog = new Dialog(requireContext());
    dialog.setContentView(binding.getRoot());
    return dialog;
  }

  @Override
  public void onStart() {
    super.onStart();
    Window window = getDialog() == null ? null : getDialog().getWindow();
    if (window != null) {
      window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
    viewModel
        .getCleanupState()
        .observe(
            this,
            state -> {
              render(state);
              if (!cleanupRequested
                  && (state.getStatus() == ReviewViewModel.CleanupStatus.IDLE
                      || (state.getStatus() == ReviewViewModel.CleanupStatus.FINISHED
                          && state.getSuggestions().isEmpty()))) {
                cleanupRequested = true;
                viewModel.startCleanup();
              }
            });
  }

  private void render(ReviewViewModel.CleanupState state) {
    if (binding == null) {
      return;
    }
    adapter.submitList(state.getSuggestions());
    binding.cleanupProgress.setVisibility(state.isRunning() ? View.VISIBLE : View.GONE);
    binding.btnAcceptAll.setEnabled(!state.isRunning() && !state.getSuggestions().isEmpty());
    binding.cleanupStatus.setText(statusText(state));
    binding.cleanupNote.setVisibility(showRulesNote(state) ? View.VISIBLE : View.GONE);
    binding.cleanupNote.setText(R.string.ai_cleanup_rules_note);
  }

  private String statusText(ReviewViewModel.CleanupState state) {
    if (state.isRunning()) {
      return getString(R.string.ai_cleanup_working);
    }
    if (!state.isFinished()) {
      return getString(R.string.ai_cleanup_idle);
    }
    int count = state.getSuggestions().size();
    if (count == 0) {
      return getString(R.string.ai_cleanup_empty);
    }
    return getResources().getQuantityString(R.plurals.ai_cleanup_suggestion_count, count, count);
  }

  private boolean showRulesNote(ReviewViewModel.CleanupState state) {
    if (!state.isFinished()) {
      return false;
    }
    if (!(state.getAvailability() instanceof AiAvailability.Available)) {
      return true;
    }
    for (ReviewViewModel.CleanupSuggestion suggestion : state.getSuggestions()) {
      if (suggestion.getSource() == AiCleanupSource.RULES) {
        return true;
      }
    }
    return false;
  }

  private void confirmAcceptAll() {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.ai_cleanup_accept_all_title)
        .setMessage(R.string.ai_cleanup_accept_all_confirm)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(
            R.string.ai_cleanup_accept_all, (dialog, which) -> viewModel.acceptAllSuggestions())
        .show();
  }

  private void closeSheet() {
    if (viewModel != null) {
      viewModel.cancelCleanup();
    }
    dismiss();
  }

  @Override
  public void onAccept(ReviewViewModel.CleanupSuggestion suggestion) {
    viewModel.acceptSuggestion(suggestion);
  }

  @Override
  public void onEdit(ReviewViewModel.CleanupSuggestion suggestion) {
    AppCompatEditText input = new AppCompatEditText(requireContext());
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(200)});
    input.setText(suggestion.getSuggestedName());
    input.setSelection(input.getText() == null ? 0 : input.getText().length());
    int padding = (int) (16 * getResources().getDisplayMetrics().density);
    FrameLayout container = new FrameLayout(requireContext());
    container.setPadding(padding, padding / 2, padding, 0);
    container.addView(input);
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.ai_cleanup_edit_title)
        .setView(container)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(
            R.string.ai_cleanup_accept,
            (dialog, which) -> {
              String text = input.getText() == null ? "" : input.getText().toString();
              viewModel.acceptSuggestionAs(suggestion, text);
            })
        .show();
  }

  @Override
  public void onDismiss(ReviewViewModel.CleanupSuggestion suggestion) {
    viewModel.dismissSuggestion(suggestion);
  }

  @Override
  public void onCancel(@NonNull android.content.DialogInterface dialog) {
    if (viewModel != null) {
      viewModel.cancelCleanup();
    }
    super.onCancel(dialog);
  }

  @Override
  public void onDestroyView() {
    if (viewModel != null) {
      viewModel.getCleanupState().removeObservers(this);
    }
    if (binding != null) {
      binding.suggestionList.setAdapter(null);
    }
    binding = null;
    adapter = null;
    super.onDestroyView();
  }
}
