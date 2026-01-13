package jetmock.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import jetmock.dto.payload.CallbackApiPayload;
import jetmock.util.ParserUtil;
import jetmock.util.ThreadUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CallbackService {

  public void callbackToClient(CallbackApiPayload callbackApiPayload) {
    log.info("callback to client started: {}", callbackApiPayload);
    ThreadUtil.sleep(callbackApiPayload.getLatency());
    JsonNode callbackRequestBody = ParserUtil.toJsonNode(callbackApiPayload.getBody());
    callbackToClient(callbackRequestBody, callbackApiPayload.getPath(),
        callbackApiPayload.getMethod());
  }

  private static void callbackToClient(JsonNode callbackRequestBody, String url, String method) {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<JsonNode> requestEntity = new HttpEntity<>(callbackRequestBody, headers);
    try {
      restTemplate.exchange(url, HttpMethod.valueOf(method), requestEntity, JsonNode.class);
      log.info("callback to client finished");
    } catch (Exception e) {
      log.error("callback failed: {},{},{}", e.getMessage(), url, callbackRequestBody);
    }
  }

}
