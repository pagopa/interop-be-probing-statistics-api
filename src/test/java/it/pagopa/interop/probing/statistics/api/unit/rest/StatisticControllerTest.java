package it.pagopa.interop.probing.statistics.api.unit.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import it.pagopa.interop.probing.statistics.api.service.StatisticService;
import it.pagopa.interop.probing.statistics.dtos.EserviceStatus;
import it.pagopa.interop.probing.statistics.dtos.StatisticContent;
import it.pagopa.interop.probing.statistics.dtos.StatisticsEserviceResponse;

@SpringBootTest
@AutoConfigureMockMvc
public class StatisticControllerTest {

  @Value("${api.telemetryData.url}")
  private String telemetryDataUrl;

  @Value("${api.telemetryData.filtered.url}")
  private String telemetryDataFilteredUrl;

  @MockBean
  private StatisticService service;

  StatisticsEserviceResponse response;

  @Autowired
  private MockMvc mockMvc;

  private final Long eservicesRecordId = 1L;

  private final Integer pollingFrequency = 5;

  private final OffsetDateTime startDate =
      OffsetDateTime.of(2023, 3, 1, 1, 0, 0, 0, ZoneOffset.UTC);

  private final OffsetDateTime endDate = OffsetDateTime.of(2023, 3, 2, 1, 0, 0, 0, ZoneOffset.UTC);

  @BeforeEach
  void setup() {
    response = StatisticsEserviceResponse.builder()
        .values(List.of(
            StatisticContent.builder().status(EserviceStatus.KO)
                .time(OffsetDateTime.of(2023, 5, 29, 0, 0, 0, 0, ZoneOffset.UTC)).build(),
            StatisticContent.builder().status(EserviceStatus.OK)
                .time(OffsetDateTime.of(2023, 5, 29, 1, 0, 0, 0, ZoneOffset.UTC)).responseTime(200L)
                .build()))
        .build();
  }

  @Test
  @DisplayName("e-service telemetry data are retrieved successfully")
  void testgetEserviceTelemetryData_whenEserviceExist_thenTelemetryDataAreReturned()
      throws Exception {
    Mockito.doReturn(response).when(service).findStatistics(eservicesRecordId, 5, null, null);
    MockHttpServletResponse response =
        mockMvc
            .perform(get(String.format(telemetryDataUrl, eservicesRecordId))
                .params(getMockRequestParamsTelemetryData(pollingFrequency)))
            .andReturn().getResponse();

    assertEquals(response.getStatus(), HttpStatus.OK.value());
    assertTrue(response.getContentAsString().contains("values"));
  }

  @Test
  @DisplayName("e-service filtered telemetry data are retrieved successfully")
  void testgetEserviceFilteredTelemetryData_whenEserviceExist_thenFilteredTelemetryDataAreReturned()
      throws Exception {
    Mockito.doReturn(response).when(service).findStatistics(eservicesRecordId, 5, startDate,
        endDate);
    MockHttpServletResponse response = mockMvc
        .perform(get(String.format(telemetryDataFilteredUrl, eservicesRecordId))
            .params(getMockRequestParamsFilterTelemetryData(pollingFrequency, startDate, endDate)))
        .andReturn().getResponse();

    assertEquals(response.getStatus(), HttpStatus.OK.value());
    assertTrue(response.getContentAsString().contains("values"));
  }

  private LinkedMultiValueMap<String, String> getMockRequestParamsTelemetryData(
      Integer pollingFrequency) {
    LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
    requestParams.add("pollingFrequency", pollingFrequency.toString());
    return requestParams;
  }

  private LinkedMultiValueMap<String, String> getMockRequestParamsFilterTelemetryData(
      Integer pollingFrequency, OffsetDateTime startDate, OffsetDateTime endDate) {
    LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
    requestParams.add("pollingFrequency", pollingFrequency.toString());
    requestParams.add("startDate", startDate.toString());
    requestParams.add("endDate", endDate.toString());
    return requestParams;
  }

}
