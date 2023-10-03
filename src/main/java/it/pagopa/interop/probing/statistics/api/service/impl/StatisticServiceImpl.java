package it.pagopa.interop.probing.statistics.api.service.impl;


import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${graph.performance.tolerance}")
  private Integer performanceTolerance;

  @Value("${graph.failure.tolerance}")
  private Integer failureTolerance;

  @Override
  public StatisticsEserviceResponse findStatistics(Long eserviceRecordId, Integer pollingFrequency,
      OffsetDateTime startDate, OffsetDateTime endDate) throws IOException {
    List<StatisticContent> content =
        timestreamService.findStatistics(eserviceRecordId, pollingFrequency, startDate, endDate);
    return calculatePerformances(content, startDate, endDate);
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

  private StatisticsEserviceResponse calculatePerformances(List<StatisticContent> values,
      OffsetDateTime startDate, OffsetDateTime endDate) {
    List<StatisticContent> failures = new ArrayList<>();
    List<StatisticContent> performances = new ArrayList<>();
    if (values.size() > 0) {
      OffsetDateTime startDateZero = values.get(0).getTime();
      Long granularityPerWeeks = 1L;
      if (Objects.nonNull(startDate) && Objects.nonNull(endDate)) {
        // Group the telemetries in intervals of 6 hours for every week except for the first week ,
        // in which the telemetries are grouped for every hour
        granularityPerWeeks = ChronoUnit.WEEKS.between(startDate, endDate) * 6;
        if (granularityPerWeeks < 1) {
          granularityPerWeeks = 1L;
        }
      }
      OffsetDateTime now = OffsetDateTime.now();
      // Remove the minute and seconds from the date that will be used to group the telemetries
      startDateZero = startDateZero
          .truncatedTo(!granularityPerWeeks.equals(1L) ? ChronoUnit.DAYS : ChronoUnit.HOURS);
      while (startDateZero.isBefore(now)) {
        OffsetDateTime innerStartDatezero = startDateZero;
        Long innerGranularity = granularityPerWeeks;
        List<StatisticContent> hourStatistic = values.stream()
            .filter(el -> el.getTime().isBefore(innerStartDatezero.plusHours(innerGranularity))
                && (el.getTime().isAfter(innerStartDatezero)
                    || el.getTime().isEqual(innerStartDatezero)))
            .collect(Collectors.toList());
        Double average = hourStatistic.stream().filter(el -> !Objects.isNull(el.getResponseTime()))
            .mapToDouble(val -> val.getResponseTime()).average().orElse(0.0);
        Integer numberOfFailures =
            hourStatistic.stream().filter(el -> !el.getStatus().equals(EserviceStatus.OK))
                .collect(Collectors.toList()).size();
        // if the point contains a fraction of KO and N/D which is bigger of the one
        // calculated via the performanceTolerance , the line gets broken
        if ((hourStatistic.size() < performanceTolerance && numberOfFailures > 0)
            || (hourStatistic.size() >= performanceTolerance
                && numberOfFailures >= hourStatistic.size() / performanceTolerance)) {
          average = 0.0;
        }
        StatisticContent performance = StatisticContent.builder().responseTime(average.longValue())
            .time(innerStartDatezero).build();
        performances.add(performance);

        failures.addAll(calculateFailures(hourStatistic));
        startDateZero = startDateZero.plusHours(granularityPerWeeks);
      }
    }
    return StatisticsEserviceResponse.builder().performances(performances).failures(failures)
        .percentages(calculatePercentages(values)).build();
  }

  private List<StatisticContent> calculateFailures(List<StatisticContent> values) {
    List<StatisticContent> failures = new ArrayList<>();
    List<EserviceStatus> failStatus = List.of(EserviceStatus.N_D, EserviceStatus.KO);
    for (EserviceStatus status : failStatus) {
      Integer numberOfFailures = values.stream().filter(el -> el.getStatus().equals(status))
          .collect(Collectors.toList()).size();
      // If the partition contains a bigger fraction of KO or N/D which is bigger of the one
      // calculated via the failureTolerance , we create a failure point for the given status
      if ((values.size() < failureTolerance && numberOfFailures > 0)
          || (values.size() >= failureTolerance
              && numberOfFailures >= values.size() / failureTolerance)) {
        StatisticContent failure =
            StatisticContent.builder().status(status).time(values.get(0).getTime()).build();
        failures.add(failure);
      }

    }
    return failures;
  }


}
