package com.example.smartshopmanager.auth;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartshopmanager.R;
import com.example.smartshopmanager.models.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnUserListener {
        void onEditClick(User user);
        void onDeleteClick(User user);
    }

    private final Context context;
    private List<User> userList;
    private final OnUserListener listener;
    private final int currentUserId;

    public UserAdapter(Context context, List<User> userList, int currentUserId, OnUserListener listener) {
        this.context = context;
        this.userList = userList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void updateList(List<User> newList) {
        this.userList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        boolean isSelf = user.getUserId() == currentUserId;
        holder.tvFullName.setText(user.getFullName() + (isSelf ? " (You)" : ""));
        holder.tvUsername.setText("@" + user.getUsername());
        holder.tvRoleBadge.setText(user.getRole());

        if (user.isActive()) {
            holder.tvStatusBadge.setText("Active");
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.success));
        } else {
            holder.tvStatusBadge.setText("Disabled");
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.error));
        }

        holder.btnDelete.setVisibility(isSelf ? View.GONE : View.VISIBLE);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(user);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(user);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvFullName, tvUsername, tvRoleBadge, tvStatusBadge;
        Button btnEdit, btnDelete;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFullName = itemView.findViewById(R.id.tvUserFullName);
            tvUsername = itemView.findViewById(R.id.tvUserUsername);
            tvRoleBadge = itemView.findViewById(R.id.tvUserRoleBadge);
            tvStatusBadge = itemView.findViewById(R.id.tvUserStatusBadge);
            btnEdit = itemView.findViewById(R.id.btnEditUser);
            btnDelete = itemView.findViewById(R.id.btnDeleteUser);
        }
    }
}
