package com.felixunlimited.word.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;

import android.util.Log;


/**
 * @author ajeet
 *05-Dec-2014  2014
 *
 */
public class StorageUtil {

    public static boolean isRemovebleSDCardMounted() {
        File file = new File("/sys/class/block/");
        File[] files = file.listFiles(new MmcblkFilter("mmcblk\\d$"));
        boolean flag = false;
        for (File mmcfile : files) {
            File scrfile = new File(mmcfile, "device/scr");
            if (scrfile.exists()) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    public static String getRemovebleSDCardPath() throws IOException {
        String sdpath = null;
        File file = new File("/sys/class/block/");
        File[] files = file.listFiles(new MmcblkFilter("mmcblk\\d$"));
        String sdcardDevfile = null;
        for (File mmcfile : files) {
            Log.d("SDCARD", mmcfile.getAbsolutePath());
            File scrfile = new File(mmcfile, "device/scr");
            if (scrfile.exists()) {
                sdcardDevfile = mmcfile.getName();
                Log.d("SDCARD", mmcfile.getName());
                break;
            }
        }
        if (sdcardDevfile == null) {
            return null;
        }
        FileInputStream is;
        BufferedReader reader;

        files = file.listFiles(new MmcblkFilter(sdcardDevfile + "p\\d+"));
        String deviceName = null;
        if (files.length > 0) {
            Log.d("SDCARD", files[0].getAbsolutePath());
            File devfile = new File(files[0], "dev");
            if (devfile.exists()) {
                FileInputStream fis = new FileInputStream(devfile);
                reader = new BufferedReader(new InputStreamReader(fis));
                String line = reader.readLine();
                deviceName = line;
            }
            Log.d("SDCARD", "" + deviceName);
            if (deviceName == null) {
                return null;
            }
            Log.d("SDCARD", deviceName);

            final File mountFile = new File("/proc/self/mountinfo");

            if (mountFile.exists()) {
                is = new FileInputStream(mountFile);
                reader = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    // Log.d("SDCARD", line);
                    // line = reader.readLine();
                    // Log.d("SDCARD", line);
                    String[] mPonts = line.split("\\s+");
                    if (mPonts.length > 6) {
                        if (mPonts[2].trim().equalsIgnoreCase(deviceName)) {
                            if (mPonts[4].contains(".android_secure")
                                    || mPonts[4].contains("asec")) {
                                continue;
                            }
                            sdpath = mPonts[4];
                            Log.d("SDCARD", mPonts[4]);

                        }
                    }

                }
            }

        }

        return sdpath;
    }

    static class MmcblkFilter implements FilenameFilter {
        private String pattern;

        public MmcblkFilter(String pattern) {
            this.pattern = pattern;

        }

        @Override
        public boolean accept(File dir, String filename) {
            if (filename.matches(pattern)) {
                return true;
            }
            return false;
        }

    }

}