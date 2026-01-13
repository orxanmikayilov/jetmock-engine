package jetmock.constant;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Constant {

  public static final Long CACHE_TTL = 1L;
  public static final String MOCK_CACHE_NAME = "mock_server";

  public static final String RANDOM_UUID = "randomUUID";
  public static final String REQUEST_PREFIX = "req";
  public static final String RESPONSE_PREFIX = "resp";
  public static final String PATH_PREFIX = "path";
  public static final String CALLBACK_PREFIX = "callback";
  public static final String EVENT_PREFIX = "event";
  public static final String CACHE_PREFIX = "cache";
  public static final String VARIABLE_SYMBOL = "$";
  public static final String DELIMITER = "/";

  /**
   * The regular expression used to identify and prevent code injection attack
   * 1. Semicolons (;): Used to separate SQL statements. A string containing a semicolon might
   * attempt to inject additional SQL commands.
   * Example: "DROP TABLE users; --"
   * 2. Backslashes (\): Used as escape characters, which can be involved in escaping quotes in
   * strings to break out of intended string literals or commands.
   * Example: "C:\\Program Files"
   */
  public static final String CONDITION_BLACKLIST_REGEX =
      "(.*[;]+.*|.*[\\\\]+.*)";

}
