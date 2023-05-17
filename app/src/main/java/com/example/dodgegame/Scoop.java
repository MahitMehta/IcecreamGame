package com.example.dodgegame;

import android.graphics.Bitmap;

public class Scoop {
    public Bitmap image;
    public int left, bottom;
    public Scoop(Bitmap image, int left, int bottom){
        this.image = image;
        this.left = left;
        this.bottom = bottom;
    }
}
