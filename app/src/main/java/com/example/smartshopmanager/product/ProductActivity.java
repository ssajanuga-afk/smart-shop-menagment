package com.example.smartshopmanager.product;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
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
import com.example.smartshopmanager.auth.RoleManager;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.models.Category;
import com.example.smartshopmanager.models.Product;
import com.example.smartshopmanager.ui.BottomNavHelper;

import java.util.ArrayList;
import java.util.List;

public class ProductActivity extends AppCompatActivity implements ProductAdapter.OnProductListener, ApiProductAdapter.OnImportListener {

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private ProductAdapter productAdapter;
    private ApiProductAdapter apiProductAdapter;
    private EditText etSearch;
    private Spinner spinnerFilterCategory;
    private Button btnAddProduct, btnFetchApi, btnManageCategories;
    private View layoutProductActions, layoutCategoryFilter;
    private TextView tvSectionTitle, tvEmptyProducts;

    private boolean isShowingApi = false;
    private List<Category> filterCategories = new ArrayList<>();
    private int selectedFilterCategoryId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(this);

        if (!RoleManager.requireAccess(this, dbHelper, true)) {
            return;
        }

        setContentView(R.layout.activity_product);

        etSearch = findViewById(R.id.etSearchProduct);
        spinnerFilterCategory = findViewById(R.id.spinnerFilterCategory);
        btnAddProduct = findViewById(R.id.btnAddProduct);
        btnFetchApi = findViewById(R.id.btnFetchApi);
        btnManageCategories = findViewById(R.id.btnManageCategories);
        layoutProductActions = findViewById(R.id.layoutProductActions);
        layoutCategoryFilter = findViewById(R.id.layoutCategoryFilter);
        tvSectionTitle = findViewById(R.id.tvSectionTitle);
        tvEmptyProducts = findViewById(R.id.tvEmptyProducts);
        recyclerView = findViewById(R.id.recyclerViewProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnAddProduct.setOnClickListener(v -> {
            if (!RoleManager.canAddEditProducts(RoleManager.getCurrentRole(this))) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                return;
            }
            showAddEditProductDialog(null);
        });

