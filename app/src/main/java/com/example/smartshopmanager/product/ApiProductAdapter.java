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
import com.example.smartshopmanager.auth.RoleManager;
import com.example.smartshopmanager.models.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ApiProductAdapter extends RecyclerView.Adapter<ApiProductAdapter.ApiViewHolder> {

    private final Context context;
    private final List<Product> allApiProducts;
    private List<Product> filteredApiProducts;
    private final OnImportListener listener;

    public interface OnImportListener {
        void onImportClick(Product product);
    }

    public ApiProductAdapter(Context context, List<Product> apiProducts, OnImportListener listener) {
        this.context = context;
        this.allApiProducts = new ArrayList<>(apiProducts);
        this.filteredApiProducts = new ArrayList<>(apiProducts);
        this.listener = listener;
    }

    public void filter(String keyword) {
        filteredApiProducts = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            filteredApiProducts.addAll(allApiProducts);
        } else {
            String query = keyword.trim().toLowerCase(Locale.ROOT);
            for (Product p : allApiProducts) {
                String name = p.getName() != null ? p.getName().toLowerCase(Locale.ROOT) : "";
                String cat = p.getCategoryName() != null ? p.getCategoryName().toLowerCase(Locale.ROOT) : "";
                if (name.contains(query) || cat.contains(query)) {
                    filteredApiProducts.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ApiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_api_product, parent, false);
        return new ApiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApiViewHolder holder, int position) {
        Product product = filteredApiProducts.get(position);
        holder.tvName.setText(product.getName());
        holder.tvCategory.setText("Category: " + product.getCategoryName());
        holder.tvPrice.setText(String.format(Locale.getDefault(), "$%.2f", product.getPrice()));

        boolean canImport = RoleManager.canImportApiProducts(RoleManager.getCurrentRole(context));
        holder.btnImport.setVisibility(canImport ? View.VISIBLE : View.GONE);
        holder.btnImport.setOnClickListener(v -> {
            if (listener != null) listener.onImportClick(product);
        });
    }

    @Override
    public int getItemCount() {
        return filteredApiProducts.size();
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
