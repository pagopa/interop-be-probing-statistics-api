package it.pagopa.interop.probing.statistics.api.rest;

import java.text.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import it.pagopa.interop.probing.statistics.api.EservicesApi;
import it.pagopa.interop.probing.statistics.api.service.StatisticService;
import it.pagopa.interop.probing.statistics.dtos.StatisticsEserviceResponse;

@RestController
public class StatisticController implements EservicesApi {

  @Autowired
  private StatisticService statisticService;

  @Override
  public ResponseEntity<StatisticsEserviceResponse> statisticsEservices(Long eserviceRecordId,
      Integer pollingFrequency)
      throws JsonMappingException, JsonProcessingException, ParseException {
    return ResponseEntity.ok(statisticService.findStatistics(eserviceRecordId, pollingFrequency));
  }
}

