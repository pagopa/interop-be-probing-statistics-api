package it.pagopa.interop.probing.statistics.api.service.impl;


import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.amazonaws.xray.AWSXRay;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import it.pagopa.interop.probing.statistics.api.service.TimestreamService;
import it.pagopa.interop.probing.statistics.api.util.logging.DateUtilities;
import it.pagopa.interop.probing.statistics.api.util.logging.Logger;
import it.pagopa.interop.probing.statistics.api.util.logging.LoggingPlaceholders;
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

  @Value("${graph.max.months}")
  private Long maxMonths;

  private static final String TIME_MEASURE = "time";

  private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm";

  @Autowired
  private Logger logger;

  @Override
  public List<StatisticContent> findStatistics(Long eserviceRecordId, Integer pollingFrequency,
      OffsetDateTime startDate, OffsetDateTime endDate) throws IOException {
    // if we have a very wide range of time, we need to increase the granularity of the
    // pollingFrequency in order to make the interpolation possible, because the sequence cant
    // exceed 10000 entries.
    Long months = 1L;
    if (Objects.nonNull(startDate) && Objects.nonNull(endDate)) {
      months += (ChronoUnit.DAYS.between(startDate, endDate) / 30);
      if (months > maxMonths) {
        months = maxMonths;
      }
    }

 // @formatter:off
    String queryString = "WITH binned_timeseries AS ("
        //approximates the times using the polling frequency
        + "SELECT binned_timestamp, status, avg_response_time " 
        + "FROM "
        //this select with the where condition on seqnum deletes all the duplicates time that will be created with the approximation keeping the lowest time
        + "(SELECT * ,  row_number() over (partition by binned_timestamp order by binned_timestamp desc,num_status desc) as seqnum FROM "
        + "(SELECT eservice_record_id ,status,bin(time,"+ (pollingFrequency * months) +"m) as binned_timestamp, cast(avg(response_time) as int) as avg_response_time,count(status) as num_status "
        + "FROM  " + database + "." + table + " "
        + "WHERE time between " 
        + (Objects.nonNull(startDate) ? "'" + startDate.format(DateTimeFormatter.ofPattern(TIME_FORMAT)) + "' " : "ago(1d) ") 
        +" and "+ (Objects.nonNull(endDate) ? "'" + endDate.format(DateTimeFormatter.ofPattern(TIME_FORMAT)) + "' " : "now() ") 
        + "and eservice_record_id = '"+ eserviceRecordId + "' "
        + "group by eservice_record_id ,bin(time,"+ (pollingFrequency * months) +"m),status) "
        + ") "
        + "WHERE seqnum = 1 "
        + "ORDER BY  binned_timestamp"
        + "), interpolated_timeseries AS ( "
        //create a timestream timeseries in which all the times included in the sequence(2) but missing in the timeseries(1) are filled with N/D value 
        + "SELECT INTERPOLATE_FILL( "
        //(1)creates a timestream timeseries with the approximated times and the status
        + "CREATE_TIME_SERIES(binned_timestamp, status), "
        //(2)creates a sequence of times between the mix and max approximated times , dividing the data by the polling frequency
        + "SEQUENCE(min(binned_timestamp), max(binned_timestamp), "+ (pollingFrequency * months) +"m),'N/D') AS interpolated_status "
        + "FROM binned_timeseries)"
        //selects the data created by the previous operations
        + "SELECT time,value as status,avg_response_time as response_time "
        + "FROM interpolated_timeseries " 
        + "CROSS JOIN UNNEST(interpolated_status) "
        + "LEFT JOIN binned_timeseries s on s.binned_timestamp = time " 
        + "ORDER BY time";
 // @formatter:on
    QueryRequest queryRequest = QueryRequest.builder().queryString(queryString).build();
    AWSXRay.beginSubsegment(LoggingPlaceholders.TIMESTREAM_SUBSEGMENT_NAME);
    final QueryIterable queryResponseIterator = queryClient.queryPaginator(queryRequest);
    List<StatisticContent> content = new ArrayList<>();
    for (QueryResponse queryResponse : queryResponseIterator) {
      content.addAll(parseQueryResult(queryResponse));
    }
    AWSXRay.endSubsegment();
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
