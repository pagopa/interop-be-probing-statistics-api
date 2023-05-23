package it.pagopa.interop.probing.statistics.api.service;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import it.pagopa.interop.probing.statistics.dtos.StatisticContent;

public interface TimestreamService {
  List<StatisticContent> findStatistics(Long eserviceRecordId, Integer pollingFrequency)
      throws IOException, ParseException;
}
