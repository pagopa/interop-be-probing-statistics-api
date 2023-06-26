package it.pagopa.interop.probing.statistics.api.service.impl;


import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.common.collect.Lists;
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

  @Value("${graph.performance.precision}")
  private Integer performancePrecision;

  @Value("${graph.performance.tolerance}")
  private Integer performanceTolerance;

  @Value("${graph.failure.precision}")
  private Integer failurePrecision;

  @Value("${graph.failure.tolerance}")
  private Integer failureTolerance;

  @Override
  public StatisticsEserviceResponse findStatistics(Long eserviceRecordId, Integer pollingFrequency,
      OffsetDateTime startDate, OffsetDateTime endDate) throws IOException {
    List<StatisticContent> content =
        timestreamService.findStatistics(eserviceRecordId, pollingFrequency, startDate, endDate);
    return StatisticsEserviceResponse.builder()
        .performances(calculatePerformances(content, pollingFrequency))
        .failures(calculateFailures(content, pollingFrequency))
        .percentages(calculatePercentages(content)).build();
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

  private List<StatisticContent> calculatePerformances(List<StatisticContent> values,
      Integer pollingFrequency) {
    List<StatisticContent> performances = new ArrayList<>();
    if (values.size() > 0) {
      // Divide all the telemetries into a number of partitions given by the performancePrecision
      List<List<StatisticContent>> performancePartitions = Lists.partition(values,
          values.size() >= performancePrecision ? values.size() / performancePrecision
              : values.size());
      // Add the curve starting point
      performances.add(values.get(0));
      for (List<StatisticContent> partition : performancePartitions) {
        // For every partition we calculate the average of the responseTime which represents the
        // performance
        Double average = partition.stream().filter(el -> !Objects.isNull(el.getResponseTime()))
            .mapToDouble(val -> val.getResponseTime()).average().orElse(0.0);
        Integer numberOfFailures =
            partition.stream().filter(el -> !el.getStatus().equals(EserviceStatus.OK))
                .collect(Collectors.toList()).size();
        // If the partition contains a bigger fraction of KO and N/D which is bigger of the one
        // calculated via the performanceTolerance , we break the line
        if ((partition.size() < performanceTolerance && numberOfFailures > 0)
            || (partition.size() >= performanceTolerance
                && numberOfFailures >= partition.size() / performanceTolerance)) {
          average = 0.0;
        }
        StatisticContent performance = StatisticContent.builder().responseTime(average.longValue())
            .time(partition.get((partition.size() - 1)).getTime()).build();
        performances.add(performance);
      }
    }
    return performances;
  }

  private List<StatisticContent> calculateFailures(List<StatisticContent> values,
      Integer pollingFrequency) {
    List<StatisticContent> failures = new ArrayList<>();
    if (values.size() > 0) {
      // Divide all the telemetries into a number of partitions given by the failurePrecision
      List<List<StatisticContent>> failurePartitions = Lists.partition(values,
          values.size() >= failurePrecision ? values.size() / failurePrecision : values.size());
      for (List<StatisticContent> partition : failurePartitions) {
        List<EserviceStatus> failStatus = List.of(EserviceStatus.N_D, EserviceStatus.KO);
        for (EserviceStatus status : failStatus) {
          Integer numberOfFailures = partition.stream().filter(el -> el.getStatus().equals(status))
              .collect(Collectors.toList()).size();
          // If the partition contains a bigger fraction of KO or N/D which is bigger of the one
          // calculated via the failureTolerance , we create a failure point for the given status
          if ((partition.size() < failureTolerance && numberOfFailures > 0)
              || (partition.size() >= failureTolerance
                  && numberOfFailures >= partition.size() / failureTolerance)) {
            StatisticContent failure = StatisticContent.builder().status(status)
                .time(partition.get((partition.size() / 2)).getTime()).build();
            failures.add(failure);
          }
        }
      }
    }
    return failures;
  }


}
