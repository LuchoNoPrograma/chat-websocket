package luis.fluoxetina.chatwebsocket.config;

import luis.fluoxetina.chatwebsocket.util.converter.ZonedDateTimeReadConverter;
import luis.fluoxetina.chatwebsocket.util.converter.ZonedDateTimeWriteConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.Arrays;

@Configuration
public class MongoConfig {

  @Bean
  public MongoCustomConversions mongoCustomConversions() {
    return new MongoCustomConversions(Arrays.asList(new ZonedDateTimeReadConverter(), new ZonedDateTimeWriteConverter()));
  }
}
