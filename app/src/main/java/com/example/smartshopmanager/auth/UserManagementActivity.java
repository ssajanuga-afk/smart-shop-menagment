package com.example.smartshopmanager.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartshopmanager.R;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.models.User;

import java.util.List;

/**
 * Admin-only CRUD management screen for staff accounts and assigned roles.
 */
public class UserManagementActivity extends AppCompatActivity implements UserAdapter.OnUserListener {

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private UserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(this);

        if (!RoleManager.requireAccess(this, dbHelper,
                RoleManager.canManageUsers(RoleManager.getCurrentRole(this)))) {
            return;
        }

        setContentView(R.layout.activity_user_management);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        Button btnAdd = findViewById(R.id.btnAddStaffUser);
        recyclerView = findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnAdd.setOnClickListener(v -> showAddEditUserDialog(null));
        loadUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!RoleManager.requireAccess(this, dbHelper,
                RoleManager.canManageUsers(RoleManager.getCurrentRole(this)))) {
            return;
        }
        loadUsers();
    }

    private void loadUsers() {
        List<User> users = dbHelper.getAllUsers();
        int currentUserId = RoleManager.getCurrentUserId(this);
        if (adapter == null) {
            adapter = new UserAdapter(this, users, currentUserId, this);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(users);
        }
    }

    private void showAddEditUserDialog(User existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_user, null);
        EditText etFullName = view.findViewById(R.id.etDialogUserFullName);
        EditText etUsername = view.findViewById(R.id.etDialogUserUsername);
        EditText etPin = view.findViewById(R.id.etDialogUserPin);
        Spinner spinnerRole = view.findViewById(R.id.spinnerDialogUserRole);
        SwitchCompat switchActive = view.findViewById(R.id.switchDialogUserActive);

        String[] roles = {RoleManager.ROLE_ADMIN, RoleManager.ROLE_MANAGER, RoleManager.ROLE_CASHIER};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        if (existing != null) {
            etFullName.setText(existing.getFullName());
            etUsername.setText(existing.getUsername());
            etPin.setHint("New PIN (leave blank to keep existing)");
            switchActive.setChecked(existing.isActive());
            for (int i = 0; i < roles.length; i++) {
                if (roles[i].equalsIgnoreCase(existing.getRole())) {
                    spinnerRole.setSelection(i);
                    break;
                }
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add Staff Account" : "Edit Staff Account")
                .setView(view)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String fullName = etFullName.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String pin = etPin.getText().toString().trim();
            String selectedRole = (String) spinnerRole.getSelectedItem();
            boolean active = switchActive.isChecked();

            if (fullName.isEmpty()) {
                etFullName.setError("Full name is required");
                return;
            }
            if (username.isEmpty()) {
                etUsername.setError("Username is required");
                return;
            }
            if (existing == null && !AuthUtils.isValidPin(pin)) {
                etPin.setError("Enter a 4-8 digit PIN");
                return;
            }
            if (existing != null && !pin.isEmpty() && !AuthUtils.isValidPin(pin)) {
                etPin.setError("PIN must be 4-8 digits");
                return;
            }

            if (existing == null) {
                User newUser = new User(fullName, username, selectedRole, active);
                long id = dbHelper.addUser(newUser, pin);
                if (id == -1) {
                    etUsername.setError("Username already exists");
                    return;
                }
                Toast.makeText(this, "Staff account created", Toast.LENGTH_SHORT).show();
            } else {
                existing.setFullName(fullName);
                existing.setUsername(username);
                existing.setRole(selectedRole);
                existing.setActive(active);
                boolean updated = dbHelper.updateUser(existing, pin.isEmpty() ? null : pin);
                if (!updated) {
                    Toast.makeText(this, "Update blocked: duplicate username or last active Admin cannot be demoted/disabled", Toast.LENGTH_LONG).show();
                    return;
                }
                if (existing.getUserId() == RoleManager.getCurrentUserId(this)) {
                    RoleManager.refreshAndValidateSession(this, dbHelper);
                }
                Toast.makeText(this, "Staff account updated", Toast.LENGTH_SHORT).show();
            }

            loadUsers();
            dialog.dismiss();
        }));

        dialog.show();
    }

    @Override
    public void onEditClick(User user) {
        showAddEditUserDialog(user);
    }

    @Override
    public void onDeleteClick(User user) {
        if (user.getUserId() == RoleManager.getCurrentUserId(this)) {
            Toast.makeText(this, "You cannot delete your own active account", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete Staff Account")
                .setMessage("Delete account for " + user.getFullName() + " (@" + user.getUsername() + ")?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (dbHelper.deleteUser(user.getUserId())) {
                        Toast.makeText(this, "Staff account deleted", Toast.LENGTH_SHORT).show();
                        loadUsers();
                    } else {
                        Toast.makeText(this, "Cannot delete the last active Admin", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
