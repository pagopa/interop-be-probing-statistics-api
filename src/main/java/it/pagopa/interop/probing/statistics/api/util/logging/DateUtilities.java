package it.pagopa.interop.probing.statistics.api.util.logging;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class DateUtilities {

  public String changeDateFormat(String dateString) {
    DateTimeFormatter dateformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS")
        .withZone(ZoneId.systemDefault());
    ZonedDateTime date = ZonedDateTime.parse(dateString, dateformatter);
    return date.toOffsetDateTime().toString();
  }

  public OffsetDateTime zeroDate(OffsetDateTime inputDate , boolean multipleDays) {
    DateTimeFormatter dateformatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    String startDateTrunc = multipleDays ? inputDate.toString().substring(0, 11) + "00:00:00.000" : inputDate.toString().substring(0, 13) + ":00:00.000";
    ZonedDateTime date = ZonedDateTime.parse(startDateTrunc.replace("T", " "), dateformatter);
    return date.toOffsetDateTime();
  }


}
