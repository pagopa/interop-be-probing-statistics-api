package it.pagopa.interop.probing.statistics.api.util.logging;

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

}
