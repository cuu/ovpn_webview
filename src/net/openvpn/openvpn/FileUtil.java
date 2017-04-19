package net.openvpn.openvpn;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.io.Reader;


public class FileUtil {

    public static class FileTooLarge extends IOException {
        public FileTooLarge(String fn, long max_size) {
            super(String.format(OpenVPNApplication.resString(R.string.file_too_large), new Object[]{fn, Long.valueOf(max_size)}));
        }
    }

    public static String readFile(String path, long max_len) throws IOException {
        return readStream(new FileInputStream(path), max_len, path);
    }

    public static String readUri(Context context, Uri uri, long max_len) throws IOException {
        return readStream(context.getContentResolver().openInputStream(uri), max_len, uriBasename(uri));
    }

    public static String readAsset(Context context, String filename) throws IOException {
        return readStream(context.getResources().getAssets().open(filename), 0, filename);
    }

    public static String readFileAppPrivate(Context context, String filename) throws IOException {
        return readStream(context.openFileInput(filename), 0, filename);
    }

    public static void writeFileAppPrivate(Context context, String filename, String content) throws IOException {
        FileOutputStream fos = context.openFileOutput(filename, 0);
        try {
            fos.write(content.getBytes());
        } finally {
            fos.close();
        }
    }

    public static String readInternalProfileDat(Context context,String filename,int offset) throws IOException{
	try{	    
	    return readGzipStream(context.openFileInput(filename),filename,offset);
	}catch(IOException e){
	    e.printStackTrace();
	}
	return "";
    }
    
    public static String readProfileDat(Context context, String filename, int offset) throws IOException{
	return readGzipStream(context.getResources().getAssets().open(filename),filename,offset);
    }
    
    public static String readGzipStream(InputStream inputStream,String string2, int offset) throws IOException{
	try{
	    inputStream.skip(offset);
	    Reader fr = new InputStreamReader(new GZIPInputStream(inputStream), "UTF-8");
	    
            BufferedReader br = new BufferedReader(fr);
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
	    return sb.toString();
	}
	finally{
	    inputStream.close();
	}
    }
    
    public static String readStream(InputStream inputStream, long l, String string2) throws IOException {
        try {
            int n;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            char[] arrc = new char[4096];
            while ((n = bufferedReader.read(arrc, 0, arrc.length)) > 0) {
                stringBuilder.append(arrc, 0, n);
                if (l <= 0 || (long)stringBuilder.length() <= l) continue;
                throw new FileTooLarge(string2, l);
            }
            String string3 = stringBuilder.toString();
            return string3;
        }
        finally {
            inputStream.close();
        }
    }

    public static byte[] readFileByteArray(String path, long max_len) throws IOException {
        File file = new File(path);
        InputStream is = new FileInputStream(file);
        try {
            long length = file.length();
            if ((max_len <= 0 || length <= max_len) && length <= Integer.MAX_VALUE) {
                byte[] bytes = new byte[((int) length)];
                int offset = 0;
                while (offset < bytes.length) {
                    int numRead = is.read(bytes, offset, bytes.length - offset);
                    if (numRead < 0) {
                        break;
                    }
                    offset += numRead;
                }
                if (offset >= bytes.length) {
                    return bytes;
                }
                throw new IOException("Could not completely read file: " + path);
            }
            throw new FileTooLarge(path, max_len);
        } finally {
            is.close();
        }
    }

    public static boolean deleteFile(String path) {
        if (path != null) {
            return new File(path).delete();
        }
        return false;
    }

    public static boolean renameFile(String from_path, String to_path) {
        if (from_path == null || to_path == null) {
            return false;
        }
        return new File(from_path).renameTo(new File(to_path));
    }

    public static String basename(String path) {
        if (path != null) {
            return new File(path).getName();
        }
        return null;
    }

    public static String dirname(String path) {
        if (path != null) {
            return new File(path).getParent();
        }
        return null;
    }

    public static String uriBasename(Uri uri) {
        if (uri != null) {
            return uri.getLastPathSegment();
        }
        return null;
    }
}
