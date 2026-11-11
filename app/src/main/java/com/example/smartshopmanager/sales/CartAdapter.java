package com.example.smartshopmanager.sales;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartshopmanager.R;
import com.example.smartshopmanager.models.SaleItem;

import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface OnCartActionListener {
        void onIncreaseQty(int position);
        void onDecreaseQty(int position);
        void onRemoveClick(int position);
    }

    private final Context context;
    private final List<SaleItem> cartItems;
    private final OnCartActionListener listener;

    public CartAdapter(Context context, List<SaleItem> cartItems, OnCartActionListener listener) {
        this.context = context;
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        SaleItem item = cartItems.get(position);
        holder.tvName.setText(item.getProductName());
        holder.tvPriceEach.setText(String.format(Locale.getDefault(), "$%.2f each", item.getUnitPrice()));
        holder.tvQty.setText(String.valueOf(item.getQuantity()));
        holder.tvTotal.setText(String.format(Locale.getDefault(), "$%.2f", item.getTotalPrice()));

        holder.btnIncrease.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (listener != null && pos != RecyclerView.NO_POSITION) listener.onIncreaseQty(pos);
        });
        holder.btnDecrease.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (listener != null && pos != RecyclerView.NO_POSITION) listener.onDecreaseQty(pos);
        });
        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (listener != null && pos != RecyclerView.NO_POSITION) listener.onRemoveClick(pos);
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPriceEach, tvQty, tvTotal;
        Button btnIncrease, btnDecrease, btnRemove;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCartProductName);
            tvPriceEach = itemView.findViewById(R.id.tvCartProductPrice);
            tvQty = itemView.findViewById(R.id.tvCartQty);
            tvTotal = itemView.findViewById(R.id.tvCartItemTotal);
            btnIncrease = itemView.findViewById(R.id.btnIncreaseCartQty);
            btnDecrease = itemView.findViewById(R.id.btnDecreaseCartQty);
            btnRemove = itemView.findViewById(R.id.btnRemoveCartItem);
        }
    }
}
