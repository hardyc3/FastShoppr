/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.lookup;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.data.remote.storelocator.BranchPageExtractor;
import com.fastshoppr.app.databinding.FragmentBranchPickerBinding;
import com.fastshoppr.app.domain.model.BranchPage;
import com.fastshoppr.app.domain.model.StoreBrand;
import com.fastshoppr.app.domain.model.StoreLocatorPages;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.StoreBrandLabel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Arrays;
import java.util.List;

/** Shows a retailer's public locator and saves the branch the user confirms. */
@AndroidEntryPoint
public final class BranchPickerFragment extends Fragment {

  private FragmentBranchPickerBinding binding;
  private BranchPickerViewModel viewModel;
  private StoreBrand currentBrand;
  private StoreBrand loadedBrand;
  private String lastPageJson;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentBranchPickerBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(BranchPickerViewModel.class);

    binding.toolbar.setTitle(R.string.branch_picker_title);
    binding.toolbar.setNavigationContentDescription(R.string.action_back);
    binding.toolbar.setNavigationOnClickListener(
        v -> Navigation.findNavController(v).popBackStack());

    List<StoreBrand> brands = Arrays.asList(StoreBrandLabel.locatorBrands());
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            brands.stream().map(brand -> getString(StoreBrandLabel.labelRes(brand))).toList());
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    binding.branchBrandSpinner.setAdapter(adapter);
    BranchPickerViewModel.PickerState initial = viewModel.getState().getValue();
    if (initial != null) {
      currentBrand = initial.getBrand();
      int initialPosition = brands.indexOf(currentBrand);
      if (initialPosition >= 0) {
        binding.branchBrandSpinner.setSelection(initialPosition);
      }
    }
    binding.branchBrandSpinner.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(
              android.widget.AdapterView<?> parent, View itemView, int position, long id) {
            if (position >= 0 && position < brands.size()) {
              viewModel.selectBrand(brands.get(position));
            }
          }

          @Override
          public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

    configureWebView();
    binding.btnUseBranch.setOnClickListener(v -> requestPreview());
    binding.btnForgetBranch.setOnClickListener(v -> viewModel.forgetSelectedBranch());

    viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    viewModel.getEvents().observe(getViewLifecycleOwner(), this::handleEvent);
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void configureWebView() {
    WebView webView = binding.branchPickerWebView;
    WebSettings settings = webView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(false);
    settings.setAllowContentAccess(false);
    settings.setAllowFileAccessFromFileURLs(false);
    settings.setAllowUniversalAccessFromFileURLs(false);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    settings.setSupportMultipleWindows(false);
    settings.setUserAgentString(settings.getUserAgentString() + " FastShoppr/1.0");
    CookieManager.getInstance().setAcceptCookie(true);
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

    webView.setWebViewClient(
        new WebViewClient() {
          @Override
          public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url =
                request == null || request.getUrl() == null ? null : request.getUrl().toString();
            if (StoreLocatorPages.isAllowedNavigation(currentBrand, url)) {
              return false;
            }
            showMessage(R.string.branch_picker_navigation_blocked);
            return true;
          }

          @Override
          public void onPageFinished(WebView view, String url) {
            lastPageJson = null;
            if (url == null || !StoreLocatorPages.isAllowedNavigation(currentBrand, url)) {
              return;
            }
            view.evaluateJavascript(BranchPageExtractor.SCRIPT, value -> lastPageJson = value);
          }

          @Override
          public void onReceivedHttpError(
              WebView view, WebResourceRequest request, android.webkit.WebResourceResponse error) {
            if (request != null && request.isForMainFrame() && error != null) {
              int code = error.getStatusCode();
              if (code == 403 || code == 429 || code == 503) {
                showMessage(R.string.branch_picker_blocked);
              }
            }
          }

          @Override
          public void onReceivedError(
              WebView view, WebResourceRequest request, WebResourceError error) {
            if (request != null && request.isForMainFrame()) {
              showMessage(R.string.branch_picker_load_failed);
            }
          }
        });
  }

  private void requestPreview() {
    if (lastPageJson == null) {
      binding.branchPickerWebView.evaluateJavascript(
          BranchPageExtractor.SCRIPT, value -> viewModel.previewCurrentPage(value));
      return;
    }
    viewModel.previewCurrentPage(lastPageJson);
  }

  private void render(BranchPickerViewModel.PickerState state) {
    if (binding == null) {
      return;
    }
    StoreBrand brand = state.getBrand();
    if (currentBrand != brand) {
      currentBrand = brand;
      lastPageJson = null;
    }
    int position = Arrays.asList(StoreBrandLabel.locatorBrands()).indexOf(brand);
    if (position >= 0 && binding.branchBrandSpinner.getSelectedItemPosition() != position) {
      binding.branchBrandSpinner.setSelection(position);
    }
    binding.branchPickerStatus.setText(
        state.getSelected() == null
            ? getString(R.string.branch_picker_unset)
            : getString(R.string.branch_picker_current, state.getSelected().getLabel()));
    if (loadedBrand != brand) {
      loadedBrand = brand;
      binding.branchPickerWebView.loadUrl(state.getLocatorUrl());
    }
  }

  private void handleEvent(SingleEvent<BranchPickerViewModel.PickerEvent> event) {
    BranchPickerViewModel.PickerEvent content = event.getContentIfNotHandled();
    if (content == null || binding == null) {
      return;
    }
    if (content instanceof BranchPickerViewModel.PickerEvent.BranchPreview preview) {
      showConfirmDialog(preview.page());
    } else if (content instanceof BranchPickerViewModel.PickerEvent.SaveCompleted) {
      Navigation.findNavController(binding.getRoot()).popBackStack();
    } else if (content instanceof BranchPickerViewModel.PickerEvent.BranchRemoved) {
      showMessage(R.string.settings_store_removed_message);
    } else if (content instanceof BranchPickerViewModel.PickerEvent.ShowMessage message) {
      int messageRes =
          switch (message.message()) {
            case NO_BRANCH_ON_PAGE -> R.string.branch_picker_no_branch;
            case CHALLENGE_PAGE -> R.string.branch_picker_blocked;
            case EXTRACTION_FAILED -> R.string.branch_picker_extract_failed;
            case UNSUPPORTED_BRAND -> R.string.branch_picker_unsupported;
          };
      showMessage(messageRes);
    }
  }

  private void showConfirmDialog(BranchPage page) {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.branch_picker_confirm_title)
        .setMessage(BranchPickerViewModel.describe(page))
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.save, (dialog, which) -> viewModel.savePendingBranch())
        .show();
  }

  private void showMessage(int messageRes) {
    if (binding != null) {
      Snackbar.make(binding.getRoot(), messageRes, Snackbar.LENGTH_LONG).show();
    }
  }

  @Override
  public void onDestroyView() {
    if (binding != null) {
      WebView webView = binding.branchPickerWebView;
      webView.stopLoading();
      webView.loadUrl("about:blank");
      webView.clearHistory();
      webView.removeAllViews();
      webView.destroy();
    }
    binding = null;
    super.onDestroyView();
  }
}
