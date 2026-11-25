package pk.edu.nfciet.shopease;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.Holder> {

    public interface OnProductClick {
        void open(Product p);
    }

    private final List<Product> items = new ArrayList<>();
    private final OnProductClick onClick;

    public ProductAdapter(OnProductClick onClick) {
        this.onClick = onClick;
    }

    public void setItems(List<Product> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Product p = items.get(position);
        h.title.setText(p.title);
        h.price.setText("Rs " + p.price);
        Glide.with(h.itemView)
                .load(p.imageUrl)
                .placeholder(R.drawable.ic_shopping_cart)
                .into(h.image);
        h.itemView.setOnClickListener(v -> onClick.open(p));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, price;

        Holder(View v) {
            super(v);
            image = v.findViewById(R.id.ivImage);
            title = v.findViewById(R.id.tvTitle);
            price = v.findViewById(R.id.tvPrice);
        }
    }
}