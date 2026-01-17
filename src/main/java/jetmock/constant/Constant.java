package jetmock.constant;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Constant {

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
