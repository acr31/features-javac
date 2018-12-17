/*
 * Copyright © 2018 Andrew Rice (acr31@cam.ac.uk)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.acr31.features.javac.graph;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.Graph;

public class ProtoOutput {

  public static void write(File outputFile, FeatureGraph featureGraph) throws IOException {
    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      toGraph(featureGraph).writeTo(fos);
    }
  }

  static Graph toGraph(FeatureGraph featureGraph) {
    Graph.Builder result = Graph.newBuilder().addAllNode(featureGraph.nodes());
    featureGraph
        .edges()
        .stream()
        .map(p -> FeatureEdge.newBuilder().build())
        .forEach(result::addEdge);
    return result.build();
  }
}
