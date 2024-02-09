package luis.fluoxetina.chatwebsocket.util.converter;


import org.springframework.core.convert.converter.Converter;

import java.time.ZonedDateTime;
import java.util.Date;

public class ZonedDateTimeReadConverter implements Converter<Date, ZonedDateTime> {
  @Override
  public ZonedDateTime convert(Date source) {
    return source.toInstant().atZone(ZonedDateTime.now().getZone());
  }
}
