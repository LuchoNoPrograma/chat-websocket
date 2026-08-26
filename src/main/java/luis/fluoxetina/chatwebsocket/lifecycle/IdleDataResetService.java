package luis.fluoxetina.chatwebsocket.lifecycle;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Log4j2
public class IdleDataResetService {
  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;
  private final TransactionTemplate transactionTemplate;
  private final long idleTimeoutNanos;
  private final AtomicLong lastActivityNanos = new AtomicLong(System.nanoTime());
  private final ReentrantLock resetLock = new ReentrantLock();

  public IdleDataResetService(JdbcTemplate jdbcTemplate,
                              DataSource dataSource,
                              TransactionTemplate transactionTemplate,
                              @Value("${app.data-reset.idle-timeout:PT30M}") Duration idleTimeout) {
    this.jdbcTemplate = jdbcTemplate;
    this.dataSource = dataSource;
    this.transactionTemplate = transactionTemplate;
    this.idleTimeoutNanos = idleTimeout.toNanos();
  }

  public void markActivity() {
    if (hasExceededIdleTimeout()) resetWhenIdle();
    lastActivityNanos.set(System.nanoTime());
  }

  @Scheduled(
    fixedDelayString = "${app.data-reset.check-interval-ms:60000}",
    initialDelayString = "${app.data-reset.initial-delay-ms:60000}"
  )
  public void resetWhenIdle() {
    if (!hasExceededIdleTimeout()) return;

    resetLock.lock();
    try {
      if (!hasExceededIdleTimeout()) return;
      transactionTemplate.executeWithoutResult(status -> restoreInitialData());
      lastActivityNanos.set(System.nanoTime());
      log.info("In-memory chat data restored after the inactivity timeout");
    } finally {
      resetLock.unlock();
    }
  }

  private void restoreInitialData() {
    jdbcTemplate.update("DELETE FROM chat_room_tags");
    jdbcTemplate.update("DELETE FROM chat_messages");
    jdbcTemplate.update("DELETE FROM chat_rooms");
    jdbcTemplate.update("DELETE FROM room_tags");
    jdbcTemplate.update("DELETE FROM chat_users");

    ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("data.sql"));
    populator.populate(DataSourceUtils.getConnection(dataSource));
  }

  private boolean hasExceededIdleTimeout() {
    return System.nanoTime() - lastActivityNanos.get() >= idleTimeoutNanos;
  }
}
