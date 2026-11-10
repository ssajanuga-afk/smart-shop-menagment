package com.example.smartshopmanager.reports;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartshopmanager.R;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.models.Sale;
import com.example.smartshopmanager.sales.SalesHistoryAdapter;

import java.util.List;

public class ReportsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvRevenue, tvExpenses, tvProfit;
    private RecyclerView recyclerViewSalesHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        dbHelper = new DatabaseHelper(this);

        tvRevenue = findViewById(R.id.tvReportRevenue);
        tvExpenses = findViewById(R.id.tvReportExpenses);
        tvProfit = findViewById(R.id.tvReportProfit);
        recyclerViewSalesHistory = findViewById(R.id.recyclerViewSalesHistory);
        recyclerViewSalesHistory.setLayoutManager(new LinearLayoutManager(this));

        double totalRevenue = dbHelper.getTotalSalesAmount();
        double totalExpenses = dbHelper.getTotalExpenses();
        double cogs = dbHelper.getTotalCostOfGoodsSold();
        double netProfit = totalRevenue - totalExpenses - cogs;

        tvRevenue.setText(String.format("$%.2f", totalRevenue));
        tvExpenses.setText(String.format("$%.2f", totalExpenses));
        tvProfit.setText(String.format("$%.2f", netProfit));

        List<Sale> sales = dbHelper.getAllSales();
        SalesHistoryAdapter adapter = new SalesHistoryAdapter(this, sales);
        recyclerViewSalesHistory.setAdapter(adapter);
    }
}
