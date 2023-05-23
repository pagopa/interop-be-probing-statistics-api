package it.pagopa.interop.probing.statistics.api.util.logging.impl;



import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;
import it.pagopa.interop.probing.statistics.api.util.logging.Logger;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class LoggerImpl implements Logger {
  @Override
  public void logRequest(Long eserviceRecordId, Integer pollingFrequency) {
    log.info("Getting telemetry. eserviceRecordId={} , pollingFrequency={}", eserviceRecordId,
        pollingFrequency);
  }

  @Override
  public void logQueryProgress(Double queryProgress) {
    log.info("Query progress so far: {}%", queryProgress);
  }

  @Override
  public void logMessageException(Exception exception) {
    log.error(ExceptionUtils.getStackTrace(exception));
  }



}
