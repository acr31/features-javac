package uk.ac.cam.acr31.features.javac;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.testing.FeatureGraphChecks;
import uk.ac.cam.acr31.features.javac.testing.SourceSpan;
import uk.ac.cam.acr31.features.javac.testing.TestCompilation;

@RunWith(JUnit4.class)
public class DataflowTest {

  @Test
  public void lastUsed_addsEdge_toArrayUsage() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  void method() {",
            "    int[] a = new int[3];",
            "    a[1] = 0;",
            "  }",
            "}");
    SourceSpan init = compilation.sourceSpan("int[] ", "a", " = ");
    SourceSpan write = compilation.sourceSpan("a", "[");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    assertThat(graph.edges(EdgeType.LAST_WRITE))
        .containsExactly(FeatureGraphChecks.edgeBetween(graph, init, write, EdgeType.LAST_WRITE));
  }
}
