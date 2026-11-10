package com.example.smartshopmanager.expense;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartshopmanager.R;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.models.Expense;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseActivity extends AppCompatActivity implements ExpenseAdapter.OnExpenseListener {

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private ExpenseAdapter expenseAdapter;
    private Button btnAddExpense;
    private TextView tvTotalExpensesAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        dbHelper = new DatabaseHelper(this);

        btnAddExpense = findViewById(R.id.btnAddExpense);
        tvTotalExpensesAmount = findViewById(R.id.tvTotalExpensesAmount);
        recyclerView = findViewById(R.id.recyclerViewExpenses);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadExpenses();

        btnAddExpense.setOnClickListener(v -> showAddEditExpenseDialog(null));
    }

    private void loadExpenses() {
        List<Expense> expenses = dbHelper.getAllExpenses();
        expenseAdapter = new ExpenseAdapter(this, expenses, this);
        recyclerView.setAdapter(expenseAdapter);

        double total = dbHelper.getTotalExpenses();
        tvTotalExpensesAmount.setText(String.format("$%.2f", total));
    }

    private void showAddEditExpenseDialog(Expense expense) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null);
        builder.setView(view);

        EditText etDesc = view.findViewById(R.id.etDialogExpenseDesc);
        EditText etAmount = view.findViewById(R.id.etDialogExpenseAmount);
        EditText etCategory = view.findViewById(R.id.etDialogExpenseCategory);
        EditText etDate = view.findViewById(R.id.etDialogExpenseDate);

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etDate.setText(currentDate);

        if (expense != null) {
            etDesc.setText(expense.getDescription());
            etAmount.setText(String.valueOf(expense.getAmount()));
            etCategory.setText(expense.getCategory());
            etDate.setText(expense.getDate());
            builder.setTitle("Edit Expense");
        } else {
            builder.setTitle("Add Expense");
        }

        builder.setPositiveButton("Save", (dialog, which) -> {
            String desc = etDesc.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String date = etDate.getText().toString().trim();

            if (desc.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Description and amount are required", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            if (date.isEmpty()) date = currentDate;

            if (expense == null) {
                dbHelper.addExpense(new Expense(desc, amount, category, date));
                Toast.makeText(this, "Expense added", Toast.LENGTH_SHORT).show();
            } else {
                expense.setDescription(desc);
                expense.setAmount(amount);
                expense.setCategory(category);
                expense.setDate(date);
                dbHelper.updateExpense(expense);
                Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show();
            }
            loadExpenses();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onEditClick(Expense expense) {
        showAddEditExpenseDialog(expense);
    }

    @Override
    public void onDeleteClick(Expense expense) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dbHelper.deleteExpense(expense.getExpenseId());
                    Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show();
                    loadExpenses();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
