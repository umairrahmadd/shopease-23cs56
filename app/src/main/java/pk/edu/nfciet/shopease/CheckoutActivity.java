package pk.edu.nfciet.shopease;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {

    private EditText etName, etPhone, etCity, etAddress;
    private TextView tvItemsCount, tvSubtotal, tvDelivery, tvTotal;
    private Button btnPlace;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference cartRef;
    private List<DocumentSnapshot> cartDocs = new ArrayList<>();
    private long subtotal, delivery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        View root = findViewById(R.id.rootCheckout);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etCity = findViewById(R.id.etCity);
        etAddress = findViewById(R.id.etAddress);
        tvItemsCount = findViewById(R.id.tvItemsCount);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvDelivery = findViewById(R.id.tvDelivery);
        tvTotal = findViewById(R.id.tvTotal);
        btnPlace = findViewById(R.id.btnPlaceOrder);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            finish();
            return;
        }

        cartRef = db.collection("carts").document(uid).collection("items");

        cartRef.get().addOnSuccessListener(snap -> {
            cartDocs = snap.getDocuments();
            long itemCount = 0;
            subtotal = 0;
            for (DocumentSnapshot d : cartDocs) {
                Long price = d.getLong("price");
                Long qty = d.getLong("quantity");
                if (price != null && qty != null) {
                    itemCount += qty;
                    subtotal += price * qty;
                }
            }
            delivery = (subtotal == 0 || subtotal >= 5000) ? 0 : 200;
            long total = subtotal + delivery;

            tvItemsCount.setText(getString(R.string.summary_items_fmt, itemCount));
            tvSubtotal.setText(getString(R.string.summary_subtotal_fmt, subtotal));
            if (delivery == 0) {
                tvDelivery.setText(R.string.summary_delivery_free);
            } else {
                tvDelivery.setText(getString(R.string.summary_delivery_fmt, delivery));
            }
            tvTotal.setText(getString(R.string.summary_total_fmt, total));
        });

        btnPlace.setOnClickListener(v -> placeOrder());
    }

    private void placeOrder() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError(getString(R.string.err_full_name_required));
            return;
        }
        if (!phone.matches("03\\d{9}")) {
            etPhone.setError(getString(R.string.err_phone_required));
            return;
        }
        if (city.isEmpty()) {
            etCity.setError(getString(R.string.err_city_required));
            return;
        }
        if (address.isEmpty()) {
            etAddress.setError(getString(R.string.err_address_required));
            return;
        }
        if (cartDocs.isEmpty()) {
            Toast.makeText(this, R.string.cart_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        List items = new ArrayList();
        for (DocumentSnapshot d : cartDocs) {
            Map row = new HashMap();
            row.put("productId", d.getId());
            row.put("title", d.getString("title"));
            row.put("price", d.getLong("price"));
            row.put("quantity", d.getLong("quantity"));
            items.add(row);
        }

        Map deliveryAddress = new HashMap();
        deliveryAddress.put("name", name);
        deliveryAddress.put("phone", phone);
        deliveryAddress.put("city", city);
        deliveryAddress.put("street", address);

        Map order = new HashMap();
        order.put("userId", FirebaseAuth.getInstance().getUid());
        order.put("items", items);
        order.put("subtotal", subtotal);
        order.put("deliveryFee", delivery);
        order.put("total", subtotal + delivery);
        order.put("address", deliveryAddress);
        order.put("paymentMethod", "COD");
        order.put("status", "placed");
        order.put("createdAt", FieldValue.serverTimestamp());

        btnPlace.setEnabled(false);

        WriteBatch batch = db.batch();
        DocumentReference orderRef = db.collection("orders").document();
        batch.set(orderRef, order);
        for (DocumentSnapshot d : cartDocs) {
            batch.delete(d.getReference());
        }

        batch.commit()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, getString(R.string.order_placed_fmt, orderRef.getId()), Toast.LENGTH_LONG).show();
                    Intent i = new Intent(this, ProductListActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .addOnFailureListener(e -> {
                    btnPlace.setEnabled(true);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}