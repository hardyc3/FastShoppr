/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lookup;

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
import com.fastshoppr.app.databinding.FragmentLookupBinding;
import com.fastshoppr.app.domain.lookup.StoreLookupResult;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;

@AndroidEntryPoint
public final class LookupFragment extends Fragment {

  private FragmentLookupBinding binding;
  private LookupViewModel viewModel;
  private String lastMultipleItemId;

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
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(LookupViewModel.class);

    binding.lookupBackButton.setOnClickListener(
        v -> Navigation.findNavController(v).popBackStack());
    binding.lookupStoreButton.setOnClickListener(
        v -> Navigation.findNavController(v).navigate(R.id.lookup_to_walmart_store));
    binding.lookupStartButton.setOnClickListener(v -> viewModel.start(false));

    viewModel.getStoreContext().observe(getViewLifecycleOwner(), this::renderStoreContext);
    viewModel
        .getHasWalmart()
        .observe(
            getViewLifecycleOwner(),
            hasWalmart ->
                binding.lookupStoreButton.setVisibility(
                    Boolean.TRUE.equals(hasWalmart) ? View.VISIBLE : View.GONE));
    viewModel.getProgress().observe(getViewLifecycleOwner(), this::renderProgress);
    viewModel.loadContext();
  }

  private void renderStoreContext(String context) {
    if (context == null || context.isBlank()) {
      binding.lookupStoreContext.setText(R.string.lookup_no_store);
    } else {
      binding.lookupStoreContext.setText(getString(R.string.lookup_store_context, context));
    }
  }

  private void renderProgress(LookupViewModel.Progress progress) {
    if (progress == null) {
      return;
    }
    binding.lookupStatus.setText(progress.status == null ? "" : progress.status);
    binding.lookupLog.setText(viewModel.getLogText());
    int total = progress.total <= 0 ? 100 : progress.total;
    int done = Math.max(0, Math.min(progress.done, total));
    binding.lookupProgress.setMax(total);
    binding.lookupProgress.setProgressCompat(done, true);
    binding.lookupStartButton.setEnabled(!progress.running);

    if (progress.multiple != null
        && progress.multipleItemId != null
        && !progress.multipleItemId.equals(lastMultipleItemId)) {
      lastMultipleItemId = progress.multipleItemId;
      showCandidateDialog(progress.multipleItemId, progress.multiple);
    }
  }

  private void showCandidateDialog(String itemId, StoreLookupResult.Multiple multiple) {
    List<StoreLookupResult.Found> candidates = multiple.getCandidates();
    List<String> labels = new ArrayList<>();
    for (StoreLookupResult.Found c : candidates) {
      String name = c.getProductName();
      if (name == null || name.isBlank()) {
        name = getString(R.string.lookup_candidate_unknown);
      }
      String loc = c.getAisle() == null ? getString(R.string.no_aisle) : c.getAisle();
      if (c.getBay() != null && !c.getBay().isBlank()) {
        loc = loc + getString(R.string.bay_suffix, c.getBay());
      }
      labels.add(name + " (" + loc + ")");
    }
    labels.add(getString(R.string.set_aisle));
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.lookup_candidates_title)
        .setItems(
            labels.toArray(new String[0]),
            (dialog, which) -> {
              if (which >= 0 && which < candidates.size()) {
                viewModel.applyCandidate(itemId, candidates.get(which));
              } else {
                Navigation.findNavController(requireView()).popBackStack();
              }
            })
        .show();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
