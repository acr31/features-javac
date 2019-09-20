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

package uk.ac.cam.acr31.features.javac.proto;

import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType;

/** Methods for interpreting a NodeType object. */
public class NodeTypes {

  /** Return true if a NodeType is a comment. */
  public static boolean isComment(NodeType nodeType) {
    switch (nodeType) {
      case COMMENT_BLOCK:
      case COMMENT_JAVADOC:
      case COMMENT_LINE:
        return true;
      default:
        return false;
    }
  }

  /** Return true if a nodeType is a symbol. */
  public static boolean isSymbol(NodeType nodeType) {
    switch (nodeType) {
      case SYMBOL:
      case SYMBOL_MTH:
      case SYMBOL_VAR:
      case SYMBOL_TYP:
        return true;
      default:
        return false;
    }
  }

  /** Return true if a nodeType is a token. */
  public static boolean isToken(NodeType nodeType) {
    switch (nodeType) {
      case TOKEN:
      case IDENTIFIER_TOKEN:
        return true;
      default:
        return false;
    }
  }
}
