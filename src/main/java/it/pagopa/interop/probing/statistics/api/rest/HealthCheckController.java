package it.pagopa.interop.probing.statistics.api.rest;

import it.pagopa.interop.probing.statistics.api.StatusApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController implements StatusApi {

  @Override
  public ResponseEntity<Void> getHealthStatus() {
    return ResponseEntity.noContent().build();
  }
}
