package it.pagopa.interop.probing.statistics.api.service;

import java.text.ParseException;
import java.util.List;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import it.pagopa.interop.probing.statistics.dtos.StatisticContent;

public interface TimestreamService {
  List<StatisticContent> findStatistics(Long eserviceRecordId, Integer pollingFrequency)
      throws JsonMappingException, JsonProcessingException, ParseException;
}