        btnManageCategories.setOnClickListener(v -> {
            if (!RoleManager.canManageCategories(RoleManager.getCurrentRole(this))) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                return;
            }
            showManageCategoriesDialog();
        });

        btnFetchApi.setOnClickListener(v -> {
            if (!isShowingApi) {
                if (!RoleManager.canImportApiProducts(RoleManager.getCurrentRole(this))) {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    return;
                }
                fetchAndShowApiProducts();
            } else {
                loadLocalProducts();
            }
        });

        spinnerFilterCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < filterCategories.size()) {
                    selectedFilterCategoryId = filterCategories.get(position).getCategoryId();
                    if (!isShowingApi) {
                        applyLocalFilter();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isShowingApi) {
                    applyLocalFilter();
                } else if (apiProductAdapter != null) {
                    apiProductAdapter.filter(s.toString());
                    tvEmptyProducts.setVisibility(apiProductAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
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
        BottomNavHelper.setup(this, R.id.nav_products);
        applyRolePermissions();
        loadFilterCategories();
        if (!isShowingApi) {
            loadLocalProducts();
        }
    }

    private void applyRolePermissions() {
        String role = RoleManager.getCurrentRole(this);
        boolean canEditProducts = RoleManager.canAddEditProducts(role);
        boolean canImport = RoleManager.canImportApiProducts(role);
        boolean canManageCats = RoleManager.canManageCategories(role);

        btnManageCategories.setVisibility(canManageCats ? View.VISIBLE : View.GONE);
        if (!canEditProducts && !canImport) {
            layoutProductActions.setVisibility(View.GONE);
            if (isShowingApi) {
                isShowingApi = false;
            }
        } else {
            layoutProductActions.setVisibility(View.VISIBLE);
            btnAddProduct.setVisibility((canEditProducts && !isShowingApi) ? View.VISIBLE : View.GONE);
            btnFetchApi.setVisibility(canImport ? View.VISIBLE : View.GONE);
        }
    }

    private void loadFilterCategories() {
        int previousId = selectedFilterCategoryId;
        filterCategories = new ArrayList<>();
        filterCategories.add(new Category(0, "All Categories"));
        filterCategories.addAll(dbHelper.getAllCategories());

        ArrayAdapter<Category> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filterCategories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterCategory.setAdapter(adapter);

        for (int i = 0; i < filterCategories.size(); i++) {
            if (filterCategories.get(i).getCategoryId() == previousId) {
                spinnerFilterCategory.setSelection(i, false);
                break;
            }
        }
    }

    private void loadLocalProducts() {
        isShowingApi = false;
        tvSectionTitle.setText("Local Products");
        layoutCategoryFilter.setVisibility(View.VISIBLE);
        btnFetchApi.setText("API Catalog");
        applyRolePermissions();
        applyLocalFilter();
    }

    private void applyLocalFilter() {
        String keyword = etSearch.getText() != null ? etSearch.getText().toString() : "";
        List<Product> filtered = dbHelper.getProductsFiltered(selectedFilterCategoryId, keyword);
        if (productAdapter == null || recyclerView.getAdapter() != productAdapter) {
            productAdapter = new ProductAdapter(this, filtered, this);
            recyclerView.setAdapter(productAdapter);
        } else {
            productAdapter.updateList(filtered);
        }
        tvEmptyProducts.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void fetchAndShowApiProducts() {
        Toast.makeText(this, "Fetching API products...", Toast.LENGTH_SHORT).show();
        ProductApiService apiService = new ProductApiService(this);
        apiService.fetchApiProducts(new ProductApiService.ProductApiCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                isShowingApi = true;
                tvSectionTitle.setText("DummyJSON API Products");
                layoutCategoryFilter.setVisibility(View.GONE);
                btnAddProduct.setVisibility(View.GONE);
                btnFetchApi.setText("Local Products");
                apiProductAdapter = new ApiProductAdapter(ProductActivity.this, products, ProductActivity.this);
                recyclerView.setAdapter(apiProductAdapter);
                String keyword = etSearch.getText() != null ? etSearch.getText().toString() : "";
                if (!keyword.isEmpty()) {
                    apiProductAdapter.filter(keyword);
                }
                tvEmptyProducts.setVisibility(apiProductAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                Toast.makeText(ProductActivity.this, "Loaded " + products.size() + " API products", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProductActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAddEditProductDialog(Product product) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null);

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

        btnAddCategory.setOnClickListener(v -> showQuickAddCategoryDialog(categories, categoryAdapter, spinnerCategory));

        if (product != null) {
            etName.setText(product.getName());
            etPrice.setText(String.valueOf(product.getPrice()));
            etCost.setText(String.valueOf(product.getCostPrice()));
            etQty.setText(String.valueOf(product.getQuantity()));
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getCategoryId() == product.getCategoryId()) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(product != null ? "Edit Product" : "Add Product")
                .setView(view)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String costStr = etCost.getText().toString().trim();
            String qtyStr = etQty.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Product name is required");
                return;
            }
            double price;
            try {
                price = Double.parseDouble(priceStr);
                if (price < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                etPrice.setError("Enter a valid non-negative price");
                return;
            }

            double cost = 0;
            if (!costStr.isEmpty()) {
                try {
                    cost = Double.parseDouble(costStr);
                    if (cost < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    etCost.setError("Enter a valid non-negative cost");
                    return;
                }
            }

            int qty = 0;
            if (!qtyStr.isEmpty()) {
                try {
                    qty = Integer.parseInt(qtyStr);
                    if (qty < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    etQty.setError("Enter a valid non-negative quantity");
                    return;
                }
            }

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
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void showQuickAddCategoryDialog(List<Category> categories, ArrayAdapter<Category> adapter, Spinner spinnerToSelect) {
        EditText input = new EditText(this);
        input.setHint("Category Name");
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add New Category")
                .setView(input)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String catName = input.getText().toString().trim();
            if (catName.isEmpty()) {
                input.setError("Category name required");
                return;
            }
            long id = dbHelper.addCategory(catName);
            if (id == -1) {
                input.setError("Category already exists");
                return;
            }
            categories.clear();
            categories.addAll(dbHelper.getAllCategories());
            adapter.notifyDataSetChanged();
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getCategoryId() == id) {
                    spinnerToSelect.setSelection(i);
                    break;
                }
            }
            loadFilterCategories();
            Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void showManageCategoriesDialog() {
        List<Category> categories = dbHelper.getAllCategories();
        String[] labels = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            Category c = categories.get(i);
            int count = dbHelper.getProductCountForCategory(c.getCategoryId());
            labels[i] = c.getCategoryName() + " (" + count + " product" + (count == 1 ? "" : "s") + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Manage Categories")
                .setItems(labels, (dialog, which) -> {
                    Category selected = categories.get(which);
                    showCategoryOptionsDialog(selected);
                })
                .setPositiveButton("+ New Category", (d, w) -> showCreateCategoryStandaloneDialog())
                .setNegativeButton("Close", null)
                .show();
    }

    private void showCreateCategoryStandaloneDialog() {
        EditText input = new EditText(this);
        input.setHint("Category Name");
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Category")
                .setView(input)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                input.setError("Category name required");
                return;
            }
            if (dbHelper.addCategory(name) == -1) {
                input.setError("Category already exists");
                return;
            }
            Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show();
            loadFilterCategories();
            dialog.dismiss();
            showManageCategoriesDialog();
        }));
        dialog.show();
    }

    private void showCategoryOptionsDialog(Category category) {
        String role = RoleManager.getCurrentRole(this);
        boolean canDelete = RoleManager.canDeleteItems(role) && category.getCategoryId() > 1;

        List<String> actions = new ArrayList<>();
        actions.add("Rename Category");
        if (canDelete) {
            actions.add("Delete Category");
        }

        new AlertDialog.Builder(this)
                .setTitle(category.getCategoryName())
                .setItems(actions.toArray(new String[0]), (d, which) -> {
                    if (which == 0) {
                        showRenameCategoryDialog(category);
                    } else if (canDelete) {
                        confirmDeleteCategory(category);
                    }
                })
                .setNegativeButton("Back", (d, w) -> showManageCategoriesDialog())
                .show();
    }

    private void showRenameCategoryDialog(Category category) {
        EditText input = new EditText(this);
        input.setText(category.getCategoryName());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Rename Category")
                .setView(input)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                input.setError("Name required");
                return;
            }
            if (!dbHelper.updateCategory(category.getCategoryId(), newName)) {
                input.setError("Duplicate or invalid category name");
                return;
            }
            Toast.makeText(this, "Category renamed", Toast.LENGTH_SHORT).show();
            loadFilterCategories();
            if (!isShowingApi) applyLocalFilter();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void confirmDeleteCategory(Category category) {
        int count = dbHelper.getProductCountForCategory(category.getCategoryId());
        String msg = count > 0
                ? "Delete \"" + category.getCategoryName() + "\"? Its " + count + " product(s) will move to General."
                : "Delete category \"" + category.getCategoryName() + "\"?";

        new AlertDialog.Builder(this)
                .setTitle("Delete Category")
                .setMessage(msg)
                .setPositiveButton("Delete", (d, w) -> {
                    if (dbHelper.deleteCategory(category.getCategoryId())) {
                        Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show();
                        if (selectedFilterCategoryId == category.getCategoryId()) {
                            selectedFilterCategoryId = 0;
                        }
                        loadFilterCategories();
                        if (!isShowingApi) applyLocalFilter();
                    } else {
                        Toast.makeText(this, "Cannot delete default General category", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEditClick(Product product) {
        if (!RoleManager.canAddEditProducts(RoleManager.getCurrentRole(this))) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            return;
        }
        showAddEditProductDialog(product);
    }

    @Override
    public void onDeleteClick(Product product) {
        if (!RoleManager.canDeleteItems(RoleManager.getCurrentRole(this))) {
            Toast.makeText(this, "Admin permission required to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete " + product.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteProduct(product.getProductId());
                    Toast.makeText(this, "Product removed", Toast.LENGTH_SHORT).show();
                    loadLocalProducts();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onImportClick(Product product) {
        if (!RoleManager.canImportApiProducts(RoleManager.getCurrentRole(this))) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dbHelper.productExistsByName(product.getName())) {
            new AlertDialog.Builder(this)
                    .setTitle("Product Already Exists")
                    .setMessage("\"" + product.getName() + "\" is already in your local catalog. Import another entry anyway?")
                    .setPositiveButton("Import", (d, w) -> performImport(product))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            performImport(product);
        }
    }

    private void performImport(Product product) {
        List<Category> categories = dbHelper.getAllCategories();
        int catId = 1;
        String apiCat = product.getCategoryName();
        if (apiCat != null && !apiCat.trim().isEmpty()) {
            boolean found = false;
            for (Category c : categories) {
                if (c.getCategoryName().equalsIgnoreCase(apiCat.trim())) {
                    catId = c.getCategoryId();
                    found = true;
                    break;
                }
            }
            if (!found) {
                long newCatId = dbHelper.addCategory(apiCat.trim());
                if (newCatId > 0) catId = (int) newCatId;
            }
        }

        Product localP = new Product(product.getName(), catId, product.getPrice(), product.getCostPrice(), product.getQuantity());
        dbHelper.addProduct(localP);
        loadFilterCategories();
        Toast.makeText(this, "Imported " + product.getName() + " to shop", Toast.LENGTH_SHORT).show();
    }
}
