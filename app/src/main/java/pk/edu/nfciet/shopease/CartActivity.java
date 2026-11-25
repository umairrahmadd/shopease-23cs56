package pk.edu.nfciet.shopease;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class CartActivity extends AppCompatActivity {

    private CollectionReference cartRef;
    private CartAdapter adapter;
    private long subtotal = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        View root = findViewById(R.id.rootCart);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            finish();
            return;
        }

        cartRef = FirebaseFirestore.getInstance()
                .collection("carts")
                .document(uid)
                .collection("items");

        adapter = new CartAdapter(new CartAdapter.Actions() {
            @Override
            public void changeQty(CartItem item, long newQty) {
                if (item.id != null) {
                    cartRef.document(item.id).update("quantity", newQty);
                }
            }

            @Override
            public void remove(CartItem item) {
                if (item.id != null) {
                    cartRef.document(item.id).delete();
                }
            }
        });

        RecyclerView rv = findViewById(R.id.rvCart);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        cartRef.addSnapshotListener(this, (snap, e) -> {
            if (e != null || snap == null) {
                return;
            }
            List<CartItem> items = snap.toObjects(CartItem.class);
            adapter.setItems(items);
            findViewById(R.id.tvEmpty).setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            showTotals(items);
        });

        findViewById(R.id.btnCheckout).setOnClickListener(v -> {
            if (subtotal == 0) {
                Toast.makeText(this, R.string.cart_empty, Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(this, CheckoutActivity.class));
            }
        });
    }

    private void showTotals(List<CartItem> items) {
        subtotal = 0;
        for (CartItem c : items) {
            subtotal += c.price * c.quantity;
        }
        long delivery = (subtotal == 0 || subtotal >= 5000) ? 0 : 200;
        ((TextView) findViewById(R.id.tvSubtotal)).setText("Subtotal: Rs " + subtotal);
        ((TextView) findViewById(R.id.tvDelivery))
                .setText(delivery == 0 ? "Delivery: Free" : "Delivery: Rs " + delivery);
        ((TextView) findViewById(R.id.tvTotal)).setText("Total: Rs " + (subtotal + delivery));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}