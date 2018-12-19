package uk.ac.cam.acr31.features.javac;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.testing.FeatureGraphChecks;
import uk.ac.cam.acr31.features.javac.testing.TestCompilation;

@RunWith(JUnit4.class)
public class ComputedFromTest {

  @Test
  public void computedFrom_addsEdgesToLocalVariables() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  public static void main(String[] args) {",
            "    int a = 0;",
            "    int b = 0;",
            "    int c = a + b;",
            "  }",
            "}");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(
        FeatureGraphChecks.edgeBetween(graph, "VARIABLE,c", "VARIABLE,a", EdgeType.COMPUTED_FROM));
    assertThat(
        FeatureGraphChecks.edgeBetween(graph, "VARIABLE,c", "VARIABLE,b", EdgeType.COMPUTED_FROM));
  }

  @Test
  public void computedFrom_addsEdgesToFields() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  static int a = 0;",
            "  public static void main(String[] args) {",
            "    int b = 0;",
            "    int c = a + b;",
            "  }",
            "}");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(
        FeatureGraphChecks.edgeBetween(graph, "VARIABLE,c", "VARIABLE,a", EdgeType.COMPUTED_FROM));
    assertThat(
        FeatureGraphChecks.edgeBetween(graph, "VARIABLE,c", "VARIABLE,b", EdgeType.COMPUTED_FROM));
  }
}
