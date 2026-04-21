package com.example.scanmaster;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.widget.Toast;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.io.image.ImageDataFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PDFUtils {

    public static File bitmapToPdf(Context context, Bitmap bitmap) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
        String fileName = "扫描_" + timeStamp + ".pdf";
        File pdfFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

        try {
            PdfWriter writer = new PdfWriter(pdfFile);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] bitmapData = stream.toByteArray();

            Image image = new Image(ImageDataFactory.create(bitmapData));
            image.setAutoScale(true);
            document.add(image);

            document.close();
            pdfDoc.close();
            stream.close();

            Toast.makeText(context, "PDF已保存到下载目录：" + fileName, Toast.LENGTH_LONG).show();
            return pdfFile;

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "PDF生成失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}
