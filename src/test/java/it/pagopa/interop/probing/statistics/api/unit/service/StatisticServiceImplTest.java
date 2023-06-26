package it.pagopa.interop.probing.statistics.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import it.pagopa.interop.probing.statistics.api.service.StatisticService;
import it.pagopa.interop.probing.statistics.api.service.TimestreamService;
import it.pagopa.interop.probing.statistics.api.service.impl.StatisticServiceImpl;
import it.pagopa.interop.probing.statistics.dtos.EserviceStatus;
import it.pagopa.interop.probing.statistics.dtos.StatisticContent;
import it.pagopa.interop.probing.statistics.dtos.StatisticsEserviceResponse;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class StatisticServiceImplTest {
  @InjectMocks
  @Autowired
  private StatisticService statisticService = new StatisticServiceImpl();

  @Mock
  private TimestreamService timestreamService;

  List<StatisticContent> response;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(statisticService, "performancePrecision", 24);
    ReflectionTestUtils.setField(statisticService, "performanceTolerance", 2);
    ReflectionTestUtils.setField(statisticService, "failurePrecision", 96);
    ReflectionTestUtils.setField(statisticService, "failureTolerance", 4);
    response = List.of(
        StatisticContent.builder().status(EserviceStatus.KO)
            .time(OffsetDateTime.of(2023, 5, 29, 0, 0, 0, 0, ZoneOffset.UTC)).build(),
        StatisticContent.builder().status(EserviceStatus.OK)
            .time(OffsetDateTime.of(2023, 5, 29, 1, 0, 0, 0, ZoneOffset.UTC)).responseTime(200L)
            .build());
  }

  @Test
  @DisplayName("The findStatistics method does not throw exception with empty result.")
  void testFindStatistics_thenDoesNotThrowException() throws IOException {
    List<StatisticContent> emptyResponse = List.of();
    Mockito.when(timestreamService.findStatistics(1L, 5, null, null)).thenReturn(emptyResponse);
    assertDoesNotThrow(() -> statisticService.findStatistics(1L, 5, null, null));
  }

  @Test
  @DisplayName("The findStatistics method successfully build a StatisticsEserviceResponse.")
  void testFindStatistics_thenSuccessfullyBuildResponse() throws IOException {
    Mockito.when(timestreamService.findStatistics(1L, 5, null, null)).thenReturn(response);
    StatisticsEserviceResponse response = statisticService.findStatistics(1L, 5, null, null);
    assertTrue(response.getPercentages().size() > 0);
  }

}
