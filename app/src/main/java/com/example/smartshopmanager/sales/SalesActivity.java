package com.example.smartshopmanager.sales;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartshopmanager.R;
import com.example.smartshopmanager.auth.RoleManager;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.models.Customer;
import com.example.smartshopmanager.models.Product;
import com.example.smartshopmanager.models.Sale;
import com.example.smartshopmanager.models.SaleItem;
import com.example.smartshopmanager.ui.BottomNavHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SalesActivity extends AppCompatActivity implements CartAdapter.OnCartActionListener {

    private DatabaseHelper dbHelper;
    private Spinner spinnerProducts;
    private EditText etQty, etDiscount, etSearchSalesHistory;
    private Button btnAddToCart, btnCheckout, btnModeCart, btnModeHistory;
    private RecyclerView recyclerViewCart, recyclerViewPosSalesHistory;
    private TextView tvSubtotal, tvFinalTotal, tvEmptyCart, tvEmptySalesHistory;
    private View layoutPosContainer, layoutSalesHistoryContainer;

    private List<Product> productList = new ArrayList<>();
    private final List<SaleItem> cartItems = new ArrayList<>();
    private CartAdapter cartAdapter;
    private SalesHistoryAdapter salesHistoryAdapter;
    private boolean showingHistory = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(this);

        if (!RoleManager.requireAccess(this, dbHelper, true)) {
            return;
        }

        setContentView(R.layout.activity_sales);

        spinnerProducts = findViewById(R.id.spinnerSalesProducts);
        etQty = findViewById(R.id.etSalesQty);
        etDiscount = findViewById(R.id.etDiscount);
        etSearchSalesHistory = findViewById(R.id.etSearchSalesHistory);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnCheckout = findViewById(R.id.btnCheckout);
        btnModeCart = findViewById(R.id.btnModeCart);
        btnModeHistory = findViewById(R.id.btnModeHistory);
        recyclerViewCart = findViewById(R.id.recyclerViewCart);
        recyclerViewPosSalesHistory = findViewById(R.id.recyclerViewPosSalesHistory);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvFinalTotal = findViewById(R.id.tvFinalTotal);
        tvEmptyCart = findViewById(R.id.tvEmptyCart);
        tvEmptySalesHistory = findViewById(R.id.tvEmptySalesHistory);
        layoutPosContainer = findViewById(R.id.layoutPosContainer);
        layoutSalesHistoryContainer = findViewById(R.id.layoutSalesHistoryContainer);

        cartAdapter = new CartAdapter(this, cartItems, this);
        recyclerViewCart.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCart.setAdapter(cartAdapter);

        recyclerViewPosSalesHistory.setLayoutManager(new LinearLayoutManager(this));
        salesHistoryAdapter = new SalesHistoryAdapter(this, new ArrayList<>(), sale ->
                SaleReceiptDialogHelper.showReceiptDialog(this, dbHelper, sale, () -> {
                    loadProductsToSpinner();
                    loadSalesHistory();
                }));
        recyclerViewPosSalesHistory.setAdapter(salesHistoryAdapter);

        btnAddToCart.setOnClickListener(v -> addToCart());
        btnCheckout.setOnClickListener(v -> showCheckoutDialog());
        btnModeCart.setOnClickListener(v -> setMode(false));
        btnModeHistory.setOnClickListener(v -> setMode(true));

        etDiscount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotals();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearchSalesHistory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (showingHistory) {
                    loadSalesHistory();
                }
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
        BottomNavHelper.setup(this, R.id.nav_sales);
        loadProductsToSpinner();
        reconcileCartWithLiveInventory();
        updateCartEmptyState();
        calculateTotals();
        if (showingHistory) {
            loadSalesHistory();
        }
    }

    private void reconcileCartWithLiveInventory() {
        if (cartItems.isEmpty()) return;
        boolean adjusted = false;
        for (int i = cartItems.size() - 1; i >= 0; i--) {
            SaleItem item = cartItems.get(i);
            Product latest = dbHelper.getProductById(item.getProductId());
            if (latest == null || !latest.isActive() || latest.getQuantity() <= 0) {
                cartItems.remove(i);
                adjusted = true;
            } else {
                item.setProductName(latest.getName());
                item.setUnitPrice(latest.getPrice());
                item.setCostPrice(latest.getCostPrice());
                if (item.getQuantity() > latest.getQuantity()) {
                    item.setQuantity(latest.getQuantity());
                    adjusted = true;
                }
            }
        }
        if (adjusted) {
            cartAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Cart adjusted to match updated inventory", Toast.LENGTH_SHORT).show();
        }
    }

    private void setMode(boolean history) {
        showingHistory = history;
        layoutPosContainer.setVisibility(history ? View.GONE : View.VISIBLE);
        layoutSalesHistoryContainer.setVisibility(history ? View.VISIBLE : View.GONE);

        int primary = ContextCompat.getColor(this, R.color.primary);
        int mint = ContextCompat.getColor(this, R.color.mint_badge);
        int white = ContextCompat.getColor(this, R.color.white);

        if (history) {
            btnModeHistory.setBackgroundTintList(ColorStateList.valueOf(primary));
            btnModeHistory.setTextColor(white);
            btnModeCart.setBackgroundTintList(ColorStateList.valueOf(mint));
            btnModeCart.setTextColor(primary);
            loadSalesHistory();
        } else {
            btnModeCart.setBackgroundTintList(ColorStateList.valueOf(primary));
            btnModeCart.setTextColor(white);
            btnModeHistory.setBackgroundTintList(ColorStateList.valueOf(mint));
            btnModeHistory.setTextColor(primary);
            loadProductsToSpinner();
        }
    }

    private void loadSalesHistory() {
        String role = RoleManager.getCurrentRole(this);
        String keyword = etSearchSalesHistory.getText() != null ? etSearchSalesHistory.getText().toString() : "";
        int cashierFilter = RoleManager.isCashier(role) ? RoleManager.getCurrentUserId(this) : 0;
        List<Sale> sales = dbHelper.getSalesFiltered("All", keyword, cashierFilter);
        salesHistoryAdapter.updateList(sales);
        tvEmptySalesHistory.setVisibility(sales.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadProductsToSpinner() {
        productList = dbHelper.getAllProducts();
        ArrayAdapter<Product> adapter = new ArrayAdapter<Product>(this, android.R.layout.simple_spinner_item, productList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                Product p = productList.get(position);
                tv.setText(formatSpinnerLabel(p));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                Product p = productList.get(position);
                tv.setText(formatSpinnerLabel(p));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProducts.setAdapter(adapter);
    }

    private String formatSpinnerLabel(Product p) {
        if (p.getQuantity() <= 0) {
            return String.format(Locale.getDefault(), "%s ($%.2f - OUT OF STOCK)", p.getName(), p.getPrice());
        }
        return String.format(Locale.getDefault(), "%s ($%.2f - Stock: %d)", p.getName(), p.getPrice(), p.getQuantity());
    }

    private void addToCart() {
        Product selectedProduct = (Product) spinnerProducts.getSelectedItem();
        if (selectedProduct == null) {
            Toast.makeText(this, "No product available. Add products first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedProduct.getQuantity() <= 0) {
            Toast.makeText(this, selectedProduct.getName() + " is out of stock", Toast.LENGTH_SHORT).show();
            return;
        }

        int qty = 1;
        String qtyStr = etQty.getText().toString().trim();
        if (!qtyStr.isEmpty()) {
            try {
                qty = Integer.parseInt(qtyStr);
            } catch (NumberFormatException e) {
                etQty.setError("Invalid quantity");
                return;
            }
        }

        if (qty <= 0) {
            etQty.setError("Quantity must be at least 1");
            return;
        }
        if (qty > selectedProduct.getQuantity()) {
            Toast.makeText(this, "Insufficient stock! Available: " + selectedProduct.getQuantity(), Toast.LENGTH_SHORT).show();
            return;
        }

        boolean exists = false;
        for (SaleItem item : cartItems) {
            if (item.getProductId() == selectedProduct.getProductId()) {
                int newQty = item.getQuantity() + qty;
                if (newQty > selectedProduct.getQuantity()) {
                    Toast.makeText(this, "Exceeds available stock (" + selectedProduct.getQuantity() + ")", Toast.LENGTH_SHORT).show();
                    return;
                }
                item.setQuantity(newQty);
                exists = true;
                break;
            }
        }

        if (!exists) {
            cartItems.add(new SaleItem(
                    selectedProduct.getProductId(),
                    selectedProduct.getName(),
                    qty,
                    selectedProduct.getPrice(),
                    selectedProduct.getCostPrice()
            ));
        }

        cartAdapter.notifyDataSetChanged();
        updateCartEmptyState();
        calculateTotals();
        etQty.setText("1");
    }

    @Override
    public void onIncreaseQty(int position) {
        SaleItem item = cartItems.get(position);
        Product fresh = dbHelper.getProductById(item.getProductId());
        int maxStock = fresh != null ? fresh.getQuantity() : 0;
        if (item.getQuantity() + 1 > maxStock) {
            Toast.makeText(this, "Max available stock reached (" + maxStock + ")", Toast.LENGTH_SHORT).show();
            return;
        }
        item.setQuantity(item.getQuantity() + 1);
        cartAdapter.notifyItemChanged(position);
        calculateTotals();
    }

    @Override
    public void onDecreaseQty(int position) {
        SaleItem item = cartItems.get(position);
        if (item.getQuantity() > 1) {
            item.setQuantity(item.getQuantity() - 1);
            cartAdapter.notifyItemChanged(position);
        } else {
            cartItems.remove(position);
            cartAdapter.notifyDataSetChanged();
            updateCartEmptyState();
        }
        calculateTotals();
    }

    @Override
    public void onRemoveClick(int position) {
        cartItems.remove(position);
        cartAdapter.notifyDataSetChanged();
        updateCartEmptyState();
        calculateTotals();
    }

    private void updateCartEmptyState() {
        tvEmptyCart.setVisibility(cartItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private double calculateSubtotal() {
        double subtotal = 0;
        for (SaleItem item : cartItems) {
            subtotal += item.getTotalPrice();
        }
        return subtotal;
    }

    private double parseValidDiscountPercent() {
        String discStr = etDiscount.getText().toString().trim();
        if (discStr.isEmpty()) return 0.0;
        try {
            double val = Double.parseDouble(discStr);
            if (val < 0) return 0.0;
            if (val > 100) return 100.0;
            return val;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void calculateTotals() {
        double subtotal = calculateSubtotal();
        double discountPercent = parseValidDiscountPercent();
        double discountAmount = subtotal * (discountPercent / 100.0);
        double finalTotal = Math.max(0, subtotal - discountAmount);

        tvSubtotal.setText(String.format(Locale.getDefault(), "$%.2f", subtotal));
        tvFinalTotal.setText(String.format(Locale.getDefault(), "$%.2f", finalTotal));
    }

    private void showCheckoutDialog() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        double discountPercent;
        String rawDisc = etDiscount.getText().toString().trim();
        if (rawDisc.isEmpty()) {
            discountPercent = 0.0;
        } else {
            try {
                discountPercent = Double.parseDouble(rawDisc);
                if (discountPercent < 0 || discountPercent > 100) {
                    etDiscount.setError("Discount must be 0 - 100%");
                    return;
                }
            } catch (NumberFormatException e) {
                etDiscount.setError("Invalid discount");
                return;
            }
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_checkout, null);
        Spinner spinnerCust = view.findViewById(R.id.spinnerCheckoutCustomer);
        RadioGroup rgPayment = view.findViewById(R.id.rgPaymentMethod);
        RadioButton rbCash = view.findViewById(R.id.rbCash);
        TextView tvTotal = view.findViewById(R.id.tvCheckoutTotal);

        rbCash.setChecked(true);

        List<Customer> customers = dbHelper.getAllCustomers();
        customers.add(0, new Customer(0, "Walk-in Customer", "", ""));
        ArrayAdapter<Customer> custAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, customers);
        custAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCust.setAdapter(custAdapter);

        double subtotal = calculateSubtotal();
        double finalTotal = Math.max(0, subtotal - (subtotal * discountPercent / 100.0));
        tvTotal.setText(String.format(Locale.getDefault(), "Final Total: $%.2f", finalTotal));

        new AlertDialog.Builder(this)
                .setTitle("Checkout")
                .setView(view)
                .setPositiveButton("Confirm Payment", (dialog, which) -> {
                    Customer selectedCust = (Customer) spinnerCust.getSelectedItem();
                    int customerId = selectedCust != null && selectedCust.getCustomerId() > 0 ? selectedCust.getCustomerId() : 0;

                    int checkedId = rgPayment.getCheckedRadioButtonId();
                    String paymentMethod = "Cash";
                    if (checkedId == R.id.rbCard) paymentMethod = "Card";
                    else if (checkedId == R.id.rbBank) paymentMethod = "Bank Transfer";

                    String saleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    Sale sale = new Sale(
                            0,
                            saleDate,
                            subtotal,
                            discountPercent,
                            finalTotal,
                            paymentMethod,
                            customerId,
                            selectedCust != null ? selectedCust.getName() : "Walk-in",
                            RoleManager.getCurrentUserId(this),
                            RoleManager.getCurrentFullName(this)
                    );

                    DatabaseHelper.SaleSaveResult result = dbHelper.saveSaleDetailed(sale, cartItems);
                    if (result.success) {
                        new AlertDialog.Builder(this)
                                .setTitle("Payment Successful!")
                                .setMessage("Receipt #" + result.saleId + " saved and inventory updated.")
                                .setPositiveButton("OK", (d, w) -> {
                                    cartItems.clear();
                                    cartAdapter.notifyDataSetChanged();
                                    updateCartEmptyState();
                                    etDiscount.setText("0");
                                    calculateTotals();
                                    loadProductsToSpinner();
                                })
                                .show();
                    } else {
                        Toast.makeText(this, result.errorMessage != null ? result.errorMessage : "Checkout failed", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
