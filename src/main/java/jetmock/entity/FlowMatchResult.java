package jetmock.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FlowMatchResult {

  //common
  String id;
  String expression;

  //api trigger
  String method;
  String path;

  //kafka trigger
  String brokerId;
  String topic;

}