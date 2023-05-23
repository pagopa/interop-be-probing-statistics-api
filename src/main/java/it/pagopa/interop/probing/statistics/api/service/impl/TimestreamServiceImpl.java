package it.pagopa.interop.probing.statistics.api.service.impl;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import it.pagopa.interop.probing.statistics.api.service.TimestreamService;
import it.pagopa.interop.probing.statistics.api.util.logging.DateUtilities;
import it.pagopa.interop.probing.statistics.api.util.logging.Logger;
import it.pagopa.interop.probing.statistics.dtos.StatisticContent;
import software.amazon.awssdk.services.timestreamquery.TimestreamQueryClient;
import software.amazon.awssdk.services.timestreamquery.model.ColumnInfo;
import software.amazon.awssdk.services.timestreamquery.model.Datum;
import software.amazon.awssdk.services.timestreamquery.model.QueryRequest;
import software.amazon.awssdk.services.timestreamquery.model.QueryResponse;
import software.amazon.awssdk.services.timestreamquery.model.QueryStatus;
import software.amazon.awssdk.services.timestreamquery.model.Row;
import software.amazon.awssdk.services.timestreamquery.paginators.QueryIterable;

@Service
public class TimestreamServiceImpl implements TimestreamService {

  @Autowired
  private TimestreamQueryClient queryClient;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private DateUtilities dateUtilities;

  @Value("${amazon.timestream.database}")
  private String database;

  @Value("${amazon.timestream.table}")
  private String table;

  private static final String TIME_MEASURE = "time";

  @Autowired
  private Logger logger;

  @Override
  public List<StatisticContent> findStatistics(Long eserviceRecordId, Integer pollingFrequency)
      throws IOException {
 // @formatter:off
    String queryString = "WITH binned_timeseries AS ("
        + "SELECT  BIN(time, "+pollingFrequency+"m) AS binned_timestamp, status, response_time " 
        + "FROM " + database + "." + table + " " 
        + "WHERE time between ago(1d) and now() "
        + "and eservice_record_id = '"+ eserviceRecordId + "' " 
        + "GROUP BY  BIN(time, "+pollingFrequency+"m) , status , response_time "
        + "), interpolated_timeseries AS ( " 
        + "SELECT INTERPOLATE_FILL( "
        + "CREATE_TIME_SERIES(binned_timestamp, status), "
        + "SEQUENCE(min(binned_timestamp), max(binned_timestamp), "+pollingFrequency+"m),'N/D') AS interpolated_status "
        + "FROM binned_timeseries)" 
        + "SELECT time,value as status,response_time "
        + "FROM interpolated_timeseries " 
        + "CROSS JOIN UNNEST(interpolated_status) "
        + "LEFT JOIN binned_timeseries s on s.binned_timestamp = time " 
        + "ORDER BY time";
 // @formatter:on
    QueryRequest queryRequest = QueryRequest.builder().queryString(queryString).build();
    final QueryIterable queryResponseIterator = queryClient.queryPaginator(queryRequest);
    List<StatisticContent> content = new ArrayList<>();
    for (QueryResponse queryResponse : queryResponseIterator) {
      content.addAll(parseQueryResult(queryResponse));
    }
    return content;

  }

  private List<StatisticContent> parseQueryResult(QueryResponse response) throws IOException {
    final QueryStatus queryStatus = response.queryStatus();
    List<StatisticContent> statistics = new ArrayList<>();

    logger.logQueryProgress(queryStatus.progressPercentage());

    List<ColumnInfo> columnInfo = response.columnInfo();
    List<Row> rows = response.rows();
    // iterate every row
    for (Row row : rows) {
      statistics.add(mapper.readValue(parseRow(columnInfo, row), StatisticContent.class));
    }
    return statistics;
  }

  private String parseRow(List<ColumnInfo> columnInfo, Row row) {
    List<Datum> data = row.data();
    List<String> rowOutput = new ArrayList<>();
    // iterate every column per row
    for (int j = 0; j < data.size(); j++) {
      rowOutput.add(parseDatum(columnInfo.get(j), data.get(j)));
    }
    return String.format("{%s}",
        rowOutput.stream().map(Object::toString).collect(Collectors.joining(",")));
  }

  private String parseDatum(ColumnInfo info, Datum datum) {
    if (Objects.nonNull(datum.nullValue()) && datum.nullValue()) {
      return "\"" + parseColumnName(info) + "\":null";
    }
    return parseScalarType(info, datum);
  }

  private String parseScalarType(ColumnInfo info, Datum datum) {
    return "\"" + parseColumnName(info) + "\":\""
        + (parseColumnName(info).equals(TIME_MEASURE)
            ? dateUtilities.changeDateFormat(datum.scalarValue())
            : datum.scalarValue())
        + "\"";
  }

  private String parseColumnName(ColumnInfo info) {
    return Objects.isNull(info.name()) ? ""
        : CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, info.name());
  }



}
