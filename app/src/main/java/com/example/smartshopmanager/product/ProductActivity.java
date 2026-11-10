package com.example.smartshopmanager.product;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartshopmanager.R;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.models.Category;
import com.example.smartshopmanager.models.Product;

import java.util.List;

public class ProductActivity extends AppCompatActivity implements ProductAdapter.OnProductListener, ApiProductAdapter.OnImportListener {

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private ProductAdapter productAdapter;
    private ApiProductAdapter apiProductAdapter;
    private EditText etSearch;
    private Button btnAddProduct, btnFetchApi;
    private TextView tvSectionTitle;

    private boolean isShowingApi = false;
    private List<Product> localProductList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);

        dbHelper = new DatabaseHelper(this);

        etSearch = findViewById(R.id.etSearchProduct);
        btnAddProduct = findViewById(R.id.btnAddProduct);
        btnFetchApi = findViewById(R.id.btnFetchApi);
        tvSectionTitle = findViewById(R.id.tvSectionTitle);
        recyclerView = findViewById(R.id.recyclerViewProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadLocalProducts();

        btnAddProduct.setOnClickListener(v -> showAddEditProductDialog(null));

        btnFetchApi.setOnClickListener(v -> {
            if (!isShowingApi) {
                fetchAndShowApiProducts();
            } else {
                loadLocalProducts();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isShowingApi) {
                    List<Product> filtered = dbHelper.searchProducts(s.toString());
                    productAdapter.updateList(filtered);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadLocalProducts() {
        isShowingApi = false;
        tvSectionTitle.setText("Local Products");
        btnAddProduct.setVisibility(View.VISIBLE);
        btnFetchApi.setText("View API Products");
        localProductList = dbHelper.getAllProducts();
        productAdapter = new ProductAdapter(this, localProductList, this);
        recyclerView.setAdapter(productAdapter);
    }

    private void fetchAndShowApiProducts() {
        Toast.load(this, "Fetching API products...", Toast.LENGTH_SHORT).show(); // wait, Toast.LENGTH_SHORT needs Context, Toast.makeText
        Toast.makeText(this, "Fetching API products...", Toast.LENGTH_SHORT).show();
        ProductApiService apiService = new ProductApiService(this);
        apiService.fetchApiProducts(new ProductApiService.ProductApiCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                isShowingApi = true;
                tvSectionTitle.setText("DummyJSON API Products");
                btnAddProduct.setVisibility(View.GONE);
                btnFetchApi.setText("View Local Products");
                apiProductAdapter = new ApiProductAdapter(ProductActivity.this, products, ProductActivity.this);
                recyclerView.setAdapter(apiProductAdapter);
                Toast.makeText(ProductActivity.this, "Loaded " + products.size() + " API products", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProductActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAddEditProductDialog(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null);
        builder.setView(view);

        EditText etName = view.findViewById(R.id.etDialogProductName);
        EditText etPrice = view.findViewById(R.id.etDialogProductPrice);
        EditText etCost = view.findViewById(R.id.etDialogProductCost);
        EditText etQty = view.findViewById(R.id.etDialogProductQty);
        Spinner spinnerCategory = view.findViewById(R.id.spinnerDialogCategory);
        Button btnAddCategory = view.findViewById(R.id.btnDialogAddCategory);

        List<Category> categories = dbHelper.getAllCategories();
        ArrayAdapter<Category> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        btnAddCategory.setOnClickListener(v -> {
            // Quick add category dialog
            AlertDialog.Builder catBuilder = new AlertDialog.Builder(this);
            EditText input = new EditText(this);
            input.setHint("Category Name");
            catBuilder.setTitle("Add New Category").setView(input);
            catBuilder.setPositiveButton("Add", (dialog, which) -> {
                String catName = input.getText().toString().trim();
                if (!catName.isEmpty()) {
                    long id = dbHelper.addCategory(catName);
                    if (id != -1) {
                        categories.clear();
                        categories.addAll(dbHelper.getAllCategories());
                        categoryAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            catBuilder.setNegativeButton("Cancel", null);
            catBuilder.show();
        });

        if (product != null) {
            etName.setText(product.getName());
            etPrice.setText(String.valueOf(product.getPrice()));
            etCost.setText(String.valueOf(product.getCostPrice()));
            etQty.setText(String.valueOf(product.getQuantity()));
            // Select category
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getCategoryId() == product.getCategoryId()) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
            builder.setTitle("Edit Product");
        } else {
            builder.setTitle("Add Product");
        }

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String costStr = etCost.getText().toString().trim();
            String qtyStr = etQty.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Name and Price are required", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(priceStr);
            double cost = costStr.isEmpty() ? 0 : Double.parseDouble(costStr);
            int qty = qtyStr.isEmpty() ? 0 : Integer.parseInt(qtyStr);
            Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
            int catId = selectedCategory != null ? selectedCategory.getCategoryId() : 1;

            if (product == null) {
                Product newP = new Product(name, catId, price, cost, qty);
                dbHelper.addProduct(newP);
                Toast.makeText(this, "Product added successfully", Toast.LENGTH_SHORT).show();
            } else {
                product.setName(name);
                product.setCategoryId(catId);
                product.setPrice(price);
                product.setCostPrice(cost);
                product.setQuantity(qty);
                dbHelper.updateProduct(product);
                Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show();
            }
            loadLocalProducts();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onEditClick(Product product) {
        showAddEditProductDialog(product);
    }

    @Override
    public void onDeleteClick(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete " + product.getName() + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dbHelper.deleteProduct(product.getProductId());
                    Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show();
                    loadLocalProducts();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onImportClick(Product product) {
        // Import API product to local DB
        // Find or create category matching API product category
        List<Category> categories = dbHelper.getAllCategories();
        int catId = 1;
        String apiCat = product.getCategoryName();
        if (apiCat != null && !apiCat.isEmpty()) {
            boolean found = false;
            for (Category c : categories) {
                if (c.getCategoryName().equalsIgnoreCase(apiCat)) {
                    catId = c.getCategoryId();
                    found = true;
                    break;
                }
            }
            if (!found) {
                catId = (int) dbHelper.addCategory(apiCat);
            }
        }

        Product localP = new Product(product.getName(), catId, product.getPrice(), product.getCostPrice(), product.getQuantity());
        dbHelper.addProduct(localP);
        Toast.makeText(this, "Imported " + product.getName() + " to shop", Toast.LENGTH_SHORT).show();
    }
}
