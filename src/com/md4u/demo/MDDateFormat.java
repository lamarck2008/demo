package com.md4u.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MDDateFormat {
    // parsers for server time
    private static SimpleDateFormat server = new SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss", Locale.CHINA
    );

    public static Date parseServer(String string) {
        try {
          return server.parse(string);
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        return null;
    }

    public static String formatServer(Date date) {
        return server.format(date);
    }

    // parsers for bonus page
    private static SimpleDateFormat bonus = new SimpleDateFormat(
        "yyyy.MM.dd", Locale.CHINA
    );

    public static String formatBonus(Date date) {
        return bonus.format(date);
    }
}
