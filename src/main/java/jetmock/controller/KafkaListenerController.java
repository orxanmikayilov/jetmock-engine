package jetmock.controller;

import java.util.List;
import jetmock.dto.ActiveKafkaListenerDto;
import jetmock.service.kafka.KafkaListenerLifecycleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/kafka/listeners")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KafkaListenerController {

  KafkaListenerLifecycleService kafkaListenerLifecycleService;

  @GetMapping
  public List<ActiveKafkaListenerDto> getActiveListeners() {
    return kafkaListenerLifecycleService.getAllActive();
  }

}