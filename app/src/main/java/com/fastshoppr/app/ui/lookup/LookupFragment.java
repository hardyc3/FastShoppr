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
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.fastshoppr.app.work.OrganizingCounts;
import com.fastshoppr.app.work.OrganizingProgressStore;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.EnumSet;

@AndroidEntryPoint
public final class LookupFragment extends Fragment {

  private static final String KEY_PROMPTED_BRANDS = "prompted_branch_brands";

  private FragmentLookupBinding binding;
  private LookupViewModel viewModel;
  private final EnumSet<StoreBrand> promptedBrands = EnumSet.noneOf(StoreBrand.class);
  private boolean autoStarted;
  private boolean branchPickerVisible;
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
    if (savedInstanceState != null) {
      String[] restored = savedInstanceState.getStringArray(KEY_PROMPTED_BRANDS);
      if (restored != null) {
        for (String name : restored) {
          try {
            promptedBrands.add(StoreBrand.valueOf(name));
          } catch (IllegalArgumentException ignored) {
            // Stale brand name; ignore it.
          }
        }
      }
    }
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
        .getMissingBranch()
        .observe(
            getViewLifecycleOwner(),
            brand -> {
              if (brand != null && !autoStarted) {
                openBranchPicker((StoreBrand) brand, false);
              }
            });
    viewModel
        .getBranchCheckDone()
        .observe(
            getViewLifecycleOwner(),
            done -> {
              if (!Boolean.TRUE.equals(done)
                  || autoStarted
                  || branchPickerVisible
                  || !Boolean.TRUE.equals(viewModel.getBranchesReady().getValue())) {
                return;
              }
              autoStarted = true;
              if (!viewModel.isRunning()) {
                startOrganizingWithPermission();
              }
            });

    viewModel.loadContext();
  }

  @Override
  public void onResume() {
    super.onResume();
    branchPickerVisible = false;
    viewModel.loadContext();
  }

  private void startOrganizingWithPermission() {
    if (!Boolean.TRUE.equals(viewModel.getBranchCheckDone().getValue())) {
      viewModel.loadContext();
      return;
    }
    StoreBrand missing = (StoreBrand) viewModel.getMissingBranch().getValue();
    if (missing != null) {
      openBranchPicker(missing, true);
      return;
    }
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

  private void openBranchPicker(StoreBrand brand, boolean force) {
    if (brand == null || branchPickerVisible || (!force && promptedBrands.contains(brand))) {
      return;
    }
    promptedBrands.add(brand);
    branchPickerVisible = true;
    Navigation.findNavController(requireView())
        .navigate(R.id.lookup_to_branch_picker, NavArguments.forBranch(brand));
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
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    String[] brands = new String[promptedBrands.size()];
    int index = 0;
    for (StoreBrand brand : promptedBrands) {
      brands[index++] = brand.name();
    }
    outState.putStringArray(KEY_PROMPTED_BRANDS, brands);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
