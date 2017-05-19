package helpers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Created by cakor on 12/2/2016.
 */
public class DateHelper {
public static Date longToDate(long lon)
{
    return new Date(lon);
}
    public static long dateToLong(Date d)
    {
        return d.getTime();
    }

    public static long localDateTimeToEpoch(LocalDateTime date){
       return date.toEpochSecond(ZoneOffset.UTC);
    }

    public static LocalDateTime epochToLocalDateTime(long epoch){
        return LocalDateTime.from(Instant.ofEpochMilli(epoch));
    }
}
