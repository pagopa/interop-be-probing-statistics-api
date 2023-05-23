package it.pagopa.interop.probing.statistics.api.util.logging;

public interface Logger {

  public void logRequest(Long eserviceRecordId, Integer pollingFrequency);

  public void logQueryProgress(Double queryProgress);
}
