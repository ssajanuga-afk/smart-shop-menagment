package com.example.smartshopmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartshopmanager.customer.CustomerActivity;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.expense.ExpenseActivity;
import com.example.smartshopmanager.product.ProductActivity;
import com.example.smartshopmanager.reports.ReportsActivity;
import com.example.smartshopmanager.sales.SalesActivity;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvProductsCount, tvCustomersCount, tvSalesAmount, tvExpensesAmount;
    private Button btnNavProducts, btnNavSales, btnNavCustomers, btnNavExpenses, btnNavReports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        tvProductsCount = findViewById(R.id.tvDashboardProductsCount);
        tvCustomersCount = findViewById(R.id.tvDashboardCustomersCount);
        tvSalesAmount = findViewById(R.id.tvDashboardSalesAmount);
        tvExpensesAmount = findViewById(R.id.tvDashboardExpensesAmount);

        btnNavProducts = findViewById(R.id.btnNavProducts);
        btnNavSales = findViewById(R.id.btnNavSales);
        btnNavCustomers = findViewById(R.id.btnNavCustomers);
        btnNavExpenses = findViewById(R.id.btnNavExpenses);
        btnNavReports = findViewById(R.id.btnNavReports);

        btnNavProducts.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProductActivity.class)));
        btnNavSales.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SalesActivity.class)));
        btnNavCustomers.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CustomerActivity.class)));
        btnNavExpenses.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ExpenseActivity.class)));
        btnNavReports.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ReportsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboardStats();
    }

    private void updateDashboardStats() {
        int productsCount = dbHelper.getTotalProductsCount();
        int customersCount = dbHelper.getTotalCustomersCount();
        double totalSales = dbHelper.getTotalSalesAmount();
        double totalExpenses = dbHelper.getTotalExpenses();

        tvProductsCount.setText(String.valueOf(productsCount));
        tvCustomersCount.setText(String.valueOf(customersCount));
        tvSalesAmount.setText(String.format("$%.2f", totalSales));
        tvExpensesAmount.setText(String.format("$%.2f", totalExpenses));
    }
}
