package jetmock.controller;

import jetmock.service.DebugRocksService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/__debug/rocks")
@RequiredArgsConstructor
@Profile({"dev", "local"})
public class DebugRocksController {

  private final DebugRocksService debugRocksService;

  /**
   * Bütün DB dump.
   * GET /__debug/rocks/all
   */
  @GetMapping("/all")
  public ResponseEntity<?> dumpAll() {
    return ResponseEntity.ok(debugRocksService.dumpAll());
  }

  /**
   * Prefix üzrə dump.
   * GET /__debug/rocks/prefix?key=flow:
   */
  @GetMapping("/prefix")
  public ResponseEntity<?> dumpByPrefix(@RequestParam("key") String prefix) {
    return ResponseEntity.ok(debugRocksService.dumpByPrefix(prefix));
  }

  /**
   * Prefix üzrə DELETE.
   * DELETE /__debug/rocks/prefix?key=flow:
   */
  @DeleteMapping("/prefix")
  public ResponseEntity<?> deleteByPrefix(@RequestParam("key") String prefix) {
    return ResponseEntity.ok(debugRocksService.deleteByPrefix(prefix));
  }

  /**
   * Tək key oxu.
   * GET /__debug/rocks/key?key=flow:123
   */
  @GetMapping("/key")
  public ResponseEntity<?> getByKey(@RequestParam("key") String key) {
    Object value = debugRocksService.getByKey(key);
    if (value == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(value);
  }

}
