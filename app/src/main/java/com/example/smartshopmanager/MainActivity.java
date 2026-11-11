package com.example.smartshopmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartshopmanager.auth.LoginActivity;
import com.example.smartshopmanager.auth.OnboardingActivity;
import com.example.smartshopmanager.auth.RoleManager;
import com.example.smartshopmanager.auth.UserManagementActivity;
import com.example.smartshopmanager.customer.CustomerActivity;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.expense.ExpenseActivity;
import com.example.smartshopmanager.models.Product;
import com.example.smartshopmanager.product.ProductActivity;
import com.example.smartshopmanager.reports.ReportsActivity;
import com.example.smartshopmanager.sales.SalesActivity;
import com.example.smartshopmanager.ui.BottomNavHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private DatabaseHelper dbHelper;
    private TextView tvWelcomeUser, tvActiveRoleBadge, tvRolePermissionSubtitle;
    private TextView tvProductsCount, tvCustomersCount;
    private TextView tvSalesAmount, tvExpensesAmount, tvNetProfitAmount;
    private TextView tvCashierShiftStats, tvLowStockSummary, tvAdminStaffCount;
    private View cardFinancialOverview, rowDashboardNetProfit, cardCashierShift, cardLowStockAlert, cardAdminControls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(this);

        if (!RoleManager.refreshAndValidateSession(this, dbHelper)) {
            openLoginScreen(false);
            return;
        }

        setContentView(R.layout.activity_main);

        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        tvActiveRoleBadge = findViewById(R.id.tvActiveRoleBadge);
        tvRolePermissionSubtitle = findViewById(R.id.tvRolePermissionSubtitle);
        tvProductsCount = findViewById(R.id.tvDashboardProductsCount);
        tvCustomersCount = findViewById(R.id.tvDashboardCustomersCount);
        tvSalesAmount = findViewById(R.id.tvDashboardSalesAmount);
        tvExpensesAmount = findViewById(R.id.tvDashboardExpensesAmount);
        tvNetProfitAmount = findViewById(R.id.tvDashboardNetProfitAmount);
        tvCashierShiftStats = findViewById(R.id.tvCashierShiftStats);
        tvLowStockSummary = findViewById(R.id.tvLowStockSummary);
        tvAdminStaffCount = findViewById(R.id.tvAdminStaffCount);

        cardFinancialOverview = findViewById(R.id.cardFinancialOverview);
        rowDashboardNetProfit = findViewById(R.id.rowDashboardNetProfit);
        cardCashierShift = findViewById(R.id.cardCashierShift);
        cardLowStockAlert = findViewById(R.id.cardLowStockAlert);
        cardAdminControls = findViewById(R.id.cardAdminControls);

        findViewById(R.id.cardMetricProducts).setOnClickListener(v -> switchToTab(ProductActivity.class));
        findViewById(R.id.cardMetricCustomers).setOnClickListener(v -> switchToTab(CustomerActivity.class));
        findViewById(R.id.rowDashboardSales).setOnClickListener(v -> switchToTab(ReportsActivity.class));
        findViewById(R.id.rowDashboardExpenses).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ExpenseActivity.class)));
        findViewById(R.id.rowDashboardNetProfit).setOnClickListener(v -> switchToTab(ReportsActivity.class));
        cardLowStockAlert.setOnClickListener(v -> switchToTab(ProductActivity.class));

        Button btnCashierOpenPos = findViewById(R.id.btnCashierOpenPos);
        if (btnCashierOpenPos != null) {
            btnCashierOpenPos.setOnClickListener(v -> switchToTab(SalesActivity.class));
        }

        Button btnManageStaff = findViewById(R.id.btnManageStaff);
        if (btnManageStaff != null) {
            btnManageStaff.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, UserManagementActivity.class)));
        }

        if (tvActiveRoleBadge != null) {
            tvActiveRoleBadge.setOnClickListener(v -> showAccountActionsDialog());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!RoleManager.refreshAndValidateSession(this, dbHelper)) {
            openLoginScreen(false);
            return;
        }
        BottomNavHelper.setup(this, R.id.nav_home);
        updateRoleDisplay();
        updateDashboardStats();
    }

    private void switchToTab(Class<?> destination) {
        BottomNavHelper.navigateToTab(this, destination);
    }

    private void updateRoleDisplay() {
        String role = RoleManager.getCurrentRole(this);
        String fullName = RoleManager.getCurrentFullName(this);

        tvWelcomeUser.setText("Hi, " + fullName + " 👋");
        tvActiveRoleBadge.setText(role + " ▾");
        tvRolePermissionSubtitle.setText(RoleManager.getRoleDescription(role));

        boolean canSeeFinancial = RoleManager.canViewFinancialOverview(role);
        boolean canSeeProfit = RoleManager.canViewNetProfit(role);
        boolean canManageUsers = RoleManager.canManageUsers(role);

        cardFinancialOverview.setVisibility(canSeeFinancial ? View.VISIBLE : View.GONE);
        rowDashboardNetProfit.setVisibility(canSeeProfit ? View.VISIBLE : View.GONE);
        cardCashierShift.setVisibility(canSeeFinancial ? View.GONE : View.VISIBLE);
        cardAdminControls.setVisibility(canManageUsers ? View.VISIBLE : View.GONE);
    }

    private void updateDashboardStats() {
        String role = RoleManager.getCurrentRole(this);
        int productsCount = dbHelper.getTotalProductsCount();
        int customersCount = dbHelper.getTotalCustomersCount();

        tvProductsCount.setText(String.valueOf(productsCount));
        tvCustomersCount.setText(String.valueOf(customersCount));

        if (RoleManager.canViewFinancialOverview(role)) {
            double totalSales = dbHelper.getTotalSalesAmount();
            double totalExpenses = dbHelper.getTotalExpenses();
            double cogs = dbHelper.getTotalCostOfGoodsSold();
            double netProfit = totalSales - totalExpenses - cogs;

            tvSalesAmount.setText(String.format(Locale.getDefault(), "$%.2f", totalSales));
            tvExpensesAmount.setText(String.format(Locale.getDefault(), "$%.2f", totalExpenses));
            tvNetProfitAmount.setText(String.format(Locale.getDefault(), "$%.2f", netProfit));

            List<Product> lowStock = dbHelper.getLowStockProducts(LOW_STOCK_THRESHOLD);
            if (!lowStock.isEmpty()) {
                cardLowStockAlert.setVisibility(View.VISIBLE);
                tvLowStockSummary.setText(String.format(Locale.getDefault(),
                        "⚠️ %d product(s) low in stock (≤ %d)", lowStock.size(), LOW_STOCK_THRESHOLD));
            } else {
                cardLowStockAlert.setVisibility(View.GONE);
            }
        } else {
            cardLowStockAlert.setVisibility(View.GONE);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            int userId = RoleManager.getCurrentUserId(this);
            int shiftCount = dbHelper.getTodaySalesCountByCashier(userId, today);
            double shiftTotal = dbHelper.getTodaySalesTotalByCashier(userId, today);
            tvCashierShiftStats.setText(String.format(Locale.getDefault(),
                    "%d sale(s) processed today • $%.2f", shiftCount, shiftTotal));
        }

        if (RoleManager.canManageUsers(role)) {
            int staffCount = dbHelper.getAllUsers().size();
            tvAdminStaffCount.setText(staffCount + " staff account(s) configured");
        }
    }

    private void showAccountActionsDialog() {
        String currentName = RoleManager.getCurrentFullName(this);
        String currentRole = RoleManager.getCurrentRole(this);
        String[] options = {"Switch Staff Account (PIN)", "View App Onboarding", "Log Out"};

        new AlertDialog.Builder(this)
                .setTitle(currentName + " (" + currentRole + ")")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openLoginScreen(true);
                    } else if (which == 1) {
                        Intent intent = new Intent(this, OnboardingActivity.class);
                        intent.putExtra(OnboardingActivity.EXTRA_REPLAY, true);
                        startActivity(intent);
                    } else {
                        RoleManager.clearSession(this);
                        openLoginScreen(false);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openLoginScreen(boolean switchMode) {
        Intent intent = new Intent(this, LoginActivity.class);
        if (switchMode) {
            intent.putExtra(LoginActivity.EXTRA_SWITCH_ACCOUNT, true);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        startActivity(intent);
        if (!switchMode) {
            finish();
        }
    }
}
