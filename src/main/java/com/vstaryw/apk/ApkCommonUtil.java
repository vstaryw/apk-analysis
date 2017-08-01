package com.vstaryw.apk;

import io.sigpipe.jbsdiff.Diff;
import io.sigpipe.jbsdiff.Patch;
import org.apache.commons.io.FileUtils;
import org.apkinfo.api.util.AXmlResourceParser;
import org.apkinfo.api.util.TypedValue;
import org.apkinfo.api.util.XmlPullParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by Sumail on 2017/4/24.
 */
public class ApkCommonUtil {

    static Logger log = LoggerFactory.getLogger(ApkCommonUtil.class);

    /**
     * 分析APK文件，取得APK文件中的 包名、版本、版本码
     * @param apkPath
     * @return
     */
    public static Map<String,String> getApkBaseInfo(String apkPath){
        ZipFile zipFile;
        Map<String,String> map = new HashMap<>();
        try {
            zipFile = new ZipFile(apkPath);
            Enumeration<?> enumeration = zipFile.entries();
            ZipEntry zipEntry = null;
            while (enumeration.hasMoreElements()) {
                zipEntry = (ZipEntry) enumeration.nextElement();
                if (zipEntry.isDirectory()) {

                } else {
                    if ("androidmanifest.xml".equals(zipEntry.getName().toLowerCase())) {
                        AXmlResourceParser parser = new AXmlResourceParser();
                        parser.open(zipFile.getInputStream(zipEntry));
                        while (true) {
                            int type = parser.next();
                            if (type == XmlPullParser.END_DOCUMENT) {
                                break;
                            }
                            String name = parser.getName();
                            if(null != name && name.toLowerCase().equals("manifest")){
                                for (int i = 0; i != parser.getAttributeCount(); i++) {
                                    if ("versionName".equals(parser.getAttributeName(i))) {
                                        String versionName = getAttributeValue(parser, i);
                                        if(null == versionName){
                                            versionName = "";
                                        }
                                        map.put("versionName", versionName);
                                    } else if ("package".equals(parser.getAttributeName(i))) {
                                        String packageName = getAttributeValue(parser, i);
                                        if(null == packageName){
                                            packageName = "";
                                        }
                                        map.put("packageName", packageName);
                                    } else if("versionCode".equals(parser.getAttributeName(i))){
                                        String versionCode = getAttributeValue(parser, i);
                                        if(null == versionCode){
                                            versionCode = "";
                                        }
                                        map.put("versionCode", versionCode);
                                    }
                                }
                                break;
                            }
                        }
                    }

                }
            }
            zipFile.close();
        } catch (Exception e) {
           log.error("getApkBaseInfo is error :"+ e.getMessage(),e);
        }
        return map;
    }

    /**
     * 对apk包进行查分
     * @param oldPath 旧版本包
     * @param newPath 新版本包
     * @param outPath 查分包
     */
    public static void diffApk(String oldPath,String newPath,String outPath){
        try {
            byte[] oldFile = FileUtils.readFileToByteArray(new File(oldPath));
            byte[] newFile=  FileUtils.readFileToByteArray(new File(newPath));
            File out = new File(outPath);
            FileOutputStream fos = new FileOutputStream(out);
            Diff.diff(oldFile,newFile,fos);
        } catch (Exception e) {
           log.error("apk diff is error :"+e.getMessage(),e);
        }

    }

    /**
     * 对apk差分包进行合并
     * @param oldPath 旧版本包
     * @param path   差分包路径
     * @param outPath 合并后的路径
     */
    public static void patchApk(String oldPath,String path,String outPath){
        try {
            byte[] oldFile = FileUtils.readFileToByteArray(new File(oldPath));
            byte[] newFile = FileUtils.readFileToByteArray(new File(path));
            Patch.patch(oldFile, newFile, new FileOutputStream(outPath));
        }catch (Exception e){
            log.error("apk path is error :"+e.getMessage(),e);
        }
    }

    private static String getAttributeValue(AXmlResourceParser parser, int index) {
        int type = parser.getAttributeValueType(index);
        int data = parser.getAttributeValueData(index);
        if (type == TypedValue.TYPE_STRING) {
            return parser.getAttributeValue(index);
        }
        if (type == TypedValue.TYPE_ATTRIBUTE) {
            return String.format("?%s%08X", getPackage(data), data);
        }
        if (type == TypedValue.TYPE_REFERENCE) {
            return String.format("@%s%08X", getPackage(data), data);
        }
        if (type == TypedValue.TYPE_FLOAT) {
            return String.valueOf(Float.intBitsToFloat(data));
        }
        if (type == TypedValue.TYPE_INT_HEX) {
            return String.format("0x%08X", data);
        }
        if (type == TypedValue.TYPE_INT_BOOLEAN) {
            return data != 0 ? "true" : "false";
        }
        if (type == TypedValue.TYPE_DIMENSION) {
            return Float.toString(complexToFloat(data)) + DIMENSION_UNITS[data & TypedValue.COMPLEX_UNIT_MASK];
        }
        if (type == TypedValue.TYPE_FRACTION) {
            return Float.toString(complexToFloat(data)) + FRACTION_UNITS[data & TypedValue.COMPLEX_UNIT_MASK];
        }
        if (type >= TypedValue.TYPE_FIRST_COLOR_INT && type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return String.format("#%08X", data);
        }
        if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) {
            return String.valueOf(data);
        }
        return String.format("<0x%X, type 0x%02X>", data, type);
    }

    private static String getPackage(int id) {
        if (id >>> 24 == 1) {
            return "android:";
        }
        return "";
    }

    // ///////////////////////////////// ILLEGAL STUFF, DONT LOOK :)
    public static float complexToFloat(int complex) {
        return (float) (complex & 0xFFFFFF00) * RADIX_MULTS[(complex >> 4) & 3];
    }

    private static final float RADIX_MULTS[] =
            {
                    0.00390625F, 3.051758E-005F,
                    1.192093E-007F, 4.656613E-010F
            };
    private static final String DIMENSION_UNITS[] = { "px", "dip", "sp", "pt", "in", "mm", "", "" };
    private static final String FRACTION_UNITS[] = { "%", "%p", "", "", "", "", "", "" };

}
