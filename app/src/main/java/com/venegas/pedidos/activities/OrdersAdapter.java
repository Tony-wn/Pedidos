package com.venegas.pedidos.activities;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.venegas.pedidos.R;
import com.venegas.pedidos.models.Order;

import java.util.List;

/**
 * Adaptador para el RecyclerView de la lista de pedidos.
 */
public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Order order);
    }

    private final List<Order> orders;
    private final OnItemClickListener listener;

    public OrdersAdapter(List<Order> orders, OnItemClickListener listener) {
        this.orders   = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order, listener);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────
    static class OrderViewHolder extends RecyclerView.ViewHolder {

        private final View     viewStatusBar;
        private final TextView tvClientName;
        private final TextView tvOrderDetail;
        private final TextView tvDateTime;
        private final TextView tvStatus;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStatusBar = itemView.findViewById(R.id.viewStatusBar);
            tvClientName  = itemView.findViewById(R.id.tvClientName);
            tvOrderDetail = itemView.findViewById(R.id.tvOrderDetail);
            tvDateTime    = itemView.findViewById(R.id.tvDateTime);
            tvStatus      = itemView.findViewById(R.id.tvStatus);
        }

        void bind(Order order, OnItemClickListener listener) {
            tvClientName.setText(order.getClientName());
            tvOrderDetail.setText(order.getOrderDetail());
            tvDateTime.setText("🕒 " + (order.getCreatedAt() != null ? order.getCreatedAt() : "Sin fecha"));

            // Estado visual
            Context ctx = itemView.getContext();
            switch (order.getStatus()) {
                case Order.STATUS_SYNCED:
                    tvStatus.setText("✅ Sincronizado");
                    tvStatus.setBackgroundColor(ctx.getResources().getColor(R.color.status_synced, null));
                    viewStatusBar.setBackgroundColor(ctx.getResources().getColor(R.color.status_synced, null));
                    break;
                case Order.STATUS_ERROR:
                    tvStatus.setText("❌ Error");
                    tvStatus.setBackgroundColor(ctx.getResources().getColor(R.color.status_error, null));
                    viewStatusBar.setBackgroundColor(ctx.getResources().getColor(R.color.status_error, null));
                    break;
                default: // PENDING
                    tvStatus.setText("⏳ Pendiente");
                    tvStatus.setBackgroundColor(ctx.getResources().getColor(R.color.status_pending, null));
                    viewStatusBar.setBackgroundColor(ctx.getResources().getColor(R.color.status_pending, null));
                    break;
            }

            // Click listener
            itemView.setOnClickListener(v -> listener.onItemClick(order));
        }
    }
}