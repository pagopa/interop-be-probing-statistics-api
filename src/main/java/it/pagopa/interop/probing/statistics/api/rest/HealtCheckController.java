package it.pagopa.interop.probing.statistics.api.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import it.pagopa.interop.probing.statistics.api.StatusApi;

@RestController
public class HealtCheckController implements StatusApi {

  @Override
  public ResponseEntity<Void> getHealthStatus() {
    return ResponseEntity.noContent().build();
  }

}
