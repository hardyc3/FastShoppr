/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import com.fastshoppr.app.databinding.ViewStateBinding;

public final class StateView extends LinearLayout {

  private final ViewStateBinding binding;

  public StateView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);
    setGravity(android.view.Gravity.CENTER);
    int padding = (int) (24 * getResources().getDisplayMetrics().density);
    setPadding(padding, padding, padding, padding);
    binding = ViewStateBinding.inflate(LayoutInflater.from(context), this);
  }

  public void showLoading() {
    setVisibility(View.VISIBLE);
    binding.stateProgress.setVisibility(View.VISIBLE);
    binding.stateMessage.setVisibility(View.GONE);
    binding.stateAction.setVisibility(View.GONE);
  }

  public void showMessage(String message) {
    setVisibility(View.VISIBLE);
    binding.stateProgress.setVisibility(View.GONE);
    binding.stateMessage.setText(message);
    binding.stateMessage.setVisibility(View.VISIBLE);
    binding.stateAction.setVisibility(View.GONE);
  }

  public void showAction(String message, String actionLabel, OnClickListener listener) {
    showMessage(message);
    binding.stateAction.setText(actionLabel);
    binding.stateAction.setVisibility(View.VISIBLE);
    binding.stateAction.setOnClickListener(listener);
  }

  public void showContent() {
    setVisibility(View.GONE);
    binding.stateProgress.setVisibility(View.GONE);
    binding.stateMessage.setVisibility(View.GONE);
    binding.stateAction.setVisibility(View.GONE);
  }
}
