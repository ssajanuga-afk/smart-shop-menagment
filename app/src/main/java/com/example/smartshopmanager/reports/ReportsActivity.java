package com.example.smartshopmanager.reports;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartshopmanager.R;
import com.example.smartshopmanager.auth.RoleManager;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.expense.ExpenseActivity;
import com.example.smartshopmanager.models.Sale;
import com.example.smartshopmanager.sales.SaleReceiptDialogHelper;
import com.example.smartshopmanager.sales.SalesHistoryAdapter;
import com.example.smartshopmanager.ui.BottomNavHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvRevenue, tvExpenses, tvProfit, tvCogsBreakdown, tvEmptyReportSales;
    private View cardNetProfit;
    private RecyclerView recyclerViewSalesHistory;
    private SalesHistoryAdapter salesAdapter;
    private EditText etSearchSales;
    private Spinner spinnerPaymentFilter;
    private String selectedPaymentMethod = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(this);

        if (!RoleManager.requireAccess(this, dbHelper,
                RoleManager.canViewFinancialReports(RoleManager.getCurrentRole(this)))) {
            return;
        }

        setContentView(R.layout.activity_reports);

        tvRevenue = findViewById(R.id.tvReportRevenue);
        tvExpenses = findViewById(R.id.tvReportExpenses);
        tvProfit = findViewById(R.id.tvReportProfit);
        tvCogsBreakdown = findViewById(R.id.tvReportCogsBreakdown);
        tvEmptyReportSales = findViewById(R.id.tvEmptyReportSales);
        cardNetProfit = findViewById(R.id.cardNetProfit);
        etSearchSales = findViewById(R.id.etSearchReportSales);
        spinnerPaymentFilter = findViewById(R.id.spinnerReportPaymentFilter);
        recyclerViewSalesHistory = findViewById(R.id.recyclerViewSalesHistory);
        recyclerViewSalesHistory.setLayoutManager(new LinearLayoutManager(this));

        Button btnManageExpenses = findViewById(R.id.btnReportsManageExpenses);
        btnManageExpenses.setOnClickListener(v -> startActivity(new Intent(this, ExpenseActivity.class)));
        findViewById(R.id.cardReportExpenses).setOnClickListener(v ->
                startActivity(new Intent(this, ExpenseActivity.class)));

        String[] paymentMethods = {"All", "Cash", "Card", "Bank Transfer"};
        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, paymentMethods);
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaymentFilter.setAdapter(methodAdapter);

        spinnerPaymentFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPaymentMethod = paymentMethods[position];
                loadFilteredSales();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etSearchSales.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadFilteredSales();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        salesAdapter = new SalesHistoryAdapter(this, new ArrayList<>(), sale ->
                SaleReceiptDialogHelper.showReceiptDialog(this, dbHelper, sale, this::refreshReportData));
        recyclerViewSalesHistory.setAdapter(salesAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!RoleManager.requireAccess(this, dbHelper,
                RoleManager.canViewFinancialReports(RoleManager.getCurrentRole(this)))) {
            return;
        }
        BottomNavHelper.setup(this, R.id.nav_reports);
        refreshReportData();
    }

    private void refreshReportData() {
        String role = RoleManager.getCurrentRole(this);
        double totalRevenue = dbHelper.getTotalSalesAmount();
        double totalExpenses = dbHelper.getTotalExpenses();
        double cogs = dbHelper.getTotalCostOfGoodsSold();
        double netProfit = totalRevenue - totalExpenses - cogs;

        tvRevenue.setText(String.format(Locale.getDefault(), "$%.2f", totalRevenue));
        tvExpenses.setText(String.format(Locale.getDefault(), "$%.2f", totalExpenses));

        boolean showNetProfit = RoleManager.canViewNetProfit(role);
        cardNetProfit.setVisibility(showNetProfit ? View.VISIBLE : View.GONE);
        if (showNetProfit) {
            tvProfit.setText(String.format(Locale.getDefault(), "$%.2f", netProfit));
            tvCogsBreakdown.setText(String.format(Locale.getDefault(),
                    "(Revenue - Expenses - COGS: $%.2f)", cogs));
        }

        loadFilteredSales();
    }

    private void loadFilteredSales() {
        String keyword = etSearchSales.getText() != null ? etSearchSales.getText().toString() : "";
        List<Sale> sales = dbHelper.getSalesFiltered(selectedPaymentMethod, keyword, 0);
        salesAdapter.updateList(sales);
        tvEmptyReportSales.setVisibility(sales.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
