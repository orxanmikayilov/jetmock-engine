package jetmock.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ThreadUtil {

  public static void sleep(Integer latency) {
    try {
      Thread.sleep(latency);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
