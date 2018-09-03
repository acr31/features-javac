package uk.ac.cam.acr31.features.javac.lexical;

import static com.google.common.collect.ImmutableRangeMap.toImmutableRangeMap;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.util.Map;
import javax.tools.JavaFileObject;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.graph.FeatureNode;
import uk.ac.cam.acr31.features.javac.graph.NodeType;

public class Tokens {

  public static ImmutableRangeMap<Integer, com.sun.tools.javac.parser.Tokens.Token> getTokens(
      JavaFileObject sourceFile, Context context) {
    ScannerFactory scannerFactory = ScannerFactory.instance(context);
    Scanner tokenScanner = scannerFactory.newScanner(getSourceFileContent(sourceFile), true);
    ImmutableRangeMap.Builder<Integer, com.sun.tools.javac.parser.Tokens.Token> tokenMap =
        ImmutableRangeMap.builder();
    while (true) {
      tokenScanner.nextToken();
      com.sun.tools.javac.parser.Tokens.Token token = tokenScanner.token();
      if (token.kind == com.sun.tools.javac.parser.Tokens.TokenKind.EOF) {
        break;
      }
      tokenMap.put(Range.closedOpen(token.pos, token.endPos), token);
    }
    return tokenMap.build();
  }

  private static CharSequence getSourceFileContent(JavaFileObject sourceFile) {
    try {
      return sourceFile.getCharContent(true);
    } catch (IOException e) {
      throw new RuntimeException("IOException reading from " + sourceFile.getName());
    }
  }

  public static ImmutableRangeMap<Integer, FeatureNode> addToFeatureGraph(
      ImmutableRangeMap<Integer, com.sun.tools.javac.parser.Tokens.Token> tokens,
      FeatureGraph featureGraph) {
    return tokens
        .asMapOfRanges()
        .entrySet()
        .stream()
        .collect(
            toImmutableRangeMap(
                Map.Entry::getKey,
                entry ->
                    featureGraph.createFeatureNode(
                        NodeType.TOKEN, tokenToString(entry.getValue()))));
  }

  private static String tokenToString(com.sun.tools.javac.parser.Tokens.Token token) {
    String content;
    switch (String.valueOf(token.kind.tag)) {
      case "STRING":
        content = token.stringVal();
        break;
      case "NAMED":
        content = token.name().toString();
        break;
      case "NUMERIC":
        content = String.valueOf(token.radix());
        break;
      default:
        content = token.kind.name();
    }
    return content;
  }
}
