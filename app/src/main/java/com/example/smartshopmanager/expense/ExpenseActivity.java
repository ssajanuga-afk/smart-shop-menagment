package com.example.smartshopmanager.expense;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartshopmanager.R;
import com.example.smartshopmanager.auth.RoleManager;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.models.Expense;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseActivity extends AppCompatActivity implements ExpenseAdapter.OnExpenseListener {

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private ExpenseAdapter expenseAdapter;
    private Button btnAddExpense;
    private TextView tvTotalExpensesAmount, tvEmptyExpenses;
    private EditText etSearchExpense;
    private Spinner spinnerFilterCategory;

    private List<String> categoryOptions = new ArrayList<>();
    private String selectedCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(this);

        if (!RoleManager.requireAccess(this, dbHelper,
                RoleManager.canManageExpenses(RoleManager.getCurrentRole(this)))) {
            return;
        }

        setContentView(R.layout.activity_expense);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        btnAddExpense = findViewById(R.id.btnAddExpense);
        tvTotalExpensesAmount = findViewById(R.id.tvTotalExpensesAmount);
        tvEmptyExpenses = findViewById(R.id.tvEmptyExpenses);
        etSearchExpense = findViewById(R.id.etSearchExpense);
        spinnerFilterCategory = findViewById(R.id.spinnerFilterExpenseCategory);
        recyclerView = findViewById(R.id.recyclerViewExpenses);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnAddExpense.setOnClickListener(v -> {
            if (!RoleManager.canManageExpenses(RoleManager.getCurrentRole(this))) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                return;
            }
            showAddEditExpenseDialog(null);
        });

        spinnerFilterCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < categoryOptions.size()) {
                    selectedCategory = categoryOptions.get(position);
                    loadExpensesList();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etSearchExpense.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadExpensesList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!RoleManager.requireAccess(this, dbHelper,
                RoleManager.canManageExpenses(RoleManager.getCurrentRole(this)))) {
            return;
        }
        loadCategoryFilter();
        loadExpensesList();
    }

    private void loadCategoryFilter() {
        String previous = selectedCategory;
        categoryOptions = dbHelper.getExpenseCategories();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterCategory.setAdapter(adapter);

        int idx = categoryOptions.indexOf(previous);
        if (idx >= 0) {
            spinnerFilterCategory.setSelection(idx, false);
        } else {
            selectedCategory = "All";
        }
    }

    private void loadExpensesList() {
        String keyword = etSearchExpense.getText() != null ? etSearchExpense.getText().toString() : "";
        List<Expense> expenses = dbHelper.getExpensesFiltered(keyword, selectedCategory);
        if (expenseAdapter == null) {
            expenseAdapter = new ExpenseAdapter(this, expenses, this);
            recyclerView.setAdapter(expenseAdapter);
        } else {
            expenseAdapter.updateList(expenses);
        }
        tvEmptyExpenses.setVisibility(expenses.isEmpty() ? View.VISIBLE : View.GONE);

        double total = dbHelper.getTotalExpenses();
        tvTotalExpensesAmount.setText(String.format(Locale.getDefault(), "$%.2f", total));
    }

    private void showAddEditExpenseDialog(Expense expense) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null);

        EditText etDesc = view.findViewById(R.id.etDialogExpenseDesc);
        EditText etAmount = view.findViewById(R.id.etDialogExpenseAmount);
        EditText etCategory = view.findViewById(R.id.etDialogExpenseCategory);
        EditText etDate = view.findViewById(R.id.etDialogExpenseDate);

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etDate.setText(currentDate);
        etDate.setFocusable(false);
        etDate.setClickable(true);
        etDate.setOnClickListener(v -> showDatePicker(etDate));

        if (expense != null) {
            etDesc.setText(expense.getDescription());
            etAmount.setText(String.valueOf(expense.getAmount()));
            etCategory.setText(expense.getCategory());
            etDate.setText(expense.getDate());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(expense != null ? "Edit Expense" : "Add Expense")
                .setView(view)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String desc = etDesc.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String date = etDate.getText().toString().trim();

            if (desc.isEmpty()) {
                etDesc.setError("Description is required");
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                etAmount.setError("Enter a positive amount");
                return;
            }
            if (category.isEmpty()) {
                category = "General";
            }
            if (date.isEmpty()) {
                date = currentDate;
            }

            if (expense == null) {
                String recordedBy = RoleManager.getCurrentFullName(this);
                dbHelper.addExpense(new Expense(desc, amount, category, date, recordedBy));
                Toast.makeText(this, "Expense added", Toast.LENGTH_SHORT).show();
            } else {
                expense.setDescription(desc);
                expense.setAmount(amount);
                expense.setCategory(category);
                expense.setDate(date);
                dbHelper.updateExpense(expense);
                Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show();
            }
            loadCategoryFilter();
            loadExpensesList();
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void showDatePicker(EditText target) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date parsed = sdf.parse(target.getText().toString().trim());
            if (parsed != null) cal.setTime(parsed);
        } catch (ParseException ignored) {}

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(year, month, dayOfMonth);
            target.setText(sdf.format(chosen.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override
    public void onEditClick(Expense expense) {
        if (!RoleManager.canManageExpenses(RoleManager.getCurrentRole(this))) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            return;
        }
        showAddEditExpenseDialog(expense);
    }

    @Override
    public void onDeleteClick(Expense expense) {
        if (!RoleManager.canDeleteItems(RoleManager.getCurrentRole(this))) {
            Toast.makeText(this, "Admin permission required to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete \"" + expense.getDescription() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteExpense(expense.getExpenseId());
                    Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show();
                    loadCategoryFilter();
                    loadExpensesList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
