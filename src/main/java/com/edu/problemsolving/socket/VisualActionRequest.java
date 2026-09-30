package com.edu.problemsolving.socket;

import java.util.List;
import java.util.Map;

public record VisualActionRequest(String actionKey, String animation, List<Map<String, Object>> entities,
                                  Map<String, Object> variables) {
}
