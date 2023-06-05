package it.pagopa.interop.probing.statistics.api.rest;

import java.io.IOException;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import it.pagopa.interop.probing.statistics.api.TelemetryDataApi;
import it.pagopa.interop.probing.statistics.api.service.StatisticService;
import it.pagopa.interop.probing.statistics.api.util.logging.Logger;
import it.pagopa.interop.probing.statistics.dtos.StatisticsEserviceResponse;

@RestController
public class StatisticController implements TelemetryDataApi {

  @Autowired
  private StatisticService statisticService;

  @Autowired
  private Logger logger;

  @Override
  public ResponseEntity<StatisticsEserviceResponse> statisticsEservices(Long eserviceRecordId,
      Integer pollingFrequency) throws IOException {
    logger.logRequest(eserviceRecordId, pollingFrequency);
    return ResponseEntity
        .ok(statisticService.findStatistics(eserviceRecordId, pollingFrequency, null, null));
  }

  @Override
  public ResponseEntity<StatisticsEserviceResponse> filteredStatisticsEservices(
      Long eserviceRecordId, Integer pollingFrequency, OffsetDateTime startDate,
      OffsetDateTime endDate) throws IOException {
    logger.logFilterRequest(eserviceRecordId, pollingFrequency, startDate, endDate);
    return ResponseEntity.ok(
        statisticService.findStatistics(eserviceRecordId, pollingFrequency, startDate, endDate));
  }
}

