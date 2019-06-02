/*
 * Copyright © 2018 The Authors (see NOTICE file)
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

package uk.ac.cam.acr31.features.javac.semantic;

import javax.lang.model.type.TypeMirror;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos;

public class AssignabilityAnalysis {
  private final FeatureGraph graph;
  private final TypeAnalysis analysis;

  public AssignabilityAnalysis(FeatureGraph graph, TypeAnalysis analysis) {
    this.graph = graph;
    this.analysis = analysis;
  }

  public static void addToGraph(FeatureGraph graph, TypeAnalysis analysis) {
    AssignabilityAnalysis assignabilityAnalysis = new AssignabilityAnalysis(graph, analysis);
    assignabilityAnalysis.performAnalysis();
  }

  private void performAnalysis() {
    for (GraphProtos.FeatureNode typeNode : graph.types()) {
      TypeMirror type = graph.lookupTypeMirror(typeNode);
      if (type == null) {
        continue;
      }
      for (GraphProtos.FeatureNode assignToTypeNode : graph.types()) {
        TypeMirror assignToType = graph.lookupTypeMirror(assignToTypeNode);
        if (assignToType == null || analysis.getTypes().isSameType(type, assignToType)) {
          continue;
        }

        if (analysis.isAssignable(type, assignToType)) {
          graph.addEdge(typeNode, assignToTypeNode, GraphProtos.FeatureEdge.EdgeType.ASSIGNABLE_TO);
        }
      }
    }
  }
}
