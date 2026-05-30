package io.github.godsarmy.mlmarkdown.sample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

final class ExplainPagerAdapter extends RecyclerView.Adapter<ExplainPagerAdapter.PageViewHolder> {
    private final List<ExplainPageItem> items = new ArrayList<>();

    void submit(List<ExplainPageItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    String getTitle(int position) {
        return items.get(position).getTitle();
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_explain_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        ExplainPageItem item = items.get(position);
        holder.itemsContainer.removeViews(
                1, Math.max(0, holder.itemsContainer.getChildCount() - 1));

        List<String> entries = item.getEntries();
        if (entries.isEmpty()) {
            holder.emptyText.setText(item.getEmptyText());
            holder.emptyText.setVisibility(View.VISIBLE);
            return;
        }

        holder.emptyText.setVisibility(View.GONE);
        for (int i = 0; i < entries.size(); i++) {
            AppCompatTextView textView = new AppCompatTextView(holder.itemView.getContext());
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) {
                params.topMargin = dp(holder.itemView, 10);
            }
            textView.setLayoutParams(params);
            textView.setBackgroundResource(R.drawable.preview_box_background);
            int padding = dp(holder.itemView, 12);
            textView.setPadding(padding, padding, padding, padding);
            textView.setTextColor(
                    ContextCompat.getColor(
                            holder.itemView.getContext(), R.color.mlkit_on_background));
            textView.setTextIsSelectable(true);
            textView.setText(entries.get(i));
            holder.itemsContainer.addView(textView);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class PageViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout itemsContainer;
        private final TextView emptyText;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            itemsContainer = itemView.findViewById(R.id.explainPageItemsContainer);
            emptyText = itemView.findViewById(R.id.explainPageEmptyText);
        }
    }

    private static int dp(View view, int value) {
        float density = view.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
