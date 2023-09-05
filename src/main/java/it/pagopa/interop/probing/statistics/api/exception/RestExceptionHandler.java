package it.pagopa.interop.probing.statistics.api.exception;

import java.io.IOException;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import com.amazonaws.xray.AWSXRay;
import it.pagopa.interop.probing.statistics.api.util.constants.ErrorMessages;
import it.pagopa.interop.probing.statistics.api.util.logging.Logger;
import it.pagopa.interop.probing.statistics.api.util.logging.LoggingPlaceholders;
import it.pagopa.interop.probing.statistics.dtos.Problem;
import it.pagopa.interop.probing.statistics.dtos.ProblemError;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  @Autowired
  private Logger log;


  /**
   * Manages the {@link IOException} creating a new {@link ResponseEntity} and sending it to the
   * client with error code 500 and information about the error
   *
   * @param ex The intercepted exception
   * @return A new {@link ResponseEntity} with {@link Problem} body
   */
  @ExceptionHandler(IOException.class)
  protected ResponseEntity<Object> handleIOException(IOException ex) {
    log.logMessageException(ex);
    Problem problemResponse = createProblem(HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorMessages.SERVER_ERROR, ErrorMessages.SERVER_ERROR);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemResponse);
  }

  /**
   * Manages the {@link TypeMismatchException} creating a new {@link ResponseEntity} and sending it
   * to the client with error code 400 and information about the error
   *
   * @param ex The intercepted exception
   * @return A new {@link ResponseEntity} with {@link Problem} body
   */
  @Override
  protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
      HttpStatus status, WebRequest request) {
    handleException(ex);
    Problem problemResponse =
        createProblem(HttpStatus.BAD_REQUEST, ErrorMessages.BAD_REQUEST, ErrorMessages.BAD_REQUEST);
    return ResponseEntity.status(status).body(problemResponse);
  }

  /**
   * Creates an instance of type {@link Problem} following the RFC 7807 standard
   *
   * @param responseCode The response error code
   * @param titleMessage The response title message
   * @param detailMessage The response detail error message
   * @return A new instance of {@link Problem}
   */
  private Problem createProblem(HttpStatus responseCode, String titleMessage,
      String detailMessage) {
    ProblemError errorDetails =
        ProblemError.builder().code(responseCode.toString()).detail(detailMessage).build();
    return Problem.builder().status(responseCode.value()).title(titleMessage).detail(detailMessage)
        .traceId(AWSXRay.getCurrentSegment().getTraceId().toString()).errors(List.of(errorDetails))
        .build();
  }

  private void handleException(Exception ex) {
    MDC.put(LoggingPlaceholders.TRACE_ID_XRAY_PLACEHOLDER,
        LoggingPlaceholders.TRACE_ID_XRAY_MDC_PREFIX
            + AWSXRay.getCurrentSegment().getTraceId().toString() + "]");
    log.logMessageException(ex);
  }
}
