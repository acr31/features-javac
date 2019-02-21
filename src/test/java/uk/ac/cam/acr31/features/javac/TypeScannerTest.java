/*
 * Copyright © 2019 Henry Mercer (henry.mercer@me.com)
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

import static junit.framework.TestCase.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.testing.FeatureGraphChecks;
import uk.ac.cam.acr31.features.javac.testing.SourceSpan;
import uk.ac.cam.acr31.features.javac.testing.TestCompilation;

import java.util.List;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public class TypeScannerTest {
  @Test
  public void typeScanner_createsTypeNodesAndEdgesForIntVariableExp() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  public static void main(String[] args) {",
            "    int a = 4;",
            "  }",
            "}");
    SourceSpan type = compilation.sourceSpan("int");
    SourceSpan initializer = compilation.sourceSpan("4");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    GraphProtos.FeatureNode typeNode = FeatureGraphChecks.findAssociatedTypeNode(graph, type);
    GraphProtos.FeatureNode initializerNode = FeatureGraphChecks.findAssociatedTypeNode(graph, initializer);
    assertSame("typeNode and initializerNode should be associated with the same type node in the graph",
        typeNode,
        initializerNode);
    assertNotNull(typeNode);
    assertEquals("int", typeNode.getContents());
  }

  @Test
  public void typeScanner_createsTypeNodesAndEdgesForIntAssignmentExp() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  public static void main(String[] args) {",
            "    int a = 4;",
            "    a = 5;",
            "  }",
            "}");
    SourceSpan variable = compilation.sourceSpan("a", " = 5");
    SourceSpan newExpression = compilation.sourceSpan("5");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    GraphProtos.FeatureNode variableNode = FeatureGraphChecks.findAssociatedTypeNode(graph, variable);
    GraphProtos.FeatureNode newExpressionNode = FeatureGraphChecks.findAssociatedTypeNode(graph, newExpression);
    assertSame("variableNode and newExpressionNode should be associated with the same type node in the graph",
        variableNode,
        newExpressionNode);
    assertNotNull(variableNode);
    assertEquals("int", variableNode.getContents());
  }

  @Test
  public void typeScanner_createsTypeNodesAndEdgesForCallWithSameTypeArgs() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  private static void f(int a, int b) {",
            "  }",
            "  public static void main(String[] args) {",
            "    f(1 + 2, 3 + 4);",
            "  }",
            "}");
    SourceSpan arg1 = compilation.sourceSpan("1 + 2");
    SourceSpan arg2 = compilation.sourceSpan("3 + 4");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    GraphProtos.FeatureNode arg1TypeNode = FeatureGraphChecks.findAssociatedTypeNode(graph, arg1);
    GraphProtos.FeatureNode arg2TypeNode = FeatureGraphChecks.findAssociatedTypeNode(graph, arg2);
    assertSame("arg1Node and arg2Node should be associated with the same type node in the graph",
        arg1TypeNode,
        arg2TypeNode);
    assertNotNull(arg1TypeNode);
    assertEquals("int", arg1TypeNode.getContents());
  }

  @Test
  public void typeScanner_createsTypeNodesAndEdgesForCallWithDifferentTypeArgs() {
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
    assertNotNull(arg1TypeNode);
    assertNotNull(arg2TypeNode);
    assertEquals("Test.A", arg1TypeNode.getContents());
    assertEquals("Test.B", arg2TypeNode.getContents());
  }
}
