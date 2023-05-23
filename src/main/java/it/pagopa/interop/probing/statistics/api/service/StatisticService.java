package it.pagopa.interop.probing.statistics.api.service;

import java.io.IOException;
import java.text.ParseException;
import it.pagopa.interop.probing.statistics.dtos.StatisticsEserviceResponse;

public interface StatisticService {
  StatisticsEserviceResponse findStatistics(Long eserviceRecordId, Integer pollingFrequency)
      throws IOException, ParseException;
}
