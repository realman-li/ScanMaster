package com.example.scanmaster;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.IOException;

public class ImageEditActivity extends AppCompatActivity {

    private ImageView ivEditImage;
    private Button btnOriginal, btnBlackWhite, btnGray, btnEnhance;
    private Button btnResetEdge, btnCorrect, btnSavePdf;

    private Bitmap originalBitmap;
    private Bitmap currentBitmap;
    private PointF[] documentPoints;
    private int currentFilterMode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_edit);

        ivEditImage = findViewById(R.id.iv_edit_image);
        btnOriginal = findViewById(R.id.btn_original);
        btnBlackWhite = findViewById(R.id.btn_black_white);
        btnGray = findViewById(R.id.btn_gray);
        btnEnhance = findViewById(R.id.btn_enhance);
        btnResetEdge = findViewById(R.id.btn_reset_edge);
        btnCorrect = findViewById(R.id.btn_correct);
        btnSavePdf = findViewById(R.id.btn_save_pdf);

        String photoUriStr = getIntent().getStringExtra("photo_uri");
        if (photoUriStr == null) {
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Uri photoUri = Uri.parse(photoUriStr);
        try {
            originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
            currentBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
            documentPoints = OpenCVUtils.findDocumentContours(originalBitmap);
            Glide.with(this).load(currentBitmap).into(ivEditImage);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnOriginal.setOnClickListener(v -> applyFilter(0));
        btnBlackWhite.setOnClickListener(v -> applyFilter(1));
        btnGray.setOnClickListener(v -> applyFilter(2));
        btnEnhance.setOnClickListener(v -> applyFilter(3));

        btnResetEdge.setOnClickListener(v -> {
            documentPoints = OpenCVUtils.findDocumentContours(originalBitmap);
            currentBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
            applyFilter(currentFilterMode);
            Toast.makeText(this, "已重置边缘", Toast.LENGTH_SHORT).show();
        });

        btnCorrect.setOnClickListener(v -> {
            if (documentPoints == null || documentPoints.length != 4) {
                Toast.makeText(this, "未检测到文档边缘", Toast.LENGTH_SHORT).show();
                return;
            }
            currentBitmap = OpenCVUtils.perspectiveTransform(currentBitmap, documentPoints);
            applyFilter(currentFilterMode);
            Toast.makeText(this, "文档矫正完成", Toast.LENGTH_SHORT).show();
        });

        btnSavePdf.setOnClickListener(v -> {
            if (currentBitmap == null) {
                Toast.makeText(this, "无有效图片", Toast.LENGTH_SHORT).show();
                return;
            }
            PDFUtils.bitmapToPdf(this, currentBitmap);
        });
    }

    private void applyFilter(int mode) {
        currentFilterMode = mode;
        Bitmap filteredBitmap = OpenCVUtils.enhanceImage(currentBitmap, mode);
        currentBitmap = filteredBitmap;
        Glide.with(this).load(currentBitmap).into(ivEditImage);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
        }
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
        }
    }
}
