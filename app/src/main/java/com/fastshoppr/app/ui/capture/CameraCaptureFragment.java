/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.fastshoppr.app.R;
import com.fastshoppr.app.data.capture.CaptureImageStore;
import com.fastshoppr.app.databinding.FragmentCameraCaptureBinding;
import com.fastshoppr.app.ui.common.SingleEvent;
import com.fastshoppr.app.ui.common.UiErrorText;
import com.fastshoppr.app.ui.navigation.NavArguments;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

@AndroidEntryPoint
public final class CameraCaptureFragment extends Fragment {

  private FragmentCameraCaptureBinding binding;
  private CameraCaptureViewModel viewModel;

  @Inject CaptureImageStore imageStore;

  private ProcessCameraProvider cameraProvider;
  private ImageCapture imageCapture;

  private final ActivityResultLauncher<String> permissionLauncher =
      registerForActivityResult(
          new ActivityResultContracts.RequestPermission(),
          granted -> {
            if (viewModel != null) {
              onPermissionResult(granted);
            }
          });

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentCameraCaptureBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(CameraCaptureViewModel.class);

    binding.toolbar.setTitle(R.string.capture_camera_toolbar_title);
    binding.toolbar.setNavigationContentDescription(R.string.action_back);
    binding.toolbar.setNavigationOnClickListener(
        v -> Navigation.findNavController(v).popBackStack());

    binding.btnAllowCamera.setOnClickListener(
        v -> permissionLauncher.launch(Manifest.permission.CAMERA));
    binding.btnCapture.setOnClickListener(v -> capturePhoto());
    binding.btnRetake.setOnClickListener(
        v -> {
          viewModel.retake();
          renderState(new CameraCaptureViewModel.CameraCaptureUiState(false, false));
        });

    viewModel.getState().observe(getViewLifecycleOwner(), this::renderState);
    viewModel.getEvents().observe(getViewLifecycleOwner(), this::handleEvent);

    updatePermissionUi(false);
  }

  @Override
  public void onStart() {
    super.onStart();
    if (hasCameraPermission()) {
      ensureCamera();
    }
  }

  @Override
  public void onStop() {
    unbindCamera();
    super.onStop();
  }

  private boolean hasCameraPermission() {
    return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED;
  }

  private void ensureCamera() {
    if (binding == null || !hasCameraPermission() || !isAdded()) {
      return;
    }
    ListenableFuture<ProcessCameraProvider> future =
        ProcessCameraProvider.getInstance(requireContext());
    future.addListener(
        () -> {
          if (binding == null || !hasCameraPermission() || !isAdded()) {
            return;
          }
          try {
            cameraProvider = future.get();
            bindCamera();
          } catch (ExecutionException | RuntimeException exception) {
            showSnackbar(R.string.camera_unavailable);
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            showSnackbar(R.string.camera_unavailable);
          }
        },
        ContextCompat.getMainExecutor(requireContext()));
  }

  private void bindCamera() {
    try {
      Preview preview = new Preview.Builder().build();
      preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());
      imageCapture =
          new ImageCapture.Builder()
              .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
              .build();
      cameraProvider.unbindAll();
      cameraProvider.bindToLifecycle(
          getViewLifecycleOwner(), CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
      updatePermissionUi(false);
    } catch (RuntimeException exception) {
      imageCapture = null;
      showSnackbar(R.string.camera_unavailable);
    }
  }

  private void unbindCamera() {
    if (cameraProvider != null) {
      cameraProvider.unbindAll();
    }
    imageCapture = null;
  }

  private void capturePhoto() {
    if (imageCapture == null || binding == null) {
      showSnackbar(R.string.camera_unavailable);
      return;
    }
    final File file;
    try {
      file = imageStore.newImageFile();
    } catch (IOException | RuntimeException exception) {
      showSnackbar(R.string.camera_capture_failed);
      return;
    }
    imageCapture.takePicture(
        new ImageCapture.OutputFileOptions.Builder(file).build(),
        ContextCompat.getMainExecutor(requireContext()),
        new ImageCapture.OnImageSavedCallback() {
          @Override
          public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
            if (binding == null) {
              return;
            }
            viewModel.onImageCaptured(file.getAbsolutePath());
          }

          @Override
          public void onError(@NonNull ImageCaptureException exception) {
            if (binding != null) {
              showSnackbar(R.string.camera_capture_failed);
            }
          }
        });
  }

  private void renderState(CameraCaptureViewModel.CameraCaptureUiState state) {
    if (binding == null) {
      return;
    }
    binding.recognitionProgress.setVisibility(state.recognizing() ? View.VISIBLE : View.GONE);
    binding.btnCapture.setEnabled(
        !state.recognizing() && hasCameraPermission() && imageCapture != null);
    binding.btnRetake.setVisibility(state.canRetake() ? View.VISIBLE : View.GONE);
    binding.btnRetake.setEnabled(!state.recognizing());
  }

  private void updatePermissionUi(boolean denied) {
    if (binding == null) {
      return;
    }
    boolean granted = hasCameraPermission();
    binding.permissionPanel.setVisibility(granted ? View.GONE : View.VISIBLE);
    binding.permissionMessage.setText(
        denied ? R.string.camera_permission_denied : R.string.camera_permission_message);
    binding.btnCapture.setEnabled(granted && imageCapture != null);
  }

  private void handleEvent(SingleEvent<CameraCaptureViewModel.CameraUiEvent> event) {
    CameraCaptureViewModel.CameraUiEvent content = event.getContentIfNotHandled();
    if (content == null || binding == null) {
      return;
    }
    if (content instanceof CameraCaptureViewModel.CameraUiEvent.NavigateToReview navigate) {
      Navigation.findNavController(requireView())
          .navigate(R.id.review, NavArguments.forCapture(navigate.captureId()));
    } else if (content instanceof CameraCaptureViewModel.CameraUiEvent.ShowError error) {
      Snackbar.make(binding.getRoot(), UiErrorText.stringRes(error.error()), Snackbar.LENGTH_SHORT)
          .show();
    } else if (content instanceof CameraCaptureViewModel.CameraUiEvent.EmptyRecognition) {
      EmptyRecognitionDialog.show(
          this, () -> {}, this::navigateToTypedCapture, this::navigateToTypedCapture);
    }
  }

  private void navigateToTypedCapture() {
    String listId = requireArguments().getString(NavArguments.LIST_ID);
    Navigation.findNavController(requireView())
        .navigate(R.id.capture_typed, NavArguments.forList(listId));
  }

  private void showSnackbar(int messageRes) {
    if (binding != null) {
      Snackbar.make(binding.getRoot(), messageRes, Snackbar.LENGTH_SHORT).show();
    }
  }

  private void onPermissionResult(boolean granted) {
    if (granted) {
      updatePermissionUi(false);
      ensureCamera();
    } else {
      updatePermissionUi(true);
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
    imageCapture = null;
  }
}
