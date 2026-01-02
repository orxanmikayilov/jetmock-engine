package jetmock.config;

import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RocksDbConfig {

  static {
    RocksDB.loadLibrary();
  }

  private RocksDB rocksDB;

  @Bean
  public RocksDB rocksDB() {
    try {
      Path dbPath = Path.of("./data/mock-rocksdb");
      Files.createDirectories(dbPath);

      Options options = new Options()
          .setCreateIfMissing(true);

      this.rocksDB = RocksDB.open(options, dbPath.toString());
      log.info("RocksDB started at {}", dbPath.toAbsolutePath());
      return rocksDB;

    } catch (Exception e) {
      throw new RuntimeException("Failed to start RocksDB", e);
    }
  }

  @PreDestroy
  public void close() {
    if (rocksDB != null) {
      rocksDB.close();
      log.info("RocksDB closed");
    }
  }

}
