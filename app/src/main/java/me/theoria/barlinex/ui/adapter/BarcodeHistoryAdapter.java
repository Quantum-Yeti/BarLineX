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

import me.theoria.barlinex.R;
import me.theoria.barlinex.data.SavedBarcode;

public class BarcodeHistoryAdapter
        extends RecyclerView.Adapter<BarcodeHistoryAdapter.BarcodeViewHolder> {

    private final List<SavedBarcode> barcodes = new ArrayList<>();

    private final OnBarcodeClickListener clickListener;
    private final OnBarcodeDeleteListener deleteListener;
    private final OnFavoriteClickListener favoriteListener;

    public interface OnBarcodeClickListener {
        void onClick(SavedBarcode barcode);
    }

    public interface OnBarcodeDeleteListener {
        void onDelete(SavedBarcode barcode);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(SavedBarcode barcode);
    }

    public BarcodeHistoryAdapter(
            OnBarcodeClickListener clickListener,
            OnBarcodeDeleteListener deleteListener,
            OnFavoriteClickListener favoriteListener) {

        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
        this.favoriteListener = favoriteListener;
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

        SavedBarcode barcode = barcodes.get(position);

        holder.textContent.setText(barcode.content);
        holder.textFormat.setText(barcode.format);

        String date = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT
        ).format(new Date(barcode.createdAt));

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
    }

    @Override
    public int getItemCount() {
        return barcodes.size();
    }

    public void setBarcodes(List<SavedBarcode> newBarcodes) {
        List<SavedBarcode> updatedList = new ArrayList<>();

        if (newBarcodes != null) {
            updatedList.addAll(newBarcodes);
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return barcodes.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return updatedList.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldPosition, int newPosition) {
                        return barcodes.get(oldPosition).id
                                == updatedList.get(newPosition).id;
                    }

                    @Override
                    public boolean areContentsTheSame(int oldPosition, int newPosition) {
                        SavedBarcode oldItem = barcodes.get(oldPosition);
                        SavedBarcode newItem = updatedList.get(newPosition);

                        return oldItem.isFavorite == newItem.isFavorite
                                && oldItem.content.equals(newItem.content)
                                && oldItem.format.equals(newItem.format)
                                && oldItem.createdAt == newItem.createdAt;
                    }
                }
        );

        barcodes.clear();
        barcodes.addAll(updatedList);

        diffResult.dispatchUpdatesTo(this);
    }

    static class BarcodeViewHolder extends RecyclerView.ViewHolder {

        TextView textContent;
        TextView textFormat;
        TextView textDate;
        ImageButton buttonDelete;
        ImageButton buttonFavorite;

        BarcodeViewHolder(@NonNull View itemView) {
            super(itemView);

            textContent = itemView.findViewById(R.id.textBarcodeContent);
            textFormat = itemView.findViewById(R.id.textBarcodeFormat);
            textDate = itemView.findViewById(R.id.textBarcodeDate);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteBarcode);
            buttonFavorite = itemView.findViewById(R.id.buttonFavoriteBarcode);
        }
    }
}