package it.pagopa.interop.probing.statistics.api.util.logging;

public class LoggingPlaceholders {
  private LoggingPlaceholders() {}

  public static final String TRACE_ID_PLACEHOLDER = "trace_id";
  public static final String TRACE_ID_XRAY_PLACEHOLDER = "AWS-XRAY-TRACE-ID";
  public static final String TRACE_ID_XRAY_MDC_PREFIX = "- [TRACE_ID= ";

}
