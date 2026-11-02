package me.theoria.barlinex.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;

import java.util.List;

import me.theoria.barlinex.R;
import me.theoria.barlinex.ui.controller.BarcodeInputController;

public class BarcodeFormatAdapter extends ArrayAdapter<BarcodeFormat> {

    public BarcodeFormatAdapter(
            Context context,
            List<BarcodeFormat> formats
    ) {
        super(context, 0, formats);
    }

    @Override
    public View getView(
            int position,
            View convertView,
            ViewGroup parent
    ) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(
            int position,
            View convertView,
            ViewGroup parent
    ) {
        return createView(position, convertView, parent);
    }

    private View createView(
            int position,
            View convertView,
            ViewGroup parent
    ) {
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(
                    R.layout.item_barcode_format,
                    parent,
                    false
            );
        }

        BarcodeFormat format = getItem(position);

        ImageView icon = view.findViewById(R.id.imageFormatIcon);
        TextView name = view.findViewById(R.id.textFormatName);
        TextView description = view.findViewById(
                R.id.textFormatDescription
        );

        if (format != null) {
            name.setText(BarcodeInputController.getFormatDisplayName(format));
            description.setText(getDescription(format));

            icon.setImageResource(
                    format == BarcodeFormat.QR_CODE
                            ? R.drawable.ic_qr_code
                            : R.drawable.ic_barcode
            );

            icon.setColorFilter(
                    android.graphics.Color.parseColor("#8B9EFF")
            );
        }

        return view;
    }

    private String getDescription(BarcodeFormat format) {
        switch (format) {
            case QR_CODE:
                return "Links, text and contact information";
            case CODE_128:
                return "Shipping and inventory labels";
            case CODE_39:
                return "Industrial identification";
            case EAN_13:
                return "Standard retail products";
            case EAN_8:
                return "Compact retail products";
            case UPC_A:
                return "US and Canadian retail products";
            case UPC_E:
                return "Compact UPC for small packages";
            case ITF:
                return "Shipping cartons and cases";
            default:
                return "";
        }
    }
}
