package jetmock.controller;

import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import jetmock.dto.KafkaBrokerRequest;
import jetmock.dto.KafkaBrokerResponse;
import jetmock.service.SettingsService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class SettingsController {

  SettingsService settingsService;

  @GetMapping("/kafka")
  public List<KafkaBrokerResponse> getBrokers() {
    return settingsService.getAll();
  }

  @PostMapping("/kafka")
  public void save(@RequestBody KafkaBrokerRequest broker) {
    settingsService.save(broker);
  }

  @DeleteMapping("/kafka/{id}")
  public void delete(@PathVariable UUID id) {
    settingsService.delete(id);
  }

  @GetMapping("/kafka/{id}")
  public KafkaBrokerResponse findById(@PathVariable UUID id) {
    return settingsService.findById(id);
  }

}