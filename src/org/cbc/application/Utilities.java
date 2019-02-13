package org.cbc.application;

/**
 * Provides static methods that perform a number of useful low level functions.
 * The functions are used by Application reporting but may be useful in other
 * situations.
 *
 * @version <b>v1.0, 11/Jun/01, C.B. Close:</b> Initial version.
 * @version <b>v1.1, 11/Jul/01, C.B. Close:</b> Implement Serializable.
 * @version <b>v1.1, 18/Nov/01, C.B. Close:</b> Match.
 */
import java.io.Serializable;
import java.text.DateFormatSymbols;
import java.util.Calendar;

public class Utilities implements Serializable {
    private static transient final DateFormatSymbols dateSymbols   = new DateFormatSymbols();
    private static transient final String[]          shortWeekdays = dateSymbols.getShortWeekdays();
    private static transient final String[]          shortMonths   = dateSymbols.getShortMonths();

    private static String digits(int value, int length) {
        String number = "";

        number = number + value;
        while (number.length() < length) {
            number = "0" + number;
        }
        return number;
    }

    private static String timeParam(char format) {
        Calendar calendar = Calendar.getInstance();

        switch (format) {
            case 'a':
                return shortWeekdays[calendar.get(Calendar.DAY_OF_WEEK)];
            case 'b':
                return shortMonths[calendar.get(Calendar.MONTH)];
            case 'd':
                return digits(calendar.get(Calendar.DAY_OF_MONTH), 2);
            case 'm':
                return digits(calendar.get(Calendar.MONTH) + 1, 2);
            case 'H':
                return digits(calendar.get(Calendar.HOUR_OF_DAY), 2);
            case 'M':
                return digits(calendar.get(Calendar.MINUTE), 2);
            case 'T':
                return digits(calendar.get(Calendar.MILLISECOND), 3);
            case 'S':
                return digits(calendar.get(Calendar.SECOND), 2);
            case 'y':
                return digits(calendar.get(Calendar.YEAR) % 100, 2);
            case 'Y':
                return digits(calendar.get(Calendar.YEAR), 2);
            case '%':
                return "%";
        }
        return "%" + format;
    }

    public static String formatTime(String format) {
        Token  token  = new Token(format);
        String output = "";

        while (token.moreCharacters()) {
            char chr = token.nextCharacter();

            if (chr == '%') {
                output += timeParam(token.nextCharacter());
            } else {
                output += chr;
            }
        }
        return output;
    }
}