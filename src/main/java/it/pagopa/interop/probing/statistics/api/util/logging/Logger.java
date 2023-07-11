package it.pagopa.interop.probing.statistics.api.util.logging;

import java.time.OffsetDateTime;

public interface Logger {

  public void logRequest(Long eserviceRecordId, Integer pollingFrequency);

  public void logFilterRequest(Long eserviceRecordId, Integer pollingFrequency,
      OffsetDateTime startDate, OffsetDateTime endDate);

  public void logQueryProgress(Double queryProgress);

  public void logMessageException(Exception exception);
}
