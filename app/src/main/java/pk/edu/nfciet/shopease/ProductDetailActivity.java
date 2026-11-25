package pk.edu.nfciet.shopease;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "productId";

    private ImageView ivImage;
    private TextView tvTitle, tvCategory, tvPrice, tvStock, tvDescription, tvQuantity, tvStatus;
    private Button btnDecreaseQty, btnIncreaseQty, btnAddToCart;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String productId;
    private Product currentProduct;
    private int selectedQuantity = 1;
    private int maxStock = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        bindViews();

        productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (TextUtils.isEmpty(productId)) {
            productId = getIntent().getStringExtra("id");
        }

        if (TextUtils.isEmpty(productId)) {
            showErrorAndFinish("Missing product ID in intent.");
            return;
        }

        setupListeners();
        loadProductDetails(productId);
    }

    private void bindViews() {
        ivImage = findViewById(R.id.ivProductDetailImage);
        tvTitle = findViewById(R.id.tvProductDetailTitle);
        tvCategory = findViewById(R.id.tvProductDetailCategory);
        tvPrice = findViewById(R.id.tvProductDetailPrice);
        tvStock = findViewById(R.id.tvProductDetailStock);
        tvDescription = findViewById(R.id.tvProductDetailDescription);
        tvQuantity = findViewById(R.id.tvQuantity);
        tvStatus = findViewById(R.id.tvDetailStatus);
        btnDecreaseQty = findViewById(R.id.btnDecreaseQty);
        btnIncreaseQty = findViewById(R.id.btnIncreaseQty);
        btnAddToCart = findViewById(R.id.btnAddToCart);

        btnAddToCart.setEnabled(false);
    }

    private void setupListeners() {
        btnDecreaseQty.setOnClickListener(v -> {
            if (maxStock > 0 && selectedQuantity > 1) {
                selectedQuantity--;
                updateQuantityUi();
            }
        });

        btnIncreaseQty.setOnClickListener(v -> {
            if (maxStock > 0 && selectedQuantity < maxStock) {
                selectedQuantity++;
                updateQuantityUi();
            }
        });

        btnAddToCart.setOnClickListener(v -> addSelectedQuantityToCart());
    }

    private void loadProductDetails(String id) {
        showStatus("Loading product...");
        db.collection("products")
                .document(id)
                .get()
                .addOnSuccessListener(this::handleProductLoaded)
                .addOnFailureListener(e -> showStatus("Failed to load product: " + e.getMessage()));
    }

    private void handleProductLoaded(DocumentSnapshot doc) {
        if (!doc.exists()) {
            showStatus("Product not found.");
            btnAddToCart.setEnabled(false);
            return;
        }

        Product product = doc.toObject(Product.class);
        if (product == null) {
            showStatus("Unable to read product data.");
            btnAddToCart.setEnabled(false);
            return;
        }

        currentProduct = product;
        tvStatus.setVisibility(View.GONE);

        tvTitle.setText(product.title != null ? product.title : "");
        tvCategory.setText(TextUtils.isEmpty(product.category) ? "" : "Category: " + product.category);
        tvPrice.setText(String.format(Locale.getDefault(), "Rs %d", product.price));
        tvDescription.setText(product.description != null ? product.description : "");

        if (!TextUtils.isEmpty(product.imageUrl)) {
            Glide.with(this)
                    .load(product.imageUrl)
                    .placeholder(R.drawable.ic_shopping_cart)
                    .into(ivImage);
        } else {
            ivImage.setImageResource(R.drawable.ic_shopping_cart);
        }

        maxStock = (int) Math.max(0L, product.stock);
        tvStock.setText(maxStock > 0
                ? String.format(Locale.getDefault(), "In Stock: %d", maxStock)
                : "Out of Stock");

        if (maxStock <= 0) {
            selectedQuantity = 0;
            btnAddToCart.setEnabled(false);
            btnIncreaseQty.setEnabled(false);
            btnDecreaseQty.setEnabled(false);
        } else {
            selectedQuantity = 1;
            btnAddToCart.setEnabled(true);
            updateQuantityUi();
        }
        tvQuantity.setText(String.valueOf(selectedQuantity));
    }

    private void updateQuantityUi() {
        tvQuantity.setText(String.valueOf(selectedQuantity));
        btnDecreaseQty.setEnabled(selectedQuantity > 1);
        btnIncreaseQty.setEnabled(selectedQuantity < maxStock);
    }

    private void addSelectedQuantityToCart() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in first.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (currentProduct == null || maxStock <= 0 || selectedQuantity <= 0) {
            Toast.makeText(this, "Item is out of stock.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAddToCart.setEnabled(false);
        String uid = user.getUid();
        DocumentReference itemRef = db.collection("carts")
                .document(uid)
                .collection("items")
                .document(productId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(itemRef);
            int existingQty = 0;
            if (snapshot.exists()) {
                Long qtyVal = snapshot.getLong("quantity");
                if (qtyVal != null) {
                    existingQty = qtyVal.intValue();
                }
            }

            int newQty = Math.min(maxStock, existingQty + selectedQuantity);
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("productId", productId);
            itemData.put("title", currentProduct.title);
            itemData.put("price", currentProduct.price);
            itemData.put("imageUrl", currentProduct.imageUrl);
            itemData.put("quantity", newQty);

            transaction.set(itemRef, itemData, SetOptions.merge());
            return newQty;
        }).addOnSuccessListener(finalQty -> {
            btnAddToCart.setEnabled(true);
            Toast.makeText(
                    this,
                    "Added to cart (Qty in cart: " + finalQty + ")",
                    Toast.LENGTH_SHORT
            ).show();
        }).addOnFailureListener(e -> {
            btnAddToCart.setEnabled(true);
            Toast.makeText(this, "Cart update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showStatus(String message) {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(message);
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}