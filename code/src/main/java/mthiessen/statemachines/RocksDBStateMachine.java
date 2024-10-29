package mthiessen.statemachines;

import mthiessen.IStateMachine;
import org.apache.commons.io.FileUtils;
import org.rocksdb.InfoLogLevel;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RocksDBStateMachine implements IStateMachine {

  private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBStateMachine.class);
  private final RocksDB rocksDb;

  public RocksDBStateMachine(final String dir) {
    try {
      this.rocksDb = initRocksDB(dir);
    } catch (IOException | RocksDBException e) {
      throw new RuntimeException(e);
    }
  }

  private RocksDB initRocksDB(final String dir) throws IOException, RocksDBException {
    Path rocksDbDir = Paths.get(dir);

    FileUtils.deleteDirectory(new File(dir));
    Files.createDirectories(rocksDbDir);

    final int rocksThreads = 2;

    final Options options =
        new Options()
            .optimizeLevelStyleCompaction()
            .setCreateIfMissing(true)
            .setCreateMissingColumnFamilies(true)
            .setIncreaseParallelism(rocksThreads)
            .setInfoLogLevel(InfoLogLevel.INFO_LEVEL);
    return RocksDB.open(options, rocksDbDir.toAbsolutePath().toString());
  }

  @Override
  public Object rmw(final Object rmwReq) {
    if (!(rmwReq instanceof WriteRequest writeRequest)) {
      LOGGER.error("Request was not of type {}", WriteRequest.class);
      return Status.BAD_REQUEST;
    }

    String table = writeRequest.table();
    String key = writeRequest.key();
    byte[] value = writeRequest.value();

    String keyToInsert = table + key;

    try {
      this.rocksDb.put(keyToInsert.getBytes(UTF_8), value);
      LOGGER.info("Put done");
      return Status.OK;
    } catch (final RocksDBException e) {
      e.printStackTrace();
      return Status.ERROR;
    }
  }

  @Override
  public Object read(final Object readReq) {
    if (!(readReq instanceof ReadRequest readRequest)) {
      LOGGER.error("Request was not of type {}", ReadRequest.class);
      return Status.BAD_REQUEST;
    }

    String table = readRequest.table();
    String key = readRequest.key();

    String keyToInsert = table + key;

    try {
      return this.rocksDb.get(keyToInsert.getBytes(UTF_8));
    } catch (final RocksDBException e) {
      e.printStackTrace();
      return Status.ERROR;
    }
  }

  public enum Status {
    BAD_REQUEST,
    ERROR,
    OK
  }

  public record WriteRequest(String table, String key, byte[] value) implements Serializable {}

  public record ReadRequest(String table, String key) implements Serializable {}
}
