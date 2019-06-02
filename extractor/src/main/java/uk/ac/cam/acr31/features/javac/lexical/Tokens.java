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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import javax.tools.JavaFileObject;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType;

public class Tokens {

  public static void addToGraph(
      JavaFileObject sourceFile, Context context, FeatureGraph featureGraph) {

    ImmutableList<ErrorProneToken> tokens =
        ErrorProneTokens.getTokens(getSourceFileContent(sourceFile).toString(), context);
    FeatureNode previousTokenNode = null;
    for (ErrorProneToken token : tokens) {
      if (token.kind() == TokenKind.EOF) {
        break;
      }
      FeatureNode tokenNode =
          featureGraph.createFeatureNode(
              getNodeType(token), tokenToString(token), token.pos(), token.endPos());
      if (previousTokenNode != null) {
        featureGraph.addEdge(previousTokenNode, tokenNode, EdgeType.NEXT_TOKEN);
      } else {
        featureGraph.setFirstToken(tokenNode);
      }
      previousTokenNode = tokenNode;

      if (token.comments() != null) {
        for (Comment comment : token.comments()) {
          if (comment.getText() != null) {
            FeatureNode commentNode =
                featureGraph.createFeatureNode(
                    getCommentNodeType(comment),
                    comment.getText(),
                    comment.getSourcePos(0),
                    comment.getSourcePos(comment.getText().length() - 1) + 1);
            featureGraph.addEdge(commentNode, tokenNode, EdgeType.COMMENT);
          }
        }
      }
    }
  }

  private static CharSequence getSourceFileContent(JavaFileObject sourceFile) {
    try {
      return sourceFile.getCharContent(true);
    } catch (IOException e) {
      throw new RuntimeException("IOException reading from " + sourceFile.getName());
    }
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
      default:
        throw new IllegalArgumentException("Unrecognised comment type");
    }
  }

  private static String tokenToString(ErrorProneToken token) {
    switch (String.valueOf(token.kind().tag)) {
      case "STRING":
      case "NUMERIC":
        return token.stringVal();
      case "NAMED":
        return token.name().toString();
      default:
        return token.kind().name();
    }
  }

  private static NodeType getNodeType(ErrorProneToken token) {
    switch (token.kind()) {
      case IDENTIFIER:
      case SUPER:
      case THIS:
        return NodeType.IDENTIFIER_TOKEN;
      default:
        return NodeType.TOKEN;
    }
  }
}
