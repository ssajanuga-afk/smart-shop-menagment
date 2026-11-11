package com.example.smartshopmanager.customer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
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
import com.example.smartshopmanager.auth.RoleManager;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.models.Customer;
import com.example.smartshopmanager.models.Sale;
import com.example.smartshopmanager.sales.SaleReceiptDialogHelper;
import com.example.smartshopmanager.ui.BottomNavHelper;

import java.util.List;
import java.util.Locale;

public class CustomerActivity extends AppCompatActivity implements CustomerAdapter.OnCustomerListener {

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private CustomerAdapter customerAdapter;
    private EditText etSearch;
    private Button btnAddCustomer;
    private TextView tvEmptyCustomers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(this);

        if (!RoleManager.requireAccess(this, dbHelper, true)) {
            return;
        }

        setContentView(R.layout.activity_customer);

        etSearch = findViewById(R.id.etSearchCustomer);
        btnAddCustomer = findViewById(R.id.btnAddCustomer);
        tvEmptyCustomers = findViewById(R.id.tvEmptyCustomers);
        recyclerView = findViewById(R.id.recyclerViewCustomers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnAddCustomer.setOnClickListener(v -> {
            if (!RoleManager.canAddEditCustomers(RoleManager.getCurrentRole(this))) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                return;
            }
            showAddEditCustomerDialog(null);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadCustomers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!RoleManager.requireAccess(this, dbHelper, true)) {
            return;
        }
        BottomNavHelper.setup(this, R.id.nav_customers);
        btnAddCustomer.setVisibility(
                RoleManager.canAddEditCustomers(RoleManager.getCurrentRole(this)) ? View.VISIBLE : View.GONE);
        String query = etSearch.getText() != null ? etSearch.getText().toString() : "";
        loadCustomers(query);
    }

    private void loadCustomers(String keyword) {
        List<Customer> customers = dbHelper.searchCustomers(keyword);
        if (customerAdapter == null) {
            customerAdapter = new CustomerAdapter(this, customers, this);
            recyclerView.setAdapter(customerAdapter);
        } else {
            customerAdapter.updateList(customers);
        }
        tvEmptyCustomers.setVisibility(customers.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAddEditCustomerDialog(Customer customer) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_customer, null);

        EditText etName = view.findViewById(R.id.etDialogCustomerName);
        EditText etPhone = view.findViewById(R.id.etDialogCustomerPhone);
        EditText etEmail = view.findViewById(R.id.etDialogCustomerEmail);

        if (customer != null) {
            etName.setText(customer.getName());
            etPhone.setText(customer.getPhone());
            etEmail.setText(customer.getEmail());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(customer != null ? "Edit Customer" : "Add Customer")
                .setView(view)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Customer name is required");
                return;
            }
            if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email address");
                return;
            }

            if (customer == null) {
                dbHelper.addCustomer(new Customer(name, phone, email));
                Toast.makeText(this, "Customer added", Toast.LENGTH_SHORT).show();
            } else {
                customer.setName(name);
                customer.setPhone(phone);
                customer.setEmail(email);
                dbHelper.updateCustomer(customer);
                Toast.makeText(this, "Customer updated", Toast.LENGTH_SHORT).show();
            }
            String query = etSearch.getText() != null ? etSearch.getText().toString() : "";
            loadCustomers(query);
            dialog.dismiss();
        }));

        dialog.show();
    }

    @Override
    public void onCustomerClick(Customer customer) {
        List<Sale> sales = dbHelper.getSalesByCustomer(customer.getCustomerId());
        double totalSpent = dbHelper.getCustomerTotalSpent(customer.getCustomerId());

        if (sales.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(customer.getName())
                    .setMessage("No purchases recorded yet for this customer.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String[] items = new String[sales.size()];
        for (int i = 0; i < sales.size(); i++) {
            Sale s = sales.get(i);
            items[i] = String.format(Locale.getDefault(),
                    "Sale #%d • $%.2f (%s) — %s",
                    s.getSaleId(), s.getTotalAmount(),
                    s.getPaymentMethod() != null ? s.getPaymentMethod() : "Cash",
                    s.getSaleDate());
        }

        new AlertDialog.Builder(this)
                .setTitle(String.format(Locale.getDefault(), "%s • Spent: $%.2f", customer.getName(), totalSpent))
                .setItems(items, (dialog, which) -> SaleReceiptDialogHelper.showReceiptDialog(
                        this, dbHelper, sales.get(which), () -> {
                            String query = etSearch.getText() != null ? etSearch.getText().toString() : "";
                            loadCustomers(query);
                        }))
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    public void onEditClick(Customer customer) {
        if (!RoleManager.canAddEditCustomers(RoleManager.getCurrentRole(this))) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            return;
        }
        showAddEditCustomerDialog(customer);
    }

    @Override
    public void onDeleteClick(Customer customer) {
        if (!RoleManager.canDeleteItems(RoleManager.getCurrentRole(this))) {
            Toast.makeText(this, "Admin permission required to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete Customer")
                .setMessage("Are you sure you want to delete " + customer.getName() + "? Past sales will remain as Walk-in.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteCustomer(customer.getCustomerId());
                    Toast.makeText(this, "Customer deleted", Toast.LENGTH_SHORT).show();
                    String query = etSearch.getText() != null ? etSearch.getText().toString() : "";
                    loadCustomers(query);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
