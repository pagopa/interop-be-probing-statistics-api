package it.pagopa.interop.probing.statistics.api.config.database;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.timestreamquery.TimestreamQueryClient;

@Configuration
public class TimestreamConfig {

  @Bean
  public TimestreamQueryClient buildQueryClient() {
    return TimestreamQueryClient.builder().credentialsProvider(DefaultCredentialsProvider.create())
        .build();
  }
}
