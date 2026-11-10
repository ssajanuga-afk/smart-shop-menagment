package com.example.smartshopmanager.sales;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartshopmanager.R;
import com.example.smartshopmanager.models.Sale;

import java.util.List;

public class SalesHistoryAdapter extends RecyclerView.Adapter<SalesHistoryAdapter.SaleViewHolder> {

    private Context context;
    private List<Sale> saleList;

    public SalesHistoryAdapter(Context context, List<Sale> saleList) {
        this.context = context;
        this.saleList = saleList;
    }

    @NonNull
    @Override
    public SaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sale, parent, false);
        return new SaleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SaleViewHolder holder, int position) {
        Sale sale = saleList.get(position);
        holder.tvIdDate.setText("Sale #" + sale.getSaleId() + " - " + sale.getSaleDate());
        holder.tvCustomer.setText("Customer: " + (sale.getCustomerName() != null ? sale.getCustomerName() : "Walk-in"));
        holder.tvPayment.setText("Paid via: " + (sale.getPaymentMethod() != null ? sale.getPaymentMethod() : "Cash"));
        holder.tvTotal.setText(String.format("$%.2f", sale.getTotalAmount()));
    }

    @Override
    public int getItemCount() {
        return saleList.size();
    }

    public static class SaleViewHolder extends RecyclerView.ViewHolder {
        TextView tvIdDate, tvCustomer, tvPayment, tvTotal;

        public SaleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIdDate = itemView.findViewById(R.id.tvSaleIdDate);
            tvCustomer = itemView.findViewById(R.id.tvSaleCustomer);
            tvPayment = itemView.findViewById(R.id.tvSalePaymentMethod);
            tvTotal = itemView.findViewById(R.id.tvSaleTotalAmount);
        }
    }
}
