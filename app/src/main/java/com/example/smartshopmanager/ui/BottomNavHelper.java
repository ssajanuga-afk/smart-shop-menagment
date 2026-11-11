package com.example.smartshopmanager.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;

import com.example.smartshopmanager.MainActivity;
import com.example.smartshopmanager.R;
import com.example.smartshopmanager.auth.RoleManager;
import com.example.smartshopmanager.customer.CustomerActivity;
import com.example.smartshopmanager.product.ProductActivity;
import com.example.smartshopmanager.reports.ReportsActivity;
import com.example.smartshopmanager.sales.SalesActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Shared role-aware BottomNavigationView binder used across top-level activities.
 */
public final class BottomNavHelper {

    private BottomNavHelper() {}

    public static void setup(Activity activity, int activeItemId) {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigation);
        if (bottomNav == null) return;

        String role = RoleManager.getCurrentRole(activity);
        MenuItem financeItem = bottomNav.getMenu().findItem(R.id.nav_reports);
        if (financeItem != null) {
            financeItem.setVisible(RoleManager.canViewFinancialReports(role));
        }

        // Detach listener before syncing selected item so onResume never triggers navigation
        bottomNav.setOnItemSelectedListener(null);
        if (bottomNav.getMenu().findItem(activeItemId) != null
                && bottomNav.getMenu().findItem(activeItemId).isVisible()) {
            bottomNav.setSelectedItemId(activeItemId);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == activeItemId) {
                return true;
            }
            Class<?> target = resolveDestination(itemId, role);
            if (target == null) {
                return false;
            }
            navigateToTab(activity, target);
            return true;
        });
    }

    public static void navigateToTab(Activity activity, Class<?> destination) {
        Intent intent = new Intent(activity, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
        UiUtils.disableOpenTransition(activity);
    }

    private static Class<?> resolveDestination(int itemId, String role) {
        if (itemId == R.id.nav_home) {
            return MainActivity.class;
        } else if (itemId == R.id.nav_products) {
            return ProductActivity.class;
        } else if (itemId == R.id.nav_sales) {
            return SalesActivity.class;
        } else if (itemId == R.id.nav_customers) {
            return CustomerActivity.class;
        } else if (itemId == R.id.nav_reports && RoleManager.canViewFinancialReports(role)) {
            return ReportsActivity.class;
        }
        return null;
    }
}
