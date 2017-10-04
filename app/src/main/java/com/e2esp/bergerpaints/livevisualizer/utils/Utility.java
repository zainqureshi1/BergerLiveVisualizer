package com.e2esp.bergerpaints.livevisualizer.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.Toast;

import com.e2esp.bergerpaints.livevisualizer.R;
import com.e2esp.bergerpaints.livevisualizer.models.Options;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 *
 * Created by Zain on 6/2/2017.
 */

public class Utility {

    public static int dpToPx(Context context, int dp) {
        Resources r = context.getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    public static Point getScreenSize() {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        return new Point(displayMetrics.widthPixels, displayMetrics.heightPixels);
    }

    public static double percentToColorTolerance(int percent, double maxColor) {
        // percent = color
        // 0 - 10 = 0 - 5
        // 11 - 25 = 6 - 20
        // 26 - 50 = 21 - 50
        // 51 - 75 = 51  - 127
        // 76 - 100 = 128 - 255
        double color;
        if (percent <= 10) {
            color = 0 + 5 * ((percent - 0.0) / (10.0 - 0.0));
        } else if (percent <= 25) {
            color = 6 + (20 - 6) * ((percent - 11.0) / (25.0 - 11.0));
        } else if (percent <= 50) {
            color = 21 + (50 - 21) * ((percent - 26.0) / (50.0 - 26.0));
        } else if (percent <= 75) {
            color = 51 + (127 - 51) * ((percent - 51.0) / (75.0 - 51.0));
        } else {
            color = 128 + (255 - 128) * ((percent - 76.0) / (100.0 - 76.0));
        }
        color = color / 255.0 * maxColor;
        return color;
    }

    public static int colorToPercentTolerance(double color, double maxColor) {
        int percent;
        color = color / maxColor * 255.0;
        if (color <= 5) {
            percent = (int) (0 + 10 * ((color - 0.0) / (5.0 - 0.0)));
        } else if (color <= 20) {
            percent = (int) (11 + (25 - 11) * ((color - 6.0) / (20.0 - 6.0)));
        } else if (color <= 50) {
            percent = (int) (26 + (50 - 26) * ((color - 21.0) / (50.0 - 21.0)));
        } else if (color <= 127) {
            percent = (int) (51 + (75 - 51) * ((color - 51.0) / (127.0 - 51.0)));
        } else {
            percent = (int) (76 + (100 - 76) * ((color - 128.0) / (255.0 - 128.0)));
        }
        return percent;
    }

    public static int[] hexToRgb(String hex) {
        return new int[] {
                Integer.valueOf(hex.substring(1, 3), 16),
                Integer.valueOf(hex.substring(3, 5), 16),
                Integer.valueOf(hex.substring(5, 7), 16)};
    }

    public static int[] colorIntToRgb(int color) {
        return new int[] {
                (color >> 16) & 0xFF,
                (color >> 8) & 0xFF,
                color & 0xFF};
    }

    public static int colorRgbToInt(int r, int g, int b) {
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    public static Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    public static Scalar convertScalarRgb2Hsv(Scalar rgbColor) {
        Mat pointMatHsv = new Mat();
        Mat pointMatRgb = new Mat(1, 1, CvType.CV_8UC3, rgbColor);
        Imgproc.cvtColor(pointMatRgb, pointMatHsv, Imgproc.COLOR_RGB2HSV, 3);
        double[] pointHsv = pointMatHsv.get(0, 0);
        return new Scalar(pointHsv);
    }

    public static void setChannel(Mat mat, int channel, double value) {
        // make sure have enough channels
        if (mat.channels() < channel+1)
            return;

        int rows = mat.rows();
        int cols = mat.cols();
        // check mat is continuous or not
        if (mat.isContinuous())
            mat.reshape(1, rows*cols).col(channel).setTo(new Scalar(value));
        else{
            for (int i = 0; i < rows; i++)
                mat.row(i).reshape(1, cols).col(channel).setTo(new Scalar(value));
        }
    }

    public static Bitmap matToBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }

    public static Mat bitmapToMat(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        return mat;
    }

    public static void showToast(Context context, int stringRes) {
        showToast(context, context.getString(stringRes));
    }

    public static void showToast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void saveOptions(Context context, Options options) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int savedOptionsCount = sharedPreferences.getInt("SavedOptionsCount", 0);
        savedOptionsCount++;
        sharedPreferences.edit()
                .putString("SavedOption"+savedOptionsCount, options.getTextToSave())
                .putInt("SavedOptionsCount", savedOptionsCount).apply();
    }

    public static ArrayList<Options> loadOptions(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int savedOptionsCount = sharedPreferences.getInt("SavedOptionsCount", 0);
        ArrayList<Options> options = new ArrayList<>();
        for (int i = 1; i <= savedOptionsCount; i++) {
            String optionsString = sharedPreferences.getString("SavedOption"+i, "");
            options.add(new Options(optionsString));
        }
        return options;
    }

    public static File saveToStorage(Context context, Bitmap bitmap, String fileName) {
        try {
            File appFolder = new File(Environment.getExternalStorageDirectory(), context.getString(R.string.app_name));
            if (!appFolder.exists() || !appFolder.isDirectory()) {
                if (!appFolder.mkdir()) {
                    return null;
                }
            }

            File file = new File(appFolder, fileName);
            file.createNewFile();

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(byteArray);
            fileOutputStream.flush();
            fileOutputStream.close();
            return file;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void broadcastGalleryUpdate(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(file);
            scanIntent.setData(contentUri);
            context.sendBroadcast(scanIntent);
        } else {
            Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()));
            context.sendBroadcast(intent);
        }
    }

}
