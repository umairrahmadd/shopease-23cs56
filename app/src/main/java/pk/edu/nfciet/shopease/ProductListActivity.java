package pk.edu.nfciet.shopease;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProductListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_product_list);

        View toolbar = findViewById(R.id.toolbar);
        int toolbarBaseHeight = toolbar.getLayoutParams().height;
        int toolbarBasePaddingTop = toolbar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    toolbarBasePaddingTop + bars.top,
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );
            if (toolbarBaseHeight > 0) {
                v.getLayoutParams().height = toolbarBaseHeight + bars.top;
                v.requestLayout();
            }
            return insets;
        });

        Button btnLogout = findViewById(R.id.btnLogout);
        int logoutBaseMarginBottom = ((android.view.ViewGroup.MarginLayoutParams) btnLogout.getLayoutParams()).bottomMargin;
        ViewCompat.setOnApplyWindowInsetsListener(btnLogout, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams lp =
                    (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.bottomMargin = logoutBaseMarginBottom + bars.bottom;
            v.setLayoutParams(lp);
            return insets;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }

        TextView tvHello = findViewById(R.id.tvHello);
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    if (name != null && !name.trim().isEmpty()) {
                        tvHello.setText("Hi, " + name.trim());
                    }
                });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            goToLogin();
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}