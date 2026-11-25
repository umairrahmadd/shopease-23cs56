package pk.edu.nfciet.shopease;

import com.google.firebase.firestore.DocumentId;

public class Product {
    @DocumentId
    public String id;
    public String title;
    public String description;
    public String category;
    public String imageUrl;
    public long price;
    public long stock;

    public Product() {
    }
}