package com.yetote.greendemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class ImageSaveUtil {

    private static final String SD_PATH = UtilsKt.getContext().getExternalCacheDir().getPath() + "/MediaProjection/";
    private static final String TAG = "ImageSaveUtil";
    private static int num = 0;

    public static void saveBitmap2file(Bitmap bmp, Context context) {
        String savePath;
        String fileName = num + ".JPEG";
        num += 1;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            savePath = SD_PATH;
        } else {
//            Toast.makeText(context, "保存失败！", Toast.LENGTH_SHORT).show();
            return;
        }
        File filePic = new File(savePath + fileName);
        try {
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
//            Toast.makeText(context, "保存成功,位置:" + filePic.getAbsolutePath(), Toast.LENGTH_SHORT).show();
//            Log.e(TAG, "saveBitmap2file: "+"保存成功,位置:" + filePic.getAbsolutePath() );
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // 其次把文件插入到系统图库
//        try {
//            MediaStore.Images.Media.insertImage(context.getContentResolver(), filePic.getAbsolutePath(), fileName, null);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        // 最后通知图库更新
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + savePath + fileName)));

    }


}