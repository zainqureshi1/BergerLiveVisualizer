package com.e2esp.bergerpaints.livevisualizer.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Zain on 7/26/2017.
 */

public class Product implements Parcelable {
    private String name;
    private String description;
    private String link;
    private int imageRes;

    public Product(String name, String description, String link, int imageRes) {
        this.name = name;
        this.description = description;
        this.link = link;
        this.imageRes = imageRes;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLink() {
        return link;
    }

    public int getImageRes() {
        return imageRes;
    }
    
    public Product(Parcel in) {
        this.name = in.readString();
        this.description = in.readString();
        this.link = in.readString();
        this.imageRes = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getName());
        dest.writeString(getDescription());
        dest.writeString(getLink());
        dest.writeInt(getImageRes());
    }

    public static final Parcelable.Creator<Product> CREATOR = new Parcelable.Creator<Product>() {
        public Product createFromParcel(Parcel p) {
            Product product = new Product(p);
            if (product == null) {
                throw new RuntimeException("Failed to unparcel Product");
            }
            return product;
        }
        public Product[] newArray(int size) {
            return new Product[size];
        }
    };

}
