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

package uk.ac.cam.acr31.features.javac.lexical;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.parser.Tokens.Token;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.util.Map;
import javax.tools.JavaFileObject;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType;

public class Tokens {

  public static ImmutableRangeMap<Integer, Token> getTokens(
      JavaFileObject sourceFile, Context context) {
    ScannerFactory scannerFactory = ScannerFactory.instance(context);
    Scanner tokenScanner = scannerFactory.newScanner(getSourceFileContent(sourceFile), true);
    ImmutableRangeMap.Builder<Integer, Token> tokenMap = ImmutableRangeMap.builder();
    while (true) {
      tokenScanner.nextToken();
      Token token = tokenScanner.token();
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
      ImmutableRangeMap<Integer, Token> tokens, FeatureGraph featureGraph) {
    ImmutableRangeMap.Builder<Integer, FeatureNode> result = ImmutableRangeMap.builder();
    for (Map.Entry<Range<Integer>, Token> entry : tokens.asMapOfRanges().entrySet()) {
      Token token = entry.getValue();
      FeatureNode featureNode =
          featureGraph.createFeatureNode(NodeType.TOKEN, tokenToString(token));
      result.put(entry.getKey(), featureNode);
      if (token.comments != null) {
        for (Comment comment : token.comments) {
          if (comment.getText() != null) {
            FeatureNode commentNode =
                featureGraph.createFeatureNode(getCommentNodeType(comment), comment.getText());
            featureGraph.putEdgeValue(featureNode, commentNode, EdgeType.COMMENT);
          }
        }
      }
    }
    return result.build();
  }

  private static NodeType getCommentNodeType(Comment comment) {
    Comment.CommentStyle style = comment.getStyle();
    switch (style) {
      case LINE:
        return NodeType.COMMENT_LINE;
      case BLOCK:
        return NodeType.COMMENT_BLOCK;
      case JAVADOC:
        return NodeType.COMMENT_JAVADOC;
    }
    throw new IllegalArgumentException("Unrecognised comment type");
  }

  private static String tokenToString(Token token) {
    switch (String.valueOf(token.kind.tag)) {
      case "STRING":
        return token.stringVal();
      case "NAMED":
        return token.name().toString();

      case "NUMERIC":
        return String.valueOf(token.radix());
      default:
        return token.kind.name();
    }
  }
}
