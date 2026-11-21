/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentSettingsBinding;
import com.fastshoppr.app.domain.model.NearbyStore;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.ui.common.StoreBrandLabel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AndroidEntryPoint
public final class SettingsFragment extends Fragment {

  private static final String KEY_PENDING_LOCATION_BRAND = "pending_location_brand";

  private FragmentSettingsBinding binding;
  private SettingsViewModel viewModel;
  private ActivityResultLauncher<String> locationPermissionLauncher;
  private StoreBrand pendingLocationBrand;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      String brandName = savedInstanceState.getString(KEY_PENDING_LOCATION_BRAND);
      if (brandName != null) {
        try {
          pendingLocationBrand = StoreBrand.valueOf(brandName);
        } catch (IllegalArgumentException unknownBrand) {
          pendingLocationBrand = null;
        }
      }
    }
    locationPermissionLauncher =
        registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
              if (Boolean.TRUE.equals(granted)) {
                if (viewModel != null && pendingLocationBrand != null) {
                  viewModel.findNearbyUsingLocation(pendingLocationBrand);
                }
              } else if (!shouldShowRequestPermissionRationale(
                  Manifest.permission.ACCESS_COARSE_LOCATION)) {
                showLocationSettingsMessage();
              } else if (viewModel != null) {
                viewModel.locationPermissionDenied();
              }
              pendingLocationBrand = null;
            });
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentSettingsBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

    binding.toolbar.setTitle(R.string.settings_title);
    binding.toolbar.setNavigationContentDescription(R.string.action_back);
    binding.toolbar.setNavigationOnClickListener(
        v -> Navigation.findNavController(v).popBackStack());

    binding.switchAiCleanup.setOnCheckedChangeListener(
        (buttonView, isChecked) -> viewModel.setAiCleanupEnabled(isChecked));
    binding.btnDownloadModel.setOnClickListener(v -> viewModel.downloadModel());
    binding.btnOpenSchedule.setOnClickListener(
        v -> Navigation.findNavController(v).navigate(R.id.action_settings_to_schedule));
    binding.btnDeleteAllData.setOnClickListener(v -> confirmDeleteAllData());

    binding.btnSaveZip.setOnClickListener(
        v ->
            viewModel.savePostalCode(
                binding.storeZipInput.getText() == null
                    ? ""
                    : binding.storeZipInput.getText().toString()));
    binding.btnFindNearbyStores.setOnClickListener(
        v -> viewModel.findNearbyUsingPostalCode(currentBrand()));
    binding.btnUseLocation.setOnClickListener(v -> requestLocation(currentBrand()));
    binding.btnEnterStoreManually.setOnClickListener(v -> showManualStoreDialog(currentBrand()));
    binding.btnForgetSavedStore.setOnClickListener(this::showForgetStoreDialog);

    viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    viewModel
        .getNearbyStoresEvent()
        .observe(
            getViewLifecycleOwner(),
            event -> {
              SettingsViewModel.StoreSearchResults results = event.getContentIfNotHandled();
              if (results != null && binding != null) {
                showStorePicker(results.getBrand(), results.getStores());
              }
            });
    viewModel
        .getStoreMessageEvent()
        .observe(
            getViewLifecycleOwner(),
            event -> {
              SettingsViewModel.StoreMessage message = event.getContentIfNotHandled();
              if (message != null && binding != null) {
                showStoreMessage(message);
              }
            });
    viewModel
        .getDeleteEvent()
        .observe(
            getViewLifecycleOwner(),
            event -> {
              Boolean success = event.getContentIfNotHandled();
              if (success == null || binding == null) {
                return;
              }
              if (success) {
                Navigation.findNavController(binding.getRoot()).popBackStack();
              } else {
                Snackbar.make(
                        binding.getRoot(),
                        R.string.settings_delete_all_failed,
                        Snackbar.LENGTH_LONG)
                    .show();
              }
            });
    viewModel.load();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (pendingLocationBrand != null) {
      outState.putString(KEY_PENDING_LOCATION_BRAND, pendingLocationBrand.name());
    }
  }

  private void showLocationSettingsMessage() {
    Snackbar.make(binding.getRoot(), R.string.settings_store_location_denied, Snackbar.LENGTH_LONG)
        .setAction(
            R.string.settings_store_open_settings,
            view ->
                startActivity(
                    new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + requireContext().getPackageName()))))
        .show();
  }

  private void requestLocation(StoreBrand brand) {
    pendingLocationBrand = brand;
    if (ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
      viewModel.findNearbyUsingLocation(brand);
      return;
    }
    new MaterialAlertDialogBuilder(requireContext())
        .setMessage(R.string.settings_store_permission_rationale)
        .setNegativeButton(R.string.cancel, (dialog, which) -> pendingLocationBrand = null)
        .setPositiveButton(
            R.string.ok,
            (dialog, which) ->
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION))
        .show();
  }

  private StoreBrand currentBrand() {
    int position = binding.storeBrandSpinner.getSelectedItemPosition();
    StoreBrand[] brands = StoreBrandLabel.locatorBrands();
    if (position < 0 || position >= brands.length) {
      return StoreBrand.WALMART;
    }
    return brands[position];
  }

  private void showStorePicker(StoreBrand brand, List<NearbyStore> stores) {
    String[] labels = new String[stores.size()];
    for (int index = 0; index < stores.size(); index++) {
      labels[index] = stores.get(index).getLabel();
    }
    int[] selection = {-1};
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(StoreBrandLabel.labelRes(brand))
        .setSingleChoiceItems(labels, -1, (dialog, which) -> selection[0] = which)
        .setNeutralButton(
            R.string.settings_store_manual, (dialog, which) -> showManualStoreDialog(brand))
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(
            R.string.save,
            (dialog, which) -> {
              if (selection[0] >= 0 && selection[0] < stores.size()) {
                viewModel.saveStore(stores.get(selection[0]));
              } else {
                Snackbar.make(
                        binding.getRoot(), R.string.settings_store_pick_one, Snackbar.LENGTH_LONG)
                    .show();
              }
            })
        .show();
  }

  private void showManualStoreDialog(StoreBrand brand) {
    LinearLayout layout = new LinearLayout(requireContext());
    layout.setOrientation(LinearLayout.VERTICAL);
    int padding = (int) (16 * getResources().getDisplayMetrics().density);
    layout.setPadding(padding, 0, padding, 0);

    EditText nameInput = new EditText(requireContext());
    nameInput.setHint(R.string.settings_store_manual_name_hint);
    nameInput.setSingleLine(true);
    layout.addView(nameInput);

    EditText addressInput = new EditText(requireContext());
    addressInput.setHint(R.string.settings_store_manual_address_hint);
    addressInput.setSingleLine(true);
    layout.addView(addressInput);

    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.settings_store_manual_title)
        .setView(layout)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(
            R.string.save,
            (dialog, which) ->
                viewModel.saveManualStore(
                    brand,
                    nameInput.getText() == null ? "" : nameInput.getText().toString(),
                    addressInput.getText() == null ? null : addressInput.getText().toString()))
        .show();
  }

  private void showForgetStoreDialog(View view) {
    SettingsViewModel.SettingsUiState state = viewModel.getState().getValue();
    if (state == null || state.getSelectedStores().isEmpty()) {
      Snackbar.make(view, R.string.settings_store_no_saved, Snackbar.LENGTH_LONG).show();
      return;
    }
    Map<StoreBrand, NearbyStore> selected = state.getSelectedStores();
    List<StoreBrand> brands = new ArrayList<>();
    List<String> labels = new ArrayList<>();
    for (StoreBrand brand : StoreBrandLabel.locatorBrands()) {
      NearbyStore store = selected.get(brand);
      if (store != null) {
        brands.add(brand);
        labels.add(getString(StoreBrandLabel.labelRes(brand)) + ": " + store.getLabel());
      }
    }
    int[] selection = {-1};
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.settings_store_forget_title)
        .setSingleChoiceItems(
            labels.toArray(new String[0]), -1, (dialog, which) -> selection[0] = which)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(
            R.string.settings_store_remove,
            (dialog, which) -> {
              if (selection[0] >= 0 && selection[0] < brands.size()) {
                viewModel.forgetStore(brands.get(selection[0]));
              }
            })
        .show();
  }

  private void showStoreMessage(SettingsViewModel.StoreMessage message) {
    int messageRes =
        switch (message) {
          case ZIP_REQUIRED -> R.string.settings_store_zip_required;
          case MANUAL_NAME_REQUIRED -> R.string.settings_store_manual_invalid;
          case LOCATION_UNAVAILABLE -> R.string.settings_store_location_unavailable;
          case LOCATION_DENIED -> R.string.settings_store_location_denied;
          case SEARCH_FAILED -> R.string.settings_store_search_failed;
          case BLOCKED -> R.string.settings_store_blocked;
          case NO_RESULTS -> R.string.settings_store_no_results;
          case ZIP_SAVED -> R.string.settings_store_saved_message;
          case STORE_SAVED -> R.string.settings_store_saved_message;
          case STORE_REMOVED -> R.string.settings_store_removed_message;
        };
    Snackbar.make(binding.getRoot(), messageRes, Snackbar.LENGTH_LONG).show();
  }

  private void confirmDeleteAllData() {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.settings_delete_all_title)
        .setMessage(R.string.settings_delete_all_message)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(
            R.string.settings_delete_all_confirm, (dialog, which) -> viewModel.deleteAllData())
        .show();
  }

  private void render(SettingsViewModel.SettingsUiState state) {
    if (binding == null) {
      return;
    }
    if (binding.switchAiCleanup.isChecked() != state.isAiCleanupEnabled()) {
      binding.switchAiCleanup.setOnCheckedChangeListener(null);
      binding.switchAiCleanup.setChecked(state.isAiCleanupEnabled());
      binding.switchAiCleanup.setOnCheckedChangeListener(
          (buttonView, isChecked) -> viewModel.setAiCleanupEnabled(isChecked));
    }
    binding.aiStatus.setText(statusTextRes(state));
    if (state.getAiStatus() == SettingsViewModel.AiStatus.DOWNLOADING) {
      binding.aiStatus.setText(
          getString(
              R.string.settings_ai_downloading_bytes,
              state.getDownloadedBytes() / (1024L * 1024L)));
    }
    binding.btnDownloadModel.setVisibility(
        state.getAiStatus() == SettingsViewModel.AiStatus.DOWNLOADABLE ? View.VISIBLE : View.GONE);

    if (state.isStoreSearching()) {
      binding.storeStatus.setText(R.string.settings_store_searching);
      binding.storeStatus.setVisibility(View.VISIBLE);
    } else {
      binding.storeStatus.setVisibility(View.GONE);
    }
    if (state.getPostalCode() != null
        && binding.storeZipInput.getText() != null
        && binding.storeZipInput.getText().toString().isEmpty()) {
      binding.storeZipInput.setText(state.getPostalCode());
    }
    binding.storeSummary.setText(formatStoreSummary(state));
  }

  private String formatStoreSummary(SettingsViewModel.SettingsUiState state) {
    List<String> lines = new ArrayList<>();
    if (state.getPostalCode() == null || state.getPostalCode().isBlank()) {
      lines.add(getString(R.string.settings_store_zip_unset));
    } else {
      lines.add(getString(R.string.settings_store_summary, state.getPostalCode()));
    }
    if (state.getSelectedStores().isEmpty()) {
      lines.add(getString(R.string.settings_store_no_saved));
      return String.join("\n", lines);
    }
    for (StoreBrand brand : StoreBrandLabel.locatorBrands()) {
      NearbyStore store = state.getSelectedStores().get(brand);
      if (store != null) {
        lines.add(
            getString(
                R.string.settings_store_saved_format,
                getString(StoreBrandLabel.labelRes(brand)),
                store.getLabel()));
      }
    }
    return String.join("\n", lines);
  }

  private int statusTextRes(SettingsViewModel.SettingsUiState state) {
    return switch (state.getAiStatus()) {
      case CHECKING -> R.string.settings_ai_checking;
      case AVAILABLE -> R.string.settings_ai_available;
      case DOWNLOADABLE -> R.string.settings_ai_downloadable;
      case DOWNLOADING -> R.string.settings_ai_downloading;
      case QUOTA_EXCEEDED -> R.string.settings_ai_quota;
      case DOWNLOAD_FAILED -> R.string.settings_ai_download_failed;
      case UNAVAILABLE -> R.string.settings_ai_unavailable;
    };
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
