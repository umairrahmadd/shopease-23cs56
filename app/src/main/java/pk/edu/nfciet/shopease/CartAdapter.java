package pk.edu.nfciet.shopease;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.Holder> {

    public interface Actions {
        void changeQty(CartItem item, long newQty);
        void remove(CartItem item);
    }

    private final List<CartItem> items = new ArrayList<>();
    private final Actions actions;

    public CartAdapter(Actions actions) {
        this.actions = actions;
    }

    public void setItems(List<CartItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        CartItem c = items.get(position);
        h.title.setText(c.title);
        h.qty.setText(String.valueOf(c.quantity));
        h.lineTotal.setText("Rs " + (c.price * c.quantity));

        Glide.with(h.itemView)
                .load(c.imageUrl)
                .placeholder(R.drawable.ic_shopping_cart)
                .into(h.image);

        h.minus.setOnClickListener(v -> {
            if (c.quantity > 1) {
                actions.changeQty(c, c.quantity - 1);
            } else {
                actions.remove(c);
            }
        });

        h.plus.setOnClickListener(v -> actions.changeQty(c, c.quantity + 1));
        h.remove.setOnClickListener(v -> actions.remove(c));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, lineTotal, qty, remove;
        Button minus, plus;

        Holder(View v) {
            super(v);
            image = v.findViewById(R.id.ivImage);
            title = v.findViewById(R.id.tvTitle);
            lineTotal = v.findViewById(R.id.tvLineTotal);
            qty = v.findViewById(R.id.tvQty);
            remove = v.findViewById(R.id.tvRemove);
            minus = v.findViewById(R.id.btnMinus);
            plus = v.findViewById(R.id.btnPlus);
        }
    }
}