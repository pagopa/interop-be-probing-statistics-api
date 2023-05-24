package it.pagopa.interop.probing.statistics.api.service.impl;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import it.pagopa.interop.probing.statistics.api.service.StatisticService;
import it.pagopa.interop.probing.statistics.api.service.TimestreamService;
import it.pagopa.interop.probing.statistics.api.util.logging.Logger;
import it.pagopa.interop.probing.statistics.dtos.EserviceStatus;
import it.pagopa.interop.probing.statistics.dtos.PercentageContent;
import it.pagopa.interop.probing.statistics.dtos.StatisticContent;
import it.pagopa.interop.probing.statistics.dtos.StatisticsEserviceResponse;

@Service
public class StatisticServiceImpl implements StatisticService {

  @Autowired
  private TimestreamService timestreamService;

  @Autowired
  private Logger logger;

  @Override
  public StatisticsEserviceResponse findStatistics(Long eserviceRecordId, Integer pollingFrequency)
      throws IOException {
    logger.logRequest(eserviceRecordId, pollingFrequency);
    List<StatisticContent> content =
        timestreamService.findStatistics(eserviceRecordId, pollingFrequency);
    content = cleanStatistics(content);
    List<PercentageContent> percenteges = calculatePercentages(content);
    return StatisticsEserviceResponse.builder().values(content).percentages(percenteges).build();
  }

  /**
   * the functions deletes all the N/D data that are isolated from other N/D's, because of some
   * false N/D given by the result interpolation of the timestream query
   *
   * @param statistics returned by the timestream query
   * @return cleaned statistics
   */
  private List<StatisticContent> cleanStatistics(List<StatisticContent> content) {
    List<StatisticContent> falseND = new ArrayList<>();
    // we ignore the first and last element because we cannot tell if they are isolated N/D
    for (int i = 1; i < content.size() - 1; i++) {
      if (content.get(i).getStatus().equals(EserviceStatus.N_D)) {
        // checks if it is an isolated N/D
        if (!content.get(i + 1).getStatus().equals(EserviceStatus.N_D)
            && !content.get(i - 1).getStatus().equals(EserviceStatus.N_D)) {
          falseND.add(content.get(i));
        }
      }
    }
    content.removeAll(falseND);
    return content;
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
