package it.pagopa.interop.probing.statistics.api.service.impl;


import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
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
      throws IOException, ParseException {
    logger.logRequest(eserviceRecordId, pollingFrequency);
    List<StatisticContent> content =
        timestreamService.findStatistics(eserviceRecordId, pollingFrequency);
    content = cleanStatistics(content);
    List<PercentageContent> percenteges = calculatePercentages(content);
    return StatisticsEserviceResponse.builder().values(content).percentages(percenteges).build();
  }

  private List<StatisticContent> cleanStatistics(List<StatisticContent> content) {
    List<StatisticContent> falseND = new ArrayList<>();
    for (StatisticContent statistic : content) {
      if (statistic.getStatus().equals(EserviceStatus.N_D)) {
        if (!getNext(statistic, content).getStatus().equals(EserviceStatus.N_D)
            && !getPrevious(statistic, content).getStatus().equals(EserviceStatus.N_D)) {
          falseND.add(statistic);
        }
      }
    }
    content.removeAll(falseND);
    return content;
  }

  public StatisticContent getNext(StatisticContent statistic, List<StatisticContent> content) {
    int idx = content.indexOf(statistic);
    if (idx < 0 || idx + 1 == content.size())
      return null;
    return content.get(idx + 1);
  }

  public StatisticContent getPrevious(StatisticContent statistic, List<StatisticContent> content) {
    int idx = content.indexOf(statistic);
    if (idx <= 0)
      return null;
    return content.get(idx - 1);
  }

  private List<PercentageContent> calculatePercentages(List<StatisticContent> values) {
    List<PercentageContent> percentages = new ArrayList<>();
    for (EserviceStatus status : EserviceStatus.values()) {
      Long filteredValues =
          values.stream().filter(value -> value.getStatus().equals(status)).count();
      percentages.add(new PercentageContent(
          values.size() > 0 ? filteredValues * (100 / Float.valueOf(values.size())) : 0,
          status.getValue()));
    }
    return percentages;
  }


}
