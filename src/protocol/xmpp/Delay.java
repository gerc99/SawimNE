package protocol.xmpp;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by gerc on 11.03.2015.
 */
public class Delay {

    public static final DateFormat XEP_0091_UTC_FORMAT = new SimpleDateFormat(
            "yyyyMMdd'T'HH:mm:ss");
    private static final SimpleDateFormat XEP_0091_UTC_FALLBACK_FORMAT = new SimpleDateFormat(
            "yyyyMd'T'HH:mm:ss");
    private static final SimpleDateFormat XEP_0082_UTC_FORMAT_WITHOUT_MILLIS = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'");
    public static final DateFormat XEP_0082_UTC_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private static Map<String, DateFormat> formats = new HashMap<String, DateFormat>();
    static {
        formats.put("^\\d+T\\d+:\\d+:\\d+$", XEP_0091_UTC_FORMAT);
        formats.put("^\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+Z$", XEP_0082_UTC_FORMAT);
        formats.put("^\\d+-\\d+-\\d+T\\d+:\\d+:\\d+Z$", XEP_0082_UTC_FORMAT_WITHOUT_MILLIS);
    }

    static {
        XEP_0091_UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        XEP_0091_UTC_FALLBACK_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        XEP_0082_UTC_FORMAT_WITHOUT_MILLIS.setTimeZone(TimeZone.getTimeZone("UTC"));
        XEP_0082_UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public long getTime(String stampString) {
        Date stamp = null;
        DateFormat format = null;

        if (stampString != null)
            for (String regexp : formats.keySet()) {
                if (stampString.matches(regexp)) {
                    try {
                        format = formats.get(regexp);
                        synchronized (format) {
                            stamp = format.parse(stampString);
                        }
                    } catch (ParseException e) {
                        // do nothing, format is still set
                    }

                    // break because only one regexp can match
                    break;
                }
            }

        /*
         * if date is in XEP-0091 format handle ambiguous dates missing the
         * leading zero in month and day
         */
        if (stampString != null
                && format == XEP_0091_UTC_FORMAT
                && stampString.split("T")[0].length() < 8) {
            stamp = handleDateWithMissingLeadingZeros(stampString);
        }

        /*
         * if date could not be parsed but XML is valid, don't shutdown
         * connection by throwing an exception instead set timestamp to current
         * time
         */
        if (stamp == null) {
            stamp = new Date();
        }
        return stamp.getTime();
    }

    private Date handleDateWithMissingLeadingZeros(String stampString) {
        Calendar now = new GregorianCalendar();
        Calendar xep91 = parseXEP91Date(stampString, XEP_0091_UTC_FORMAT);
        Calendar xep91Fallback = parseXEP91Date(stampString, XEP_0091_UTC_FALLBACK_FORMAT);
        List<Calendar> dates = filterDatesBefore(now, xep91, xep91Fallback);
        if (!dates.isEmpty()) {
            return determineNearestDate(now, dates).getTime();
        }
        return null;
    }

    private Calendar parseXEP91Date(String stampString, DateFormat dateFormat) {
        try {
            synchronized (dateFormat) {
                dateFormat.parse(stampString);
                return dateFormat.getCalendar();
            }
        }
        catch (ParseException e) {
            return null;
        }
    }

    private List<Calendar> filterDatesBefore(Calendar now, Calendar... dates) {
        List<Calendar> result = new ArrayList<Calendar>();

        for (Calendar calendar : dates) {
            if (calendar != null && calendar.before(now)) {
                result.add(calendar);
            }
        }

        return result;
    }

    private Calendar determineNearestDate(final Calendar now, List<Calendar> dates) {
        Collections.sort(dates, new Comparator<Calendar>() {

            public int compare(Calendar o1, Calendar o2) {
                Long diff1 = new Long(now.getTimeInMillis() - o1.getTimeInMillis());
                Long diff2 = new Long(now.getTimeInMillis() - o2.getTimeInMillis());
                return diff1.compareTo(diff2);
            }

        });

        return dates.get(0);
    }
}
