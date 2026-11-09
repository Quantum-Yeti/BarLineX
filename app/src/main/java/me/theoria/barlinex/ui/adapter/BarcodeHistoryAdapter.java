package me.theoria.barlinex.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import me.theoria.barlinex.R;
import me.theoria.barlinex.data.BarcodeEntity;

public class BarcodeHistoryAdapter
        extends RecyclerView.Adapter<BarcodeHistoryAdapter.BarcodeViewHolder> {

    private final List<BarcodeEntity> barcodes = new ArrayList<>();
    private final List<BarcodeEntity> filteredBarcodes = new ArrayList<>();

    private final OnBarcodeClickListener clickListener;
    private final OnBarcodeDeleteListener deleteListener;
    private final OnFavoriteClickListener favoriteListener;
    private final OnBarcodeRenameListener renameListener;

    public interface OnBarcodeClickListener {
        void onClick(BarcodeEntity barcode);
    }

    public interface OnBarcodeDeleteListener {
        void onDelete(BarcodeEntity barcode);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(BarcodeEntity barcode);
    }

    public interface OnBarcodeRenameListener {
        void onRename(BarcodeEntity barcode);
    }

    public BarcodeHistoryAdapter(
            OnBarcodeClickListener clickListener,
            OnBarcodeDeleteListener deleteListener,
            OnFavoriteClickListener favoriteListener,
            OnBarcodeRenameListener renameListener
    ) {
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
        this.favoriteListener = favoriteListener;
        this.renameListener = renameListener;
    }

    @NonNull
    @Override
    public BarcodeViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_barcode, parent, false);

        return new BarcodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull BarcodeViewHolder holder, int position) {

        BarcodeEntity barcode = filteredBarcodes.get(position);

        String displayName = barcode.barcodeName == null || barcode.barcodeName.trim().isEmpty()
                ? barcode.barcodeContent
                : barcode.barcodeName;

        holder.textContent.setText(displayName);
        holder.textFormat.setText(barcode.barcodeFormat);

        String date = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT
        ).format(new Date(barcode.barcodeCreatedAt));

        holder.textDate.setText(date);

        holder.buttonFavorite.setImageResource(
                barcode.isFavorite
                        ? R.drawable.ic_favorite_red_filled
                        : R.drawable.ic_favorite
        );

        holder.buttonFavorite.setContentDescription(
                barcode.isFavorite
                        ? "Remove from favorites"
                        : "Add to favorites"
        );

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClick(barcode);
            }
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(barcode);
            }
        });

        holder.buttonFavorite.setOnClickListener(v -> {
            if (favoriteListener != null) {
                favoriteListener.onFavoriteClick(barcode);
            }
        });

        holder.buttonRename.setOnClickListener(v -> {
            if (renameListener != null) {
                renameListener.onRename(barcode);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredBarcodes.size();
    }

    public void setBarcodes(List<BarcodeEntity> newBarcodes) {
        barcodes.clear();

        if (newBarcodes != null) {
            barcodes.addAll(newBarcodes);
        }

        filter("");
    }

    public void filter(String query) {
        String searchQuery = query == null
                ? ""
                : query.trim().toLowerCase(Locale.ROOT);

        List<BarcodeEntity> updatedList = new ArrayList<>();

        for (BarcodeEntity barcode : barcodes) {

            String name = barcode.barcodeName == null ? "" : barcode.barcodeName;
            String content = barcode.barcodeContent == null ? "" : barcode.barcodeContent;
            String format = barcode.barcodeFormat == null ? "" : barcode.barcodeFormat;

            if (name.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || content.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || format.toLowerCase(Locale.ROOT).contains(searchQuery)) {

                updatedList.add(barcode);
            }
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return filteredBarcodes.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return updatedList.size();
                    }

                    @Override
                    public boolean areItemsTheSame(
                            int oldPosition, int newPosition) {

                        return filteredBarcodes.get(oldPosition).id
                                == updatedList.get(newPosition).id;
                    }

                    @Override
                    public boolean areContentsTheSame(
                            int oldPosition, int newPosition) {

                        BarcodeEntity oldItem = filteredBarcodes.get(oldPosition);
                        BarcodeEntity newItem = updatedList.get(newPosition);

                        return oldItem.isFavorite == newItem.isFavorite
                                && Objects.equals(oldItem.barcodeName, newItem.barcodeName)
                                && Objects.equals(oldItem.barcodeContent, newItem.barcodeContent)
                                && Objects.equals(oldItem.barcodeFormat, newItem.barcodeFormat)
                                && oldItem.barcodeCreatedAt == newItem.barcodeCreatedAt;
                    }
                }
        );

        filteredBarcodes.clear();
        filteredBarcodes.addAll(updatedList);

        diffResult.dispatchUpdatesTo(this);
    }

    static class BarcodeViewHolder extends RecyclerView.ViewHolder {

        TextView textContent;
        TextView textFormat;
        TextView textDate;
        ImageButton buttonDelete;
        ImageButton buttonFavorite;
        ImageButton buttonRename;

        BarcodeViewHolder(@NonNull View itemView) {
            super(itemView);

            textContent = itemView.findViewById(R.id.textBarcodeContent);
            textFormat = itemView.findViewById(R.id.textBarcodeFormat);
            textDate = itemView.findViewById(R.id.textBarcodeDate);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteBarcode);
            buttonFavorite = itemView.findViewById(R.id.buttonFavoriteBarcode);
            buttonRename = itemView.findViewById(R.id.buttonRenameBarcode);
        }
    }
}
