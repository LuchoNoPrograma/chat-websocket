package luis.fluoxetina.chatwebsocket.util.converter;


import org.bson.json.StrictJsonWriter;
import org.springframework.core.convert.converter.Converter;

import java.time.ZonedDateTime;
import java.util.Date;

public class ZonedDateTimeWriteConverter implements Converter<ZonedDateTime, Date> {
  @Override
  public Date convert(ZonedDateTime source) {
    return Date.from(source.toInstant());
  }
}
