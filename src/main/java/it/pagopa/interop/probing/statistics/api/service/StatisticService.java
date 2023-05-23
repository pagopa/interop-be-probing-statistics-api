package it.pagopa.interop.probing.statistics.api.service;

import java.text.ParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import it.pagopa.interop.probing.statistics.dtos.StatisticsEserviceResponse;

public interface StatisticService {
  StatisticsEserviceResponse findStatistics(Long eserviceRecordId, Integer pollingFrequency)
      throws JsonMappingException, JsonProcessingException, ParseException;
}
