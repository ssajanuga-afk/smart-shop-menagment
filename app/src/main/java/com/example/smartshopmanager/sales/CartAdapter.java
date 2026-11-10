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

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private Context context;
    private List<SaleItem> cartItems;
    private OnCartRemoveListener listener;

    public interface OnCartRemoveListener {
        void onRemoveClick(int position);
    }

    public CartAdapter(Context context, List<SaleItem> cartItems, OnCartRemoveListener listener) {
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
        holder.tvPriceQty.setText(String.format("$%.2f x %d", item.getUnitPrice(), item.getQuantity()));
        holder.tvTotal.setText(String.format("$%.2f", item.getTotalPrice()));

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) listener.onRemoveClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPriceQty, tvTotal;
        Button btnRemove;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCartProductName);
            tvPriceQty = itemView.findViewById(R.id.tvCartProductPrice);
            tvTotal = itemView.findViewById(R.id.tvCartItemTotal);
            btnRemove = itemView.findViewById(R.id.btnRemoveCartItem);
        }
    }
}
