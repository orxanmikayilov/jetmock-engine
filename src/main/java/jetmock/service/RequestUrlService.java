package jetmock.service;

import static jetmock.contant.Constant.DELIMITER;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestUrlService {

  public boolean urlMatches(String mockUrl, String[] requestUrlParts) {
    String[] mockUrlParts = mockUrl.split(DELIMITER);

    if (mockUrlParts.length != requestUrlParts.length) {
      return false;
    }

    for (int i = 0; i < mockUrlParts.length; i++) {
      if (!mockUrlParts[i].startsWith(":") && !mockUrlParts[i].equals(requestUrlParts[i])) {
        return false;
      }
    }
    return true;
  }

  public Map<String, String> extractPathVariables(String mockUrl,
                                                  String requestUrl) {
    String[] requestUrlParts = requestUrl.split(DELIMITER);
    String[] mockUrlParts = mockUrl.split(DELIMITER);

    Map<String, String> pathVariables = new HashMap<>();
    for (int i = 0; i < mockUrlParts.length; i++) {
      String mockUrlPart = mockUrlParts[i];
      String requestUrlPart = requestUrlParts[i];
      if (mockUrlPart.startsWith(":")) {
        pathVariables.put(mockUrlPart.substring(1), requestUrlPart);
      }
    }
    return pathVariables;
  }

  public String getRequestPath(String groupName, String requestUri) {
    return requestUri.substring((DELIMITER + groupName).length());
  }

}
