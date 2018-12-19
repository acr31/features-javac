package uk.ac.cam.acr31.features.javac;

import static com.google.common.truth.Truth8.assertThat;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.testing.FeatureGraphChecks;
import uk.ac.cam.acr31.features.javac.testing.TestCompilation;

@RunWith(JUnit4.class)
public class LastLexicalUseTest {

  @Test
  public void lastLexicalUse_chainsInOrder() {
    // ARRANGE
    TestCompilation compilation =
        TestCompilation.compile(
            "Test.java", //
            "public class Test {",
            "  public static void main(String[] args) {",
            "    int x = 0;",
            "    if (x>4) {",
            "      x++;",
            "    }",
            "    else {",
            "      x+=1;",
            "    }",
            "  }",
            "}");

    // ACT
    FeatureGraph graph =
        FeaturePlugin.createFeatureGraph(compilation.compilationUnit(), compilation.context());

    // ASSERT
    Optional<GraphProtos.FeatureEdge> first =
        FeatureGraphChecks.edgeBetween(
            graph, "VARIABLE,x", "GREATER_THAN,IDENTIFIER,x", EdgeType.LAST_LEXICAL_USE);
    Optional<GraphProtos.FeatureEdge> second =
        FeatureGraphChecks.edgeBetween(
            graph,
            "GREATER_THAN,IDENTIFIER,x",
            "POSTFIX_INCREMENT,IDENTIFIER,x",
            EdgeType.LAST_LEXICAL_USE);
    Optional<GraphProtos.FeatureEdge> third =
        FeatureGraphChecks.edgeBetween(
            graph,
            "POSTFIX_INCREMENT,IDENTIFIER,x",
            "PLUS_ASSIGNMENT,IDENTIFIER,x",
            EdgeType.LAST_LEXICAL_USE);
    assertThat(first).isPresent();
    assertThat(second).isPresent();
    assertThat(third).isPresent();
  }
}
