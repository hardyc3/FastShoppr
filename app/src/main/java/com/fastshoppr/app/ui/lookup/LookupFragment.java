/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lookup;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentLookupBinding;
import com.fastshoppr.app.work.OrganizingCounts;
import com.fastshoppr.app.work.OrganizingProgressStore;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public final class LookupFragment extends Fragment {

  private FragmentLookupBinding binding;
  private LookupViewModel viewModel;
  private boolean autoStarted;
  private boolean waitingStoreVerification;
  private ActivityResultLauncher<String> notificationPermissionLauncher;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentLookupBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    notificationPermissionLauncher =
        registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
              if (Boolean.FALSE.equals(granted)) {
                Snackbar.make(
                        requireView(),
                        R.string.lookup_permission_denied_message,
                        Snackbar.LENGTH_LONG)
                    .show();
              }
              // proceed to organize regardless; scheduler handles denied permission by queuing
              // Walmart
              viewModel.organizeNow();
            });
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(LookupViewModel.class);

    binding.lookupBackButton.setOnClickListener(
        v -> Navigation.findNavController(v).popBackStack());
    binding.lookupStartButton.setOnClickListener(v -> startOrganizingWithPermission());
    binding.lookupRetryFailedButton.setOnClickListener(v -> viewModel.retryFailed());
    binding.lookupCancelButton.setOnClickListener(v -> viewModel.cancel());

    viewModel.getStoreContext().observe(getViewLifecycleOwner(), this::renderStoreContext);
    viewModel.getProgress().observe(getViewLifecycleOwner(), this::renderProgress);
    viewModel.getCounts().observe(getViewLifecycleOwner(), this::renderCounts);
    viewModel.getPerStoreDetails().observe(getViewLifecycleOwner(), this::renderDetails);
    viewModel
        .getWalmartVerified()
        .observe(
            getViewLifecycleOwner(),
            verified -> {
              if (waitingStoreVerification && Boolean.TRUE.equals(verified)) {
                waitingStoreVerification = false;
                if (!autoStarted && !viewModel.isRunning()) {
                  autoStarted = true;
                  startOrganizingWithPermission();
                }
              }
            });

    viewModel.loadContext();
    maybeAutoOrganize();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.loadContext();
    if (waitingStoreVerification) {
      // returning from store picker; observe will trigger if verified
    }
  }

  private void maybeAutoOrganize() {
    if (autoStarted) {
      return;
    }
    viewModel
        .getHasWalmart()
        .observe(
            getViewLifecycleOwner(),
            hasWalmart -> {
              if (autoStarted) return;
              Boolean verified = viewModel.getWalmartVerified().getValue();
              if (Boolean.TRUE.equals(hasWalmart) && !Boolean.TRUE.equals(verified)) {
                waitingStoreVerification = true;
                autoStarted = true; // prevent duplicate nav
                Navigation.findNavController(requireView()).navigate(R.id.lookup_to_walmart_store);
                return;
              }
              if (!viewModel.isRunning()) {
                autoStarted = true;
                startOrganizingWithPermission();
              }
            });
  }

  private void startOrganizingWithPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
              requireContext(), Manifest.permission.POST_NOTIFICATIONS)
          != PackageManager.PERMISSION_GRANTED) {
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
          new MaterialAlertDialogBuilder(requireContext())
              .setMessage(R.string.lookup_permission_rationale)
              .setNegativeButton(android.R.string.cancel, (d, w) -> viewModel.organizeNow())
              .setPositiveButton(
                  R.string.ok,
                  (d, w) ->
                      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
              .show();
          return;
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        return;
      }
    }
    viewModel.organizeNow();
  }

  private void renderStoreContext(String context) {
    if (context == null || context.isBlank()) {
      binding.lookupStoreContext.setText(R.string.lookup_no_store);
    } else {
      binding.lookupStoreContext.setText(getString(R.string.lookup_store_context, context));
    }
  }

  private void renderProgress(OrganizingProgressStore.Progress progress) {
    boolean running = progress != null && progress.running;
    binding.lookupStartButton.setEnabled(!running);
    binding.lookupCancelButton.setEnabled(running);
    if (progress == null) {
      binding.lookupStatus.setText("");
      binding.lookupProgress.setIndeterminate(false);
      binding.lookupProgress.setProgressCompat(0, false);
      return;
    }
    String status = progress.status == null ? "" : progress.status;
    if (progress.walmartNeedsForeground) {
      status = status + "\n" + getString(R.string.lookup_walmart_queued_foreground);
    }
    binding.lookupStatus.setText(status);
    int total = progress.total <= 0 ? 0 : progress.total;
    int done = Math.max(0, Math.min(progress.done, total));
    binding.lookupProgress.setIndeterminate(total <= 0 && progress.running);
    if (total > 0) {
      binding.lookupProgress.setMax(total);
      binding.lookupProgress.setProgressCompat(done, true);
    }
  }

  private void renderCounts(OrganizingCounts counts) {
    // counts shown with per-store details
  }

  private void renderDetails(String details) {
    if (details == null || details.isBlank()) {
      OrganizingCounts c = viewModel.getCounts().getValue();
      binding.lookupLog.setText(
          c == null ? "" : getString(R.string.lookup_completed_counts, c.summary()));
      return;
    }
    binding.lookupLog.setText(details);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
