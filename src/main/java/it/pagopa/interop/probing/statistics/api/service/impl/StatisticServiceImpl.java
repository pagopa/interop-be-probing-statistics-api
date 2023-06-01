package it.pagopa.interop.probing.statistics.api.service.impl;


import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import it.pagopa.interop.probing.statistics.api.service.StatisticService;
import it.pagopa.interop.probing.statistics.api.service.TimestreamService;
import it.pagopa.interop.probing.statistics.dtos.EserviceStatus;
import it.pagopa.interop.probing.statistics.dtos.PercentageContent;
import it.pagopa.interop.probing.statistics.dtos.StatisticContent;
import it.pagopa.interop.probing.statistics.dtos.StatisticsEserviceResponse;

@Service
public class StatisticServiceImpl implements StatisticService {

  @Autowired
  private TimestreamService timestreamService;



  @Override
  public StatisticsEserviceResponse findStatistics(Long eserviceRecordId, Integer pollingFrequency,
      OffsetDateTime startDate, OffsetDateTime endDate) throws IOException {
    List<StatisticContent> content =
        timestreamService.findStatistics(eserviceRecordId, pollingFrequency, startDate, endDate);
    List<PercentageContent> percenteges = calculatePercentages(content);
    return StatisticsEserviceResponse.builder().values(content).percentages(percenteges).build();
  }

  private List<PercentageContent> calculatePercentages(List<StatisticContent> values) {
    List<PercentageContent> percentages = new ArrayList<>();
    Map<EserviceStatus, Float> fractions = values.stream()
        .collect(Collectors.groupingBy(StatisticContent::getStatus, Collectors.collectingAndThen(
            Collectors.counting(), count -> (count * 100 / Float.valueOf(values.size())))));
    for (EserviceStatus status : EserviceStatus.values()) {
      percentages.add(new PercentageContent(
          Objects.nonNull(fractions.get(status)) ? fractions.get(status) : 0, status.getValue()));
    }
    return percentages;
  }



}
