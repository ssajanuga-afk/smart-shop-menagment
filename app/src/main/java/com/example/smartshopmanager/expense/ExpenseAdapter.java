package com.example.smartshopmanager.expense;

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
import com.example.smartshopmanager.models.Expense;

import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private final Context context;
    private List<Expense> expenseList;
    private final OnExpenseListener listener;

    public interface OnExpenseListener {
        void onEditClick(Expense expense);
        void onDeleteClick(Expense expense);
    }

    public ExpenseAdapter(Context context, List<Expense> expenseList, OnExpenseListener listener) {
        this.context = context;
        this.expenseList = expenseList;
        this.listener = listener;
    }

    public void updateList(List<Expense> newList) {
        this.expenseList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.tvDesc.setText(expense.getDescription());
        holder.tvCategory.setText("Category: " + (expense.getCategory() != null && !expense.getCategory().isEmpty() ? expense.getCategory() : "General"));
        String recorded = (expense.getRecordedBy() != null && !expense.getRecordedBy().isEmpty())
                ? " • by " + expense.getRecordedBy() : "";
        holder.tvDate.setText("Date: " + expense.getDate() + recorded);
        holder.tvAmount.setText(String.format(Locale.getDefault(), "$%.2f", expense.getAmount()));

        String role = RoleManager.getCurrentRole(context);
        holder.btnEdit.setVisibility(RoleManager.canManageExpenses(role) ? View.VISIBLE : View.GONE);
        holder.btnDelete.setVisibility(RoleManager.canDeleteItems(role) ? View.VISIBLE : View.GONE);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(expense);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(expense);
        });
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvDesc, tvCategory, tvDate, tvAmount;
        Button btnEdit, btnDelete;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDesc = itemView.findViewById(R.id.tvExpenseDesc);
            tvCategory = itemView.findViewById(R.id.tvExpenseCategory);
            tvDate = itemView.findViewById(R.id.tvExpenseDate);
            tvAmount = itemView.findViewById(R.id.tvExpenseAmount);
            btnEdit = itemView.findViewById(R.id.btnEditExpense);
            btnDelete = itemView.findViewById(R.id.btnDeleteExpense);
        }
    }
}
