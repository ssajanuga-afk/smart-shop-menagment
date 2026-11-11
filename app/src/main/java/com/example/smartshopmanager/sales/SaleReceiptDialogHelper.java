package com.example.smartshopmanager.sales;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.smartshopmanager.R;
import com.example.smartshopmanager.auth.RoleManager;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.models.Customer;
import com.example.smartshopmanager.models.Sale;
import com.example.smartshopmanager.models.SaleItem;

import java.util.List;
import java.util.Locale;

/**
 * Shared receipt detail, edit, and void/delete dialog for sales records across POS History,
 * Customer Purchase History, and Reports.
 */
public final class SaleReceiptDialogHelper {

    public interface OnSaleChangedListener {
        void onSaleChanged();
    }

    private SaleReceiptDialogHelper() {}

    public static void showReceiptDialog(Activity activity, DatabaseHelper dbHelper, Sale sale, OnSaleChangedListener listener) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_sale_receipt, null);
        TextView tvHeader = view.findViewById(R.id.tvReceiptHeader);
        TextView tvMeta = view.findViewById(R.id.tvReceiptMeta);
        LinearLayout layoutItems = view.findViewById(R.id.layoutReceiptItems);
        TextView tvTotals = view.findViewById(R.id.tvReceiptTotals);

        String customerDisplay = (sale.getCustomerName() != null && !sale.getCustomerName().isEmpty())
                ? sale.getCustomerName() : "Walk-in Customer";
        String cashierDisplay = (sale.getCashierName() != null && !sale.getCashierName().isEmpty())
                ? sale.getCashierName() : "Staff";
        String methodDisplay = (sale.getPaymentMethod() != null && !sale.getPaymentMethod().isEmpty())
                ? sale.getPaymentMethod() : "Cash";

        tvHeader.setText("Receipt #" + sale.getSaleId());
        tvMeta.setText(String.format(Locale.getDefault(),
                "Date: %s\nCustomer: %s\nPayment: %s\nCashier: %s",
                sale.getSaleDate(), customerDisplay, methodDisplay, cashierDisplay));

        List<SaleItem> items = dbHelper.getSaleItems(sale.getSaleId());
        if (items.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText("No itemized lines recorded for this sale.");
            empty.setTextColor(ContextCompat.getColor(activity, R.color.text_muted));
            empty.setTextSize(12f);
            layoutItems.addView(empty);
        } else {
            for (SaleItem item : items) {
                LinearLayout row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 6, 0, 6);

                TextView left = new TextView(activity);
                LinearLayout.LayoutParams lpLeft = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                left.setLayoutParams(lpLeft);
                left.setText(String.format(Locale.getDefault(), "%s  (%d × $%.2f)",
                        item.getProductName(), item.getQuantity(), item.getUnitPrice()));
                left.setTextColor(ContextCompat.getColor(activity, R.color.text_primary));
                left.setTextSize(13f);

                TextView right = new TextView(activity);
                right.setText(String.format(Locale.getDefault(), "$%.2f", item.getTotalPrice()));
                right.setTextColor(ContextCompat.getColor(activity, R.color.success));
                right.setTextSize(13f);
                right.setTypeface(null, android.graphics.Typeface.BOLD);

                row.addView(left);
                row.addView(right);
                layoutItems.addView(row);
            }
        }

        double subtotal = sale.getSubtotal() > 0 ? sale.getSubtotal() : sale.getTotalAmount();
        tvTotals.setText(String.format(Locale.getDefault(),
                "Subtotal: $%.2f   |   Discount: %.1f%%\nFinal Total: $%.2f",
                subtotal, sale.getDiscountPercent(), sale.getTotalAmount()));

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setView(view)
                .setPositiveButton("Close", null);

        String role = RoleManager.getCurrentRole(activity);
        if (RoleManager.canEditOrVoidSales(role)) {
            builder.setNeutralButton("Edit Info", (d, w) ->
                    showEditSaleDialog(activity, dbHelper, sale, listener));
            builder.setNegativeButton("Void / Delete", (d, w) ->
                    confirmVoidSale(activity, dbHelper, sale, listener));
        }

        builder.show();
    }

    private static void showEditSaleDialog(Activity activity, DatabaseHelper dbHelper, Sale sale, OnSaleChangedListener listener) {
        if (!RoleManager.canEditOrVoidSales(RoleManager.getCurrentRole(activity))) {
            Toast.makeText(activity, "Admin permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_checkout, null);
        Spinner spinnerCust = view.findViewById(R.id.spinnerCheckoutCustomer);
        RadioGroup rgPayment = view.findViewById(R.id.rgPaymentMethod);
        RadioButton rbCash = view.findViewById(R.id.rbCash);
        RadioButton rbCard = view.findViewById(R.id.rbCard);
        RadioButton rbBank = view.findViewById(R.id.rbBank);
        TextView tvTotal = view.findViewById(R.id.tvCheckoutTotal);

        List<Customer> customers = dbHelper.getAllCustomers();
        customers.add(0, new Customer(0, "Walk-in Customer", "", ""));
        ArrayAdapter<Customer> custAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, customers);
        custAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCust.setAdapter(custAdapter);

        for (int i = 0; i < customers.size(); i++) {
            if (customers.get(i).getCustomerId() == sale.getCustomerId()) {
                spinnerCust.setSelection(i);
                break;
            }
        }

        String method = sale.getPaymentMethod() != null ? sale.getPaymentMethod() : "Cash";
        if ("Card".equalsIgnoreCase(method)) {
            rbCard.setChecked(true);
        } else if ("Bank Transfer".equalsIgnoreCase(method)) {
            rbBank.setChecked(true);
        } else {
            rbCash.setChecked(true);
        }
        tvTotal.setText(String.format(Locale.getDefault(), "Sale Total: $%.2f", sale.getTotalAmount()));

        new AlertDialog.Builder(activity)
                .setTitle("Edit Sale #" + sale.getSaleId())
                .setView(view)
                .setPositiveButton("Save", (d, w) -> {
                    Customer selected = (Customer) spinnerCust.getSelectedItem();
                    int custId = selected != null ? selected.getCustomerId() : 0;
                    int checked = rgPayment.getCheckedRadioButtonId();
                    String updatedMethod = "Cash";
                    if (checked == R.id.rbCard) updatedMethod = "Card";
                    else if (checked == R.id.rbBank) updatedMethod = "Bank Transfer";

                    if (dbHelper.updateSale(sale.getSaleId(), custId, updatedMethod)) {
                        Toast.makeText(activity, "Sale updated", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onSaleChanged();
                    } else {
                        Toast.makeText(activity, "Failed to update sale", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void confirmVoidSale(Activity activity, DatabaseHelper dbHelper, Sale sale, OnSaleChangedListener listener) {
        if (!RoleManager.canEditOrVoidSales(RoleManager.getCurrentRole(activity))) {
            Toast.makeText(activity, "Admin permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle("Void Sale #" + sale.getSaleId())
                .setMessage("Voiding this sale will delete it from history and restore all sold quantities back to product stock. Continue?")
                .setPositiveButton("Void Sale", (d, w) -> {
                    if (dbHelper.deleteSale(sale.getSaleId())) {
                        Toast.makeText(activity, "Sale voided and stock restored", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onSaleChanged();
                    } else {
                        Toast.makeText(activity, "Failed to void sale", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
