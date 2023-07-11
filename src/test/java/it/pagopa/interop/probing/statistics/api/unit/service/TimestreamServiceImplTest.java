package it.pagopa.interop.probing.statistics.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
import com.amazonaws.xray.AWSXRay;
import it.pagopa.interop.probing.statistics.api.service.TimestreamService;
import it.pagopa.interop.probing.statistics.api.service.impl.TimestreamServiceImpl;
import software.amazon.awssdk.services.timestreamquery.TimestreamQueryClient;
import software.amazon.awssdk.services.timestreamquery.model.ColumnInfo;
import software.amazon.awssdk.services.timestreamquery.model.Datum;
import software.amazon.awssdk.services.timestreamquery.model.QueryRequest;
import software.amazon.awssdk.services.timestreamquery.model.QueryResponse;
import software.amazon.awssdk.services.timestreamquery.model.QueryStatus;
import software.amazon.awssdk.services.timestreamquery.model.Row;
import software.amazon.awssdk.services.timestreamquery.paginators.QueryIterable;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class TimestreamServiceImplTest {
  @InjectMocks
  @Autowired
  private TimestreamService timestreamService = new TimestreamServiceImpl();

  @Mock
  private TimestreamQueryClient queryClient;

  @Mock
  QueryIterable iterator;

  @Mock
  Iterator<QueryResponse> mockIter;

  QueryRequest queryRequest;

  QueryResponse response;

  private static final String ESERVICE_RECORD_ID_DIMENSION = "eservice_record_id";

  private static final String KO_REASON_DIMENSION = "ko_reason";

  private static final String RESPONSE_TIME_MEASURE = "response_time";

  private static final String STATUS_MEASURE = "status";

  private static final String TIME_MEASURE_NAME = "time";

  @BeforeEach
  void setup() {
    AWSXRay.beginSegment("test");
    ReflectionTestUtils.setField(timestreamService, "database", "test_database");
    ReflectionTestUtils.setField(timestreamService, "table", "test_table");
    ReflectionTestUtils.setField(timestreamService, "maxMonths", 3L);
    response =
        QueryResponse.builder()
            .queryStatus(QueryStatus.builder().progressPercentage(100.00).build())
            .columnInfo(List.of(ColumnInfo.builder().name(ESERVICE_RECORD_ID_DIMENSION).build(),
                ColumnInfo.builder().name(KO_REASON_DIMENSION).build(),
                ColumnInfo.builder().name(RESPONSE_TIME_MEASURE).build(),
                ColumnInfo.builder().name(STATUS_MEASURE).build(),
                ColumnInfo.builder().name(TIME_MEASURE_NAME).build()))
            .rows(List.of(Row.builder()
                .data(List.of(Datum.builder().scalarValue("1").build(),
                    Datum.builder().scalarValue(null).build(),
                    Datum.builder().scalarValue("50").build(),
                    Datum.builder().scalarValue("OK").build(),
                    Datum.builder().scalarValue("2022-07-07 17:42:32.939000000").build()))
                .build()))
            .build();
  }

  @AfterEach
  void clean() {
    AWSXRay.endSegment();
  }

  @Test
  @DisplayName("The findStatistics method successfully build a List of StatisticContent.")
  void testFindStatistics_thenSuccessfullyBuildStatisticContent() throws IOException {
    Mockito.when(queryClient.queryPaginator(Mockito.any(QueryRequest.class))).thenReturn(iterator);
    Mockito.when(iterator.iterator()).thenReturn(mockIter);
    Mockito.when(mockIter.hasNext()).thenReturn(true, false);
    Mockito.when(mockIter.next()).thenReturn(response);
    assertDoesNotThrow(() -> timestreamService.findStatistics(1L, 5, null, null));
  }

}
