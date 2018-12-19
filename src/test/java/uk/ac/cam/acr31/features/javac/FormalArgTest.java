package uk.ac.cam.acr31.features.javac;

import static com.google.common.truth.Truth8.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos;
import uk.ac.cam.acr31.features.javac.testing.FeatureGraphChecks;
import uk.ac.cam.acr31.features.javac.testing.TestCompilation;

@RunWith(JUnit4.class)
public class FormalArgTest {

  @Test
  public void formalArg_createsEdge_betweenIdentifierAndFormalParameter() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  static void b(int formalParameter) {}",
            "  public static void main(String[] args) {",
            "    int a = 4;",
            "    b(a);",
            "  }",
            "}");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(
            FeatureGraphChecks.edgeBetween(
                graph,
                "IDENTIFIER,a",
                "VARIABLE,formalParameter",
                GraphProtos.FeatureEdge.EdgeType.FORMAL_ARG_NAME))
        .isPresent();
  }
}
