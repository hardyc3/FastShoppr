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
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.data.remote.walmart.WalmartWebViewLookupService;
import com.fastshoppr.app.databinding.FragmentWalmartStoreBinding;
import com.google.android.material.snackbar.Snackbar;

/**
 * Assisted Walmart store selection (D-022). Walmart applies a default store (usually by IP) and
 * Walmart exposes no reliable programmatic way to pin a store, so the user picks their store here
 * once; the WebView cookie jar keeps that choice for subsequent assisted lookups until the user
 * taps "Forget store".
 */
public final class WalmartStoreFragment extends Fragment {

  private FragmentWalmartStoreBinding binding;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentWalmartStoreBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    WebView webView = binding.walmartStoreWebView;
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
          public boolean shouldOverrideUrlLoading(
              WebView view, android.webkit.WebResourceRequest navRequest) {
            String url = navRequest == null ? null : navRequest.getUrl().toString();
            return !WalmartWebViewLookupService.isAllowedNavigation(url);
          }
        });
    webView.loadUrl("https://www.walmart.com/");

    binding.walmartStoreDoneButton.setOnClickListener(
        v -> Navigation.findNavController(v).popBackStack());
    binding.walmartStoreForgetButton.setOnClickListener(v -> forgetStore(v));
  }

  private void forgetStore(View anchor) {
    CookieManager.getInstance().removeAllCookies(null);
    CookieManager.getInstance().flush();
    WebStorage.getInstance().deleteAllData();
    Snackbar.make(anchor, R.string.store_session_forgotten, Snackbar.LENGTH_SHORT).show();
  }

  @Override
  public void onDestroyView() {
    if (binding != null) {
      WebView webView = binding.walmartStoreWebView;
      webView.stopLoading();
      webView.loadUrl("about:blank");
      webView.clearHistory();
      webView.removeAllViews();
      webView.destroy();
      binding = null;
    }
    super.onDestroyView();
  }
}
