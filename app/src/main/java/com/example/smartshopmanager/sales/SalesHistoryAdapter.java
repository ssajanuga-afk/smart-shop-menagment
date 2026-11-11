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
import java.util.Locale;

public class SalesHistoryAdapter extends RecyclerView.Adapter<SalesHistoryAdapter.SaleViewHolder> {

    public interface OnSaleClickListener {
        void onSaleClick(Sale sale);
    }

    private final Context context;
    private List<Sale> saleList;
    private OnSaleClickListener listener;

    public SalesHistoryAdapter(Context context, List<Sale> saleList) {
        this(context, saleList, null);
    }

    public SalesHistoryAdapter(Context context, List<Sale> saleList, OnSaleClickListener listener) {
        this.context = context;
        this.saleList = saleList;
        this.listener = listener;
    }

    public void updateList(List<Sale> newList) {
        this.saleList = newList;
        notifyDataSetChanged();
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

        String cust = (sale.getCustomerName() != null && !sale.getCustomerName().isEmpty())
                ? sale.getCustomerName() : "Walk-in";
        String cashier = (sale.getCashierName() != null && !sale.getCashierName().isEmpty())
                ? " • " + sale.getCashierName() : "";
        holder.tvCustomer.setText("Customer: " + cust + cashier);

        String method = sale.getPaymentMethod() != null ? sale.getPaymentMethod() : "Cash";
        holder.tvPayment.setText("Paid via: " + method + " • Tap for receipt");
        holder.tvTotal.setText(String.format(Locale.getDefault(), "$%.2f", sale.getTotalAmount()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSaleClick(sale);
        });
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
