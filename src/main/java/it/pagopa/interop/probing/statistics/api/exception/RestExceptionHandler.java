package it.pagopa.interop.probing.statistics.api.exception;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
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
   * Manages the {@link ParseException} creating a new {@link ResponseEntity} and sending it to the
   * client with error code 500 and information about the error
   *
   * @param ex The intercepted exception
   * @return A new {@link ResponseEntity} with {@link Problem} body
   */
  @ExceptionHandler(ParseException.class)
  protected ResponseEntity<Object> handleIOException(ParseException ex) {
    log.logMessageException(ex);
    Problem problemResponse = createProblem(HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorMessages.SERVER_ERROR, ErrorMessages.SERVER_ERROR);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemResponse);
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
        .traceId(MDC.get(LoggingPlaceholders.TRACE_ID_PLACEHOLDER)).errors(List.of(errorDetails))
        .build();
  }
}
