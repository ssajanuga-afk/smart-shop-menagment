package com.example.smartshopmanager.customer;

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
import com.example.smartshopmanager.models.Customer;

import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder> {

    private final Context context;
    private List<Customer> customerList;
    private final OnCustomerListener listener;

    public interface OnCustomerListener {
        void onCustomerClick(Customer customer);
        void onEditClick(Customer customer);
        void onDeleteClick(Customer customer);
    }

    public CustomerAdapter(Context context, List<Customer> customerList, OnCustomerListener listener) {
        this.context = context;
        this.customerList = customerList;
        this.listener = listener;
    }

    public void updateList(List<Customer> newList) {
        this.customerList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_customer, parent, false);
        return new CustomerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        Customer customer = customerList.get(position);
        holder.tvName.setText(customer.getName());
        holder.tvPhone.setText("Phone: " + (customer.getPhone() != null && !customer.getPhone().isEmpty() ? customer.getPhone() : "N/A"));
        holder.tvEmail.setText("Email: " + (customer.getEmail() != null && !customer.getEmail().isEmpty() ? customer.getEmail() : "N/A"));

        String role = RoleManager.getCurrentRole(context);
        holder.btnEdit.setVisibility(RoleManager.canAddEditCustomers(role) ? View.VISIBLE : View.GONE);
        holder.btnDelete.setVisibility(RoleManager.canDeleteItems(role) ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCustomerClick(customer);
        });
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(customer);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(customer);
        });
    }

    @Override
    public int getItemCount() {
        return customerList.size();
    }

    public static class CustomerViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvEmail;
        Button btnEdit, btnDelete;

        public CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCustomerName);
            tvPhone = itemView.findViewById(R.id.tvCustomerPhone);
            tvEmail = itemView.findViewById(R.id.tvCustomerEmail);
            btnEdit = itemView.findViewById(R.id.btnEditCustomer);
            btnDelete = itemView.findViewById(R.id.btnDeleteCustomer);
        }
    }
}
