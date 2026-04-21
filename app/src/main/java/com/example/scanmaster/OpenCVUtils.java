package com.example.scanmaster;

import android.graphics.Bitmap;
import android.graphics.PointF;

import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OpenCVUtils {

    // 自动查找文档边缘，返回4个顶点
    public static PointF[] findDocumentContours(Bitmap bitmap) {
        Mat src = new Mat();
        Mat gray = new Mat();
        Mat blur = new Mat();
        Mat canny = new Mat();

        Utils.bitmapToMat(bitmap, src);
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray, blur, new Size(5, 5), 0);
        Imgproc.Canny(blur, canny, 50, 150);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(canny, canny, kernel);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(canny, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        Collections.sort(contours, (o1, o2) -> Double.compare(Imgproc.contourArea(o2), Imgproc.contourArea(o1)));

        for (MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            double perimeter = Imgproc.arcLength(contour2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(contour2f, approx, 0.02 * perimeter, true);

            if (approx.total() == 4) {
                Point[] points = approx.toArray();
                PointF[] result = new PointF[4];
                for (int i = 0; i < 4; i++) {
                    result[i] = new PointF((float) points[i].x, (float) points[i].y);
                }
                releaseMats(src, gray, blur, canny, hierarchy);
                return sortPoints(result);
            }
        }

        releaseMats(src, gray, blur, canny, hierarchy);
        return new PointF[]{
                new PointF(0, 0),
                new PointF(bitmap.getWidth(), 0),
                new PointF(bitmap.getWidth(), bitmap.getHeight()),
                new PointF(0, bitmap.getHeight())
        };
    }

    // 透视矫正，把歪的文档拉正
    public static Bitmap perspectiveTransform(Bitmap bitmap, PointF[] points) {
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);

        PointF[] sortedPoints = sortPoints(points);
        Point tl = new Point(sortedPoints[0].x, sortedPoints[0].y);
        Point tr = new Point(sortedPoints[1].x, sortedPoints[1].y);
        Point br = new Point(sortedPoints[2].x, sortedPoints[2].y);
        Point bl = new Point(sortedPoints[3].x, sortedPoints[3].y);

        double widthTop = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));
        double widthBottom = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        int maxWidth = (int) Math.max(widthTop, widthBottom);

        double heightLeft = Math.sqrt(Math.pow(bl.x - tl.x, 2) + Math.pow(bl.y - tl.y, 2));
        double heightRight = Math.sqrt(Math.pow(br.x - tr.x, 2) + Math.pow(br.y - tr.y, 2));
        int maxHeight = (int) Math.max(heightLeft, heightRight);

        MatOfPoint2f dst = new MatOfPoint2f(
                new Point(0, 0),
                new Point(maxWidth - 1, 0),
                new Point(maxWidth - 1, maxHeight - 1),
                new Point(0, maxHeight - 1)
        );

        MatOfPoint2f srcMat = new MatOfPoint2f(tl, tr, br, bl);
        Mat transform = Imgproc.getPerspectiveTransform(srcMat, dst);
        Mat resultMat = new Mat(maxHeight, maxWidth, src.type());
        Imgproc.warpPerspective(src, resultMat, transform, resultMat.size());

        Bitmap resultBitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resultMat, resultBitmap);

        releaseMats(src, transform, resultMat, srcMat, dst);
        return resultBitmap;
    }

    // 文档滤镜增强（4种模式，和夸克扫描王一致）
    public static Bitmap enhanceImage(Bitmap bitmap, int mode) {
        Mat src = new Mat();
        Mat result = new Mat();
        Utils.bitmapToMat(bitmap, src);

        switch (mode) {
            case 0: // 原图
                result = src.clone();
                break;
            case 1: // 黑白文档
                Imgproc.cvtColor(src, result, Imgproc.COLOR_BGR2GRAY);
                Imgproc.adaptiveThreshold(result, result, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 15, 8);
                break;
            case 2: // 灰度增强
                Imgproc.cvtColor(src, result, Imgproc.COLOR_BGR2GRAY);
                Core.normalize(result, result, 0, 255, Core.NORM_MINMAX);
                break;
            case 3: // 去阴影增强
                Mat lab = new Mat();
                Imgproc.cvtColor(src, lab, Imgproc.COLOR_BGR2Lab);
                List<Mat> labChannels = new ArrayList<>();
                Core.split(lab, labChannels);
                Mat lChannel = labChannels.get(0);
                Imgproc.GaussianBlur(lChannel, lChannel, new Size(3, 3), 0);
                Core.normalize(lChannel, lChannel, 0, 255, Core.NORM_MINMAX);
                Core.merge(labChannels, lab);
                Imgproc.cvtColor(lab, result, Imgproc.COLOR_Lab2BGR);
                releaseMats(lab, lChannel);
                break;
        }

        Bitmap resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, resultBitmap);

        releaseMats(src, result);
        return resultBitmap;
    }

    // 顶点排序（左上、右上、右下、左下）
    private static PointF[] sortPoints(PointF[] points) {
        PointF[] sorted = new PointF[4];
        Arrays.sort(points, (a, b) -> Float.compare(a.x + a.y, b.x + b.y));
        sorted[0] = points[0];
        sorted[2] = points[3];

        Arrays.sort(points, (a, b) -> Float.compare(a.x - a.y, b.x - b.y));
        sorted[1] = points[3];
        sorted[3] = points[0];

        return sorted;
    }

    // 释放内存，避免闪退
    private static void releaseMats(Mat... mats) {
        for (Mat mat : mats) {
            if (mat != null && !mat.isReleased()) {
                mat.release();
            }
        }
    }
}
