package com.example.smartshopmanager.product;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartshopmanager.R;
import com.example.smartshopmanager.models.Product;

import java.util.List;

public class ApiProductAdapter extends RecyclerView.Adapter<ApiProductAdapter.ApiViewHolder> {

    private Context context;
    private List<Product> apiProducts;
    private OnImportListener listener;

    public interface OnImportListener {
        void onImportClick(Product product);
    }

    public ApiProductAdapter(Context context, List<Product> apiProducts, OnImportListener listener) {
        this.context = context;
        this.apiProducts = apiProducts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ApiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_api_product, parent, false);
        return new ApiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApiViewHolder holder, int position) {
        Product product = apiProducts.get(position);
        holder.tvName.setText(product.getName());
        holder.tvCategory.setText("Category: " + product.getCategoryName());
        holder.tvPrice.setText(String.format("$%.2f", product.getPrice()));

        holder.btnImport.setOnClickListener(v -> {
            if (listener != null) listener.onImportClick(product);
        });
    }

    @Override
    public int getItemCount() {
        return apiProducts.size();
    }

    public static class ApiViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory, tvPrice;
        Button btnImport;

        public ApiViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvApiProductName);
            tvCategory = itemView.findViewById(R.id.tvApiProductCategory);
            tvPrice = itemView.findViewById(R.id.tvApiProductPrice);
            btnImport = itemView.findViewById(R.id.btnImportProduct);
        }
    }
}
