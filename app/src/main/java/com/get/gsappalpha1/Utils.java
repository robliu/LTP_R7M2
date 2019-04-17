package com.get.gsappalpha1;

/**
 *  ===== This work is copyrighted! Do not copy or reuse without Guidance permission! ======
 *
 *    For detail, contact www.guidance-edu.com
 *
 *
 *     Created by Rob on 1/11/2017.
 *
 *  Copy Rights 2017, Guidance Education Technologies, All Rights Reserved.
 *
 */

import java.lang.reflect.Array;
import java.lang.reflect.Field;


public class Utils {
    private static final Field TEXT_LINE_CACHED;

    static {
        Field textLineCached = null;
        try {
            textLineCached = Class.forName("android.text.TextLine").getDeclaredField("sCached");
            textLineCached.setAccessible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        TEXT_LINE_CACHED = textLineCached;
    }

    public static void clearTextLineCache() {
        // If the field was not found for whatever reason just return.
        if (TEXT_LINE_CACHED == null) return;

        Object cached = null;
        try {
            // Get reference to the TextLine sCached array.
            cached = TEXT_LINE_CACHED.get(null);
        } catch (Exception ex) {
            //
        }
        if (cached != null) {
            // Clear the array.
            int cacheLen = Array.getLength(cached);
            for (int i = 0; i < cacheLen; i ++) {
                Array.set(cached, i, null);
            }
        }
    }

    private Utils() {}
}
