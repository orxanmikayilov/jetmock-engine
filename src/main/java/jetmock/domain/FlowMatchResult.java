package jetmock.domain;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FlowMatchResult {

  //common
  UUID id;
  String expression;

  //api trigger
  String method;
  String path;

  //kafka trigger
  String brokerId;
  String topic;

}