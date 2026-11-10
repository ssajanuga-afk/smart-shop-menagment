package com.example.smartshopmanager.customer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartshopmanager.R;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.models.Customer;

import java.util.List;

public class CustomerActivity extends AppCompatActivity implements CustomerAdapter.OnCustomerListener {

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private CustomerAdapter customerAdapter;
    private EditText etSearch;
    private Button btnAddCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        dbHelper = new DatabaseHelper(this);

        etSearch = findViewById(R.id.etSearchCustomer);
        btnAddCustomer = findViewById(R.id.btnAddCustomer);
        recyclerView = findViewById(R.id.recyclerViewCustomers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadCustomers();

        btnAddCustomer.setOnClickListener(v -> showAddEditCustomerDialog(null));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<Customer> filtered = dbHelper.searchCustomers(s.toString());
                customerAdapter.updateList(filtered);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadCustomers() {
        List<Customer> customers = dbHelper.getAllCustomers();
        customerAdapter = new CustomerAdapter(this, customers, this);
        recyclerView.setAdapter(customerAdapter);
    }

    private void showAddEditCustomerDialog(Customer customer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_customer, null);
        builder.setView(view);

        EditText etName = view.findViewById(R.id.etDialogCustomerName);
        EditText etPhone = view.findViewById(R.id.etDialogCustomerPhone);
        EditText etEmail = view.findViewById(R.id.etDialogCustomerEmail);

        if (customer != null) {
            etName.setText(customer.getName());
            etPhone.setText(customer.getPhone());
            etEmail.setText(customer.getEmail());
            builder.setTitle("Edit Customer");
        } else {
            builder.setTitle("Add Customer");
        }

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Customer name is required", Toast.LENGTH_SHORT).show();
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
            loadCustomers();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onEditClick(Customer customer) {
        showAddEditCustomerDialog(customer);
    }

    @Override
    public void onDeleteClick(Customer customer) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Customer")
                .setMessage("Are you sure you want to delete " + customer.getName() + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dbHelper.deleteCustomer(customer.getCustomerId());
                    Toast.makeText(this, "Customer deleted", Toast.LENGTH_SHORT).show();
                    loadCustomers();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
