package com.example.blueshot;

import android.graphics.Bitmap;
import android.util.Log;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;


public class AndroidBmpUtil {

    private static final int BMP_WIDTH_OF_TIMES = 4;
    private static final int BYTE_PER_PIXEL = 1;

    public static int IMAGE_WIDTH;
    public static int IMAGE_HEIGHT;

    public static boolean createBMPImage(Bitmap bitmap, String parentActivity) throws IOException {
        long start = System.currentTimeMillis();
        if(bitmap == null){
            return false;
        }

        //image size
        int width = bitmap.getWidth();
        Log.i("width is", " " + width);
        int height = bitmap.getHeight();
        Log.i("height is", " " + height);

        //image dummy data size
        //reason : the amount of bytes per image row must be a multiple of 4 (requirements of bmp format)
        byte[] dummyBytesPerRow = null;
        boolean hasDummy = false;
        int rowWidthInBytes = BYTE_PER_PIXEL * width; //source image width * number of bytes to encode one pixel.
        if (rowWidthInBytes % BMP_WIDTH_OF_TIMES > 0){
            hasDummy=true;
            //the number of dummy bytes we need to add on each row
            dummyBytesPerRow = new byte[(BMP_WIDTH_OF_TIMES - (rowWidthInBytes % BMP_WIDTH_OF_TIMES))];
            Log.i("dummy bytes per row", dummyBytesPerRow.length + " bytes");
            //just fill an array with the dummy bytes we need to append at the end of each row
            Arrays.fill(dummyBytesPerRow, (byte) 0xFF);
        }

        //an array to receive the pixels from the source image
        int[] pixels = new int[width * height];

        //the number of bytes used in the file to store raw image data (excluding file headers)
        int imageSize = (rowWidthInBytes + (hasDummy ? dummyBytesPerRow.length:0)) * height;
        Log.i("image size", imageSize + " bytes");
        //file headers size
        //int imageDataOffset = 0x36;
        int imageDataOffset = 1078;

        //final size of the file
        int fileSize = imageSize + imageDataOffset;
        Log.i("filesize", fileSize + " bytes");

        //Android Bitmap Image Data
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        //ByteArrayOutputStream baos = new ByteArrayOutputStream(fileSize);
        ByteBuffer buffer = ByteBuffer.allocate(fileSize);
        ByteBuffer headerBuffer = ByteBuffer.allocate(imageDataOffset);
        ByteBuffer imageBuffer  = ByteBuffer.allocate(imageSize);
        /*
         * BITMAP FILE HEADER Write Start
         */
        buffer.put((byte)0x42);
        buffer.put((byte)0x4D);
        headerBuffer.put((byte)0x42);
        headerBuffer.put((byte)0x4D);

        //size
        buffer.put(writeInt(fileSize));
        headerBuffer.put(writeInt(fileSize));

        //reserved
        buffer.put(writeShort((short)0));
        buffer.put(writeShort((short)0));
        headerBuffer.put(writeShort((short)0));
        headerBuffer.put(writeShort((short)0));

        //image data start offset
        buffer.put(writeInt(imageDataOffset));
        headerBuffer.put(writeInt(imageDataOffset));

        /* BITMAP FILE HEADER Write End */

        //*******************************************

        /* BITMAP INFO HEADER Write Start */
        //size
        buffer.put(writeInt(0x28));
        headerBuffer.put(writeInt(0x28));

        IMAGE_WIDTH = width + (hasDummy ? (dummyBytesPerRow.length == 3 ? 1 : 0) : 0);
        IMAGE_HEIGHT = height;

        //width, height
        //if we add 3 dummy bytes per row : it means we add a pixel (and the image width is modified.
        buffer.put(writeInt(IMAGE_WIDTH));
        buffer.put(writeInt(height));
        headerBuffer.put(writeInt(IMAGE_WIDTH));
        headerBuffer.put(writeInt(height));

        //planes
        buffer.put(writeShort((short)1));
        headerBuffer.put(writeShort((short)1));

        //bit count
        //buffer.put(writeShort((short)16));
        buffer.put(writeShort((short)8));
        headerBuffer.put(writeShort((short)8));

        //bit compression
        buffer.put(writeInt(0));
        headerBuffer.put(writeInt(0));

        //image data size
        buffer.put(writeInt(imageSize));
        headerBuffer.put(writeInt(imageSize));

        //horizontal resolution in pixels per meter
        buffer.put(writeInt(0));
        headerBuffer.put(writeInt(0));

        //vertical resolution in pixels per meter (unreliable)
        buffer.put(writeInt(0));
        headerBuffer.put(writeInt(0));

        //number of colors being used - optimized
        buffer.put(writeInt(0));
        headerBuffer.put(writeInt(0));

        //number of important colors being used
        buffer.put(writeInt(0));
        headerBuffer.put(writeInt(0));

        /* BITMAP INFO HEADER Write End */

        /* COLOR PALETTE Start */

        for (int i = 0; i < 256; i++) {
            byte red8, green8, blue8;
            red8 = (byte) (i & 0x000000E0);
            green8 = (byte) (i & 0x0000001C);
            blue8 = (byte) (i & 0x00000003);

            int RGBColor = (blue8 << 6) | (green8 << 11) | (red8 << 16);
            buffer.put(writeInt(RGBColor));
            headerBuffer.put(writeInt(RGBColor));
        }

        CameraActivity.headerByteArray = headerBuffer.array();
        Log.i("headerbytearray length is ", " "+CameraActivity.headerByteArray.length);
        /* COLOR PALETTE End */

        /* BITMAP PIXELS Start */

        int row = height;
        int col = width;
        int startPosition = 0;
        int endPosition = 0;
        while( row > 0 ){

            startPosition = (row - 1) * col;
            endPosition = row * col;

            for(int i = startPosition; i < endPosition; i++ ){
                byte red8, green8, blue8;
                red8 = (byte) ((pixels[i] & 0x00E00000) >> 21);
                green8 = (byte) ((pixels[i] & 0x0000E000) >> 13);
                blue8 = (byte) ((pixels[i] & 0x000000C0) >> 6);

                byte RGB1Byte = (byte) (blue8 | (green8 << 2) | (red8 << 5));
                buffer.put(RGB1Byte);
                imageBuffer.put(RGB1Byte);
            }

            if(hasDummy){
                buffer.put(dummyBytesPerRow);
                imageBuffer.put(dummyBytesPerRow);
            }
            row--;
        }

        /* BITMAP PIXELS End */
        Log.v("AndroidBmpUtil" ,System.currentTimeMillis()-start+" ms");
        if (parentActivity.equals("CameraActivity")) {
            CameraActivity.imageArray = imageBuffer.array();
            CameraActivity.byteArray = buffer.array();
            Log.i("bytearray length is ", " "+CameraActivity.byteArray.length);
        }
        else if (parentActivity.equals("MainActivity")) {
            MainActivity.uploadByteArray = buffer.array();
        }
        else {
            return false;
        }

        return true;
    }

    public static boolean save(String parentActivity, String filePath) throws IOException {
        if(filePath == null){
            return false;
        }

        FileOutputStream fos = new FileOutputStream(filePath);
        if (parentActivity.equals("CameraActivity")) {
            fos.write(CameraActivity.byteArray);        }
        else if (parentActivity.equals("MainActivity")) {
            fos.write(MainActivity.uploadByteArray);
        }
        else {
            return false;
        }

        fos.close();

        return true;
    }

    /**
     * Write integer to little-endian
     * @param value
     * @return
     * @throws IOException
     */
    private static byte[] writeInt(int value) throws IOException {
        byte[] b = new byte[4];

        b[0] = (byte)(value & 0x000000FF);
        b[1] = (byte)((value & 0x0000FF00) >> 8);
        b[2] = (byte)((value & 0x00FF0000) >> 16);
        b[3] = (byte)((value & 0xFF000000) >> 24);

        return b;
    }

    /**
     * Write short to little-endian byte array
     * @param value
     * @return
     * @throws IOException
     */
    private static byte[] writeShort(short value) throws IOException {
        byte[] b = new byte[2];

        b[0] = (byte)(value & 0x00FF);
        b[1] = (byte)((value & 0xFF00) >> 8);

        return b;
    }
}
