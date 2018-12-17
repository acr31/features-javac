package uk.ac.cam.acr31.features.javac.graph;

import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType;

public class FeatureNodes {

  private static int nodeIdCounter = 0;

  static FeatureNode create(NodeType nodeType, String contents) {
    return FeatureNode.newBuilder()
        .setId(nodeIdCounter++)
        .setType(nodeType)
        .setContents(contents)
        .build();
  }
}
