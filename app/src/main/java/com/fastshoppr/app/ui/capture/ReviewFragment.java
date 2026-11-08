/*
 * FastShoppr
 *
 * Copyright (c) 2026 FastShoppr contributors.
 */
package com.fastshoppr.app.ui.capture;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fastshoppr.app.R;
import com.fastshoppr.app.databinding.FragmentReviewBinding;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;

@AndroidEntryPoint
public final class ReviewFragment extends Fragment {

  private FragmentReviewBinding binding;
  private ReviewViewModel viewModel;
  private ReviewAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentReviewBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(ReviewViewModel.class);

    viewModel
        .getEvents()
        .observe(
            getViewLifecycleOwner(),
            singleEvent -> {
              ReviewViewModel.ReviewUiEvent content = singleEvent.getContentIfNotHandled();
              if (content == null) {
                return;
              }
              if (content instanceof ReviewViewModel.ReviewUiEvent.LineAdded) {
                int index = viewModel.getLines().size() - 1;
                adapter.notifyItemInserted(index);
                binding.recyclerView.scrollToPosition(index);
              } else if (content instanceof ReviewViewModel.ReviewUiEvent.LinesChanged) {
                adapter.notifyDataSetChanged();
              } else if (content instanceof ReviewViewModel.ReviewUiEvent.SelectionChanged) {
                binding.checkSelectAll.setChecked(
                    viewModel.getSelectedCount() == viewModel.getLines().size());
                binding.checkSelectAll.setVisibility(
                    viewModel.getLines().isEmpty() ? View.GONE : View.VISIBLE);
              } else if (content instanceof ReviewViewModel.ReviewUiEvent.SaveCompleted) {
                requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack(
                        R.id.lists, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
              } else if (content instanceof ReviewViewModel.ReviewUiEvent.Discarded) {
                requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack(
                        R.id.lists, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
              }
            });

    adapter =
        new ReviewAdapter(
            viewModel.getLines(),
            new ReviewAdapter.OnLineChanged() {
              @Override
              public void onLineChanged(int position, boolean selected) {}
            },
            new ReviewAdapter.OnDeleteSelected() {
              @Override
              public void onDeleteSelected() {
                viewModel.deleteSelected();
              }
            },
            new ReviewAdapter.OnMergeSelected() {
              @Override
              public void onMergeSelected() {
                viewModel.mergeSelected();
              }
            },
            new ReviewAdapter.OnAddLine() {
              @Override
              public void onAddLine() {
                viewModel.addLine("");
              }
            },
            new ReviewAdapter.OnSplitLine() {
              @Override
              public void onSplitLine() {
                int selectedIndex = getSelectedIndex();
                if (selectedIndex >= 0) {
                  viewModel.splitLine(selectedIndex);
                }
              }
            });
    binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.recyclerView.setAdapter(adapter);

    binding.checkSelectAll.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
              viewModel.selectAll();
            } else {
              viewModel.deselectAll();
            }
            adapter.notifyDataSetChanged();
          }
        });

    binding.btnDeleteSelected.setOnClickListener(v -> viewModel.deleteSelected());

    binding.btnMergeSelected.setOnClickListener(v -> viewModel.mergeSelected());

    binding.btnAddLine.setOnClickListener(v -> viewModel.addLine(""));

    binding.btnSplit.setOnClickListener(
        v -> {
          int selectedIndex = getSelectedIndex();
          if (selectedIndex >= 0) {
            viewModel.splitLine(selectedIndex);
          }
        });

    binding.btnSave.setOnClickListener(v -> viewModel.saveSelected());

    binding.btnDiscard.setOnClickListener(v -> viewModel.discard());

    viewModel.loadCapture();
  }

  private int getSelectedIndex() {
    List<ReviewViewModel.ReviewLine> lines = viewModel.getLines();
    for (int index = 0; index < lines.size(); index++) {
      if (lines.get(index).selected) {
        return index;
      }
    }
    return -1;
  }

  public static final class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private final List<ReviewViewModel.ReviewLine> lines;
    private final OnLineChanged onLineChanged;
    private final OnDeleteSelected onDeleteSelected;
    private final OnMergeSelected onMergeSelected;
    private final OnAddLine onAddLine;
    private final OnSplitLine onSplitLine;

    public ReviewAdapter(
        List<ReviewViewModel.ReviewLine> lines,
        OnLineChanged onLineChanged,
        OnDeleteSelected onDeleteSelected,
        OnMergeSelected onMergeSelected,
        OnAddLine onAddLine,
        OnSplitLine onSplitLine) {
      this.lines = lines;
      this.onLineChanged = onLineChanged;
      this.onDeleteSelected = onDeleteSelected;
      this.onMergeSelected = onMergeSelected;
      this.onAddLine = onAddLine;
      this.onSplitLine = onSplitLine;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view =
          LayoutInflater.from(parent.getContext())
              .inflate(R.layout.item_review_line, parent, false);
      return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
      ReviewViewModel.ReviewLine line = lines.get(position);
      holder.checkBox.setChecked(line.selected);
      holder.textField.setText(line.text);
      holder.confidenceText.setText(
          String.format("%.1f", line.confidence)
              + " "
              + holder.context.getString(R.string.low_confidence));
      holder.checkBox.setOnCheckedChangeListener(
          new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
              int adapterPosition = holder.getAdapterPosition();
              if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
              }
              ReviewViewModel.ReviewLine line = lines.get(adapterPosition);
              line.selected = isChecked;
              onLineChanged.onLineChanged(adapterPosition, isChecked);
            }
          });
      holder.textField.setOnEditorActionListener(
          (v, actionId, event) -> {
            line.text = holder.textField.getText().toString();
            int editorPosition = holder.getAdapterPosition();
            if (editorPosition != RecyclerView.NO_POSITION) {
              onLineChanged.onLineChanged(editorPosition, false);
            }
            return true;
          });
    }

    @Override
    public int getItemCount() {
      return lines.size();
    }

    public static final class ViewHolder extends RecyclerView.ViewHolder {

      private final android.content.Context context;
      private final android.widget.CheckBox checkBox;
      private final EditText textField;
      private final TextView confidenceText;

      public ViewHolder(@NonNull View view) {
        super(view);
        context = view.getContext();
        checkBox = view.findViewById(R.id.checkBox);
        textField = view.findViewById(R.id.textField);
        confidenceText = view.findViewById(R.id.confidenceText);
      }
    }

    public interface OnLineChanged {
      void onLineChanged(int position, boolean selected);
    }

    public interface OnDeleteSelected {
      void onDeleteSelected();
    }

    public interface OnMergeSelected {
      void onMergeSelected();
    }

    public interface OnAddLine {
      void onAddLine();
    }

    public interface OnSplitLine {
      void onSplitLine();
    }
  }
}
