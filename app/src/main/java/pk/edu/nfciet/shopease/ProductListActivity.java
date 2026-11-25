package pk.edu.nfciet.shopease;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ProductListActivity extends AppCompatActivity {

    private ProductAdapter adapter;
    private ProgressBar progress;
    private TextView tvMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        View root = findViewById(R.id.mainProductList);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            goToLogin();
            return;
        }

        setSupportActionBar(findViewById(R.id.toolbar));
        progress = findViewById(R.id.progress);
        tvMessage = findViewById(R.id.tvMessage);

        RecyclerView rv = findViewById(R.id.rvProducts);
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductAdapter(p -> {
            Intent i = new Intent(this, ProductDetailActivity.class);
            i.putExtra("productId", p.id);
            startActivity(i);
        });
        rv.setAdapter(adapter);

        loadProducts();
    }

    private void loadProducts() {
        progress.setVisibility(View.VISIBLE);
        tvMessage.setVisibility(View.GONE);

        FirebaseFirestore.getInstance().collection("products").get()
                .addOnSuccessListener(snap -> {
                    progress.setVisibility(View.GONE);
                    List<Product> list = snap.toObjects(Product.class);
                    adapter.setItems(list);
                    if (list.isEmpty()) showMessage("No products yet.");
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    showMessage("Could not load products. Tap to retry.");
                    tvMessage.setOnClickListener(v -> loadProducts());
                });
    }

    private void showMessage(String text) {
        tvMessage.setText(text);
        tvMessage.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_cart) {
            startActivity(new Intent(this, CartActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            goToLogin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void goToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}