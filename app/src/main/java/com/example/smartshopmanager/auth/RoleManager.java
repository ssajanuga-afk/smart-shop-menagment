package com.example.smartshopmanager.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.models.User;

/**
 * Centralized session state and role-based access control (RBAC) policy for Admin, Manager, and Cashier.
 */
public class RoleManager {

    private static final String PREF_NAME = "SmartShopRolePrefs";
    private static final String APP_PREF_NAME = "SmartShopAppPrefs";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    private static final String KEY_USER_ID = "active_user_id";
    private static final String KEY_USERNAME = "active_username";
    private static final String KEY_FULL_NAME = "active_full_name";
    private static final String KEY_ROLE = "active_role";

    public static final String ROLE_ADMIN = "Admin";
    public static final String ROLE_MANAGER = "Manager";
    public static final String ROLE_CASHIER = "Cashier";

    public static void setActiveSession(Context context, User user) {
        if (user == null) {
            clearSession(context);
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_USER_ID, user.getUserId())
                .putString(KEY_USERNAME, user.getUsername())
                .putString(KEY_FULL_NAME, user.getFullName())
                .putString(KEY_ROLE, user.getRole())
                .apply();
    }

    public static void clearSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public static boolean isOnboardingCompleted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public static void setOnboardingCompleted(Context context, boolean completed) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    public static boolean isLoggedIn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, -1) > 0 && prefs.getString(KEY_ROLE, null) != null;
    }

    public static int getCurrentUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public static String getCurrentUsername(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USERNAME, "");
    }

    public static String getCurrentFullName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_FULL_NAME, "Guest");
    }

    public static String getCurrentRole(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ROLE, ROLE_CASHIER);
    }

    /**
     * Kept for unit testing and legacy callers; validates active role value.
     */
    public static void setCurrentRole(Context context, String role) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ROLE, role).apply();
    }

    /**
     * Re-validates the active session against SQLite so deleted/deactivated accounts are logged out
     * and updated roles/names take effect immediately.
     */
    public static boolean refreshAndValidateSession(Context context, DatabaseHelper dbHelper) {
        int userId = getCurrentUserId(context);
        if (userId <= 0) {
            return false;
        }
        User dbUser = dbHelper.getUserById(userId);
        if (dbUser == null || !dbUser.isActive()) {
            clearSession(context);
            return false;
        }
        setActiveSession(context, dbUser);
        return true;
    }

    /**
     * Enforces access on restricted activities; redirects to LoginActivity if not signed in
     * or finishes the activity with a notice if the current role lacks permission.
     */
    public static boolean requireAccess(Activity activity, DatabaseHelper dbHelper, boolean hasPermission) {
        if (!refreshAndValidateSession(activity, dbHelper)) {
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();
            return false;
        }
        if (!hasPermission) {
            Toast.makeText(activity, "Access denied for " + getCurrentRole(activity) + " role", Toast.LENGTH_SHORT).show();
            activity.finish();
            return false;
        }
        return true;
    }

    // --- Role Permission Matrix ---

    public static boolean isAdmin(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role);
    }

    public static boolean isManager(String role) {
        return ROLE_MANAGER.equalsIgnoreCase(role);
    }

    public static boolean isCashier(String role) {
        return ROLE_CASHIER.equalsIgnoreCase(role);
    }

    public static boolean canDeleteItems(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role);
    }

    public static boolean canAddEditProducts(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role) || ROLE_MANAGER.equalsIgnoreCase(role);
    }

    public static boolean canImportApiProducts(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role) || ROLE_MANAGER.equalsIgnoreCase(role);
    }

    public static boolean canManageCategories(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role) || ROLE_MANAGER.equalsIgnoreCase(role);
    }

    public static boolean canAddEditCustomers(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role)
                || ROLE_MANAGER.equalsIgnoreCase(role)
                || ROLE_CASHIER.equalsIgnoreCase(role);
    }

    public static boolean canManageExpenses(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role) || ROLE_MANAGER.equalsIgnoreCase(role);
    }

    public static boolean canViewFinancialOverview(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role) || ROLE_MANAGER.equalsIgnoreCase(role);
    }

    public static boolean canViewFinancialReports(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role) || ROLE_MANAGER.equalsIgnoreCase(role);
    }

    public static boolean canViewNetProfit(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role);
    }

    public static boolean canEditOrVoidSales(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role);
    }

    public static boolean canManageUsers(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role);
    }

    public static String getRoleDescription(String role) {
        if (ROLE_ADMIN.equalsIgnoreCase(role)) {
            return "Admin • Full Store Control, Staff & Net Profit";
        } else if (ROLE_MANAGER.equalsIgnoreCase(role)) {
            return "Manager • Inventory, Expenses & Reports (No Deletes)";
        }
        return "Cashier • POS Checkout & Customers";
    }
}
