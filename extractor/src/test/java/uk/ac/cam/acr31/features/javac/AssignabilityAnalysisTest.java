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
package uk.ac.cam.acr31.features.javac;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.testing.FeatureGraphChecks;
import uk.ac.cam.acr31.features.javac.testing.SourceSpan;
import uk.ac.cam.acr31.features.javac.testing.TestCompilation;

@RunWith(JUnit4.class)
public class AssignabilityAnalysisTest {
  @Test
  public void assignabilityAnalysis_doesNotIntroduceAssignableEdgeBetweenUnassignableClasses() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  static class A {}",
            "  static class B {}",
            "  private static void f(A a, B b) {",
            "  }",
            "  public static void main(String[] args) {",
            "    f(new A(), new B());",
            "  }",
            "}");
    SourceSpan arg1 = compilation.sourceSpan("new A()");
    SourceSpan arg2 = compilation.sourceSpan("new B()");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    GraphProtos.FeatureNode arg1TypeNode = FeatureGraphChecks.findAssociatedTypeNode(graph, arg1);
    GraphProtos.FeatureNode arg2TypeNode = FeatureGraphChecks.findAssociatedTypeNode(graph, arg2);

    assertThat(
            FeatureGraphChecks.isEdgeBetween(
                graph, arg1TypeNode, arg2TypeNode, EdgeType.ASSIGNABLE_TO))
        .isFalse();
    assertThat(
            FeatureGraphChecks.isEdgeBetween(
                graph, arg2TypeNode, arg1TypeNode, EdgeType.ASSIGNABLE_TO))
        .isFalse();
  }

  @Test
  public void assignabilityAnalysis_doesNotIntroduceAssignableEdgeBetweenAssignableClasses() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  static class A {}",
            "  static class B extends A {}",
            "  private static void f(A a, B b) {",
            "  }",
            "  public static void main(String[] args) {",
            "    f(new A(), new B());",
            "  }",
            "}");
    SourceSpan arg1 = compilation.sourceSpan("new A()");
    SourceSpan arg2 = compilation.sourceSpan("new B()");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    GraphProtos.FeatureNode arg1TypeNode = FeatureGraphChecks.findAssociatedTypeNode(graph, arg1);
    GraphProtos.FeatureNode arg2TypeNode = FeatureGraphChecks.findAssociatedTypeNode(graph, arg2);
    assertThat(
            FeatureGraphChecks.isEdgeBetween(
                graph, arg1TypeNode, arg2TypeNode, EdgeType.ASSIGNABLE_TO))
        .isFalse();
    assertThat(
            FeatureGraphChecks.isEdgeBetween(
                graph, arg2TypeNode, arg1TypeNode, EdgeType.ASSIGNABLE_TO))
        .isTrue();
  }
}
