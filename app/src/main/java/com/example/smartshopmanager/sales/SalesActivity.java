package com.example.smartshopmanager.sales;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartshopmanager.R;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.models.Customer;
import com.example.smartshopmanager.models.Product;
import com.example.smartshopmanager.models.Sale;
import com.example.smartshopmanager.models.SaleItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SalesActivity extends AppCompatActivity implements CartAdapter.OnCartRemoveListener {

    private DatabaseHelper dbHelper;
    private Spinner spinnerProducts;
    private EditText etQty, etDiscount;
    private Button btnAddToCart, btnCheckout;
    private RecyclerView recyclerViewCart;
    private TextView tvSubtotal, tvFinalTotal;

    private List<Product> productList;
    private List<SaleItem> cartItems;
    private CartAdapter cartAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales);

        dbHelper = new DatabaseHelper(this);

        spinnerProducts = findViewById(R.id.spinnerSalesProducts);
        etQty = findViewById(R.id.etSalesQty);
        etDiscount = findViewById(R.id.etDiscount);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnCheckout = findViewById(R.id.btnCheckout);
        recyclerViewCart = findViewById(R.id.recyclerViewCart);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvFinalTotal = findViewById(R.id.tvFinalTotal);

        cartItems = new ArrayList<>();
        cartAdapter = new CartAdapter(this, cartItems, this);
        recyclerViewCart.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCart.setAdapter(cartAdapter);

        loadProductsToSpinner();

        btnAddToCart.setOnClickListener(v -> addToCart());

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

        btnCheckout.setOnClickListener(v -> showCheckoutDialog());
    }

    private void loadProductsToSpinner() {
        productList = dbHelper.getAllProducts();
        ArrayAdapter<Product> adapter = new ArrayAdapter<Product>(this, android.R.layout.simple_spinner_item, productList) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                Product p = productList.get(position);
                tv.setText(p.getName() + " ($" + p.getPrice() + " - Stock: " + p.getQuantity() + ")");
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                Product p = productList.get(position);
                tv.setText(p.getName() + " ($" + p.getPrice() + " - Stock: " + p.getQuantity() + ")");
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProducts.setAdapter(adapter);
    }

    private void addToCart() {
        Product selectedProduct = (Product) spinnerProducts.getSelectedItem();
        if (selectedProduct == null) {
            Toast.makeText(this, "No product available", Toast.LENGTH_SHORT).show();
            return;
        }

        String qtyStr = etQty.getText().toString().trim();
        int qty = qtyStr.isEmpty() ? 1 : Integer.parseInt(qtyStr);

        if (qty <= 0) {
            Toast.makeText(this, "Quantity must be at least 1", Toast.LENGTH_SHORT).show();
            return;
        }

        if (qty > selectedProduct.getQuantity()) {
            Toast.makeText(this, "Insufficient stock! Available: " + selectedProduct.getQuantity(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if already in cart
        boolean exists = false;
        for (SaleItem item : cartItems) {
            if (item.getProductId() == selectedProduct.getProductId()) {
                int newQty = item.getQuantity() + qty;
                if (newQty > selectedProduct.getQuantity()) {
                    Toast.makeText(this, "Exceeds available stock", Toast.LENGTH_SHORT).show();
                    return;
                }
                item.setQuantity(newQty);
                exists = true;
                break;
            }
        }

        if (!exists) {
            cartItems.add(new SaleItem(selectedProduct.getProductId(), selectedProduct.getName(), qty, selectedProduct.getPrice()));
        }

        cartAdapter.notifyDataSetChanged();
        calculateTotals();
        etQty.setText("1");
        Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemoveClick(int position) {
        cartItems.remove(position);
        cartAdapter.notifyDataSetChanged();
        calculateTotals();
    }

    private double calculateSubtotal() {
        double subtotal = 0;
        for (SaleItem item : cartItems) {
            subtotal += item.getTotalPrice();
        }
        return subtotal;
    }

    private void calculateTotals() {
        double subtotal = calculateSubtotal();
        double discountPercent = 0;
        try {
            String discStr = etDiscount.getText().toString().trim();
            if (!discStr.isEmpty()) {
                discountPercent = Double.parseDouble(discStr);
            }
        } catch (NumberFormatException ignored) {}

        double discountAmount = subtotal * (discountPercent / 100.0);
        double finalTotal = Math.max(0, subtotal - discountAmount);

        tvSubtotal.setText(String.format("$%.2f", subtotal));
        tvFinalTotal.setText(String.format("$%.2f", finalTotal));
    }

    private void showCheckoutDialog() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_checkout, null);
        builder.setView(view);

        Spinner spinnerCust = view.findViewById(R.id.spinnerCheckoutCustomer);
        RadioGroup rgPayment = view.findViewById(R.id.rgPaymentMethod);
        TextView tvTotal = view.findViewById(R.id.tvCheckoutTotal);

        List<Customer> customers = dbHelper.getAllCustomers();
        // Add walk-in option
        customers.add(0, new Customer(0, "Walk-in Customer", "", ""));
        ArrayAdapter<Customer> custAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, customers);
        custAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCust.setAdapter(custAdapter);

        double subtotal = calculateSubtotal();
        double discount = 0;
        try {
            String d = etDiscount.getText().toString().trim();
            if (!d.isEmpty()) discount = Double.parseDouble(d);
        } catch (Exception ignored) {}
        double finalTotal = Math.max(0, subtotal - (subtotal * discount / 100.0));

        tvTotal.setText(String.format("Final Total: $%.2f", finalTotal));

        builder.setTitle("Checkout");
        builder.setPositiveButton("Confirm Payment", (dialog, which) -> {
            // Simulate payment (random success/failure)
            boolean paymentSuccess = new Random().nextDouble() > 0.1; // 90% success rate

            if (!paymentSuccess) {
                new AlertDialog.Builder(this)
                        .setTitle("Payment Failed")
                        .setMessage("Transaction declined by payment gateway simulation. Please try again.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            Customer selectedCust = (Customer) spinnerCust.getSelectedItem();
            int customerId = selectedCust != null && selectedCust.getCustomerId() > 0 ? selectedCust.getCustomerId() : 0;

            int checkedId = rgPayment.getCheckedRadioButtonId();
            String paymentMethod = "Cash";
            if (checkedId == R.id.rbCard) paymentMethod = "Card";
            else if (checkedId == R.id.rbBank) paymentMethod = "Bank Transfer";

            String saleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            Sale sale = new Sale(0, saleDate, finalTotal, paymentMethod, customerId, selectedCust != null ? selectedCust.getName() : "Walk-in");

            boolean success = dbHelper.saveSale(sale, cartItems);
            if (success) {
                new AlertDialog.Builder(this)
                        .setTitle("Payment Successful!")
                        .setMessage("Sale saved successfully and stock updated.")
                        .setPositiveButton("OK", (d, w) -> {
                            cartItems.clear();
                            cartAdapter.notifyDataSetChanged();
                            etDiscount.setText("0");
                            calculateTotals();
                            loadProductsToSpinner();
                        })
                        .show();
            } else {
                Toast.makeText(this, "Failed to save sale transaction", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
