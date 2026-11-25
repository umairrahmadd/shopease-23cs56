package pk.edu.nfciet.shopease;

import com.google.firebase.firestore.DocumentId;

public class CartItem {
    @DocumentId
    public String id;
    public String title;
    public String imageUrl;
    public long price;
    public long quantity;

    public CartItem() {
    }
}