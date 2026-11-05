package me.theoria.barlinex.ui.adapter;

import android.content.Context;
import android.graphics.Color;
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

    public BarcodeFormatAdapter(Context context, List<BarcodeFormat> formats) {
        super(context, 0, formats);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
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
        TextView description = view.findViewById(R.id.textFormatDescription);

        if (format != null) {
            name.setText(BarcodeInputController.getFormatDisplayName(format));
            description.setText(getDescription(format));

            icon.setImageResource(
                    format == BarcodeFormat.QR_CODE
                            ? R.drawable.ic_qr_code
                            : R.drawable.ic_barcode
            );

            icon.setColorFilter(Color.parseColor("#8B9EFF"));
        }

        return view;
    }

    private String getDescription(BarcodeFormat format) {
        switch (format) {
            case QR_CODE:
                return "Websites, Wi-Fi, contacts, and general data";

            case CODE_128:
                return "Shipping labels, inventory, and logistics";

            case CODE_39:
                return "Industrial labels and asset tracking";

            case EAN_13:
                return "Retail products and international trade";

            case EAN_8:
                return "Small retail products and packaging";

            case UPC_A:
                return "Retail products in the US and Canada";

            case UPC_E:
                return "Compact retail product packaging";

            case ITF:
                return "Shipping cartons and warehouse cases";

            default:
                return "General-purpose barcode";
        }
    }
}