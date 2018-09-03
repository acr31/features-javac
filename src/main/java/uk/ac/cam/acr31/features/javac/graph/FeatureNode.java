package uk.ac.cam.acr31.features.javac.graph;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class FeatureNode {

  private static long counter = 0;

  abstract long nodeId();

  abstract NodeType nodeType();

  abstract String contents();

  static FeatureNode create(NodeType nodeType, String contents) {
    return new AutoValue_FeatureNode(counter++, nodeType, contents);
  }
}
