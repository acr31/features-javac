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

import com.google.common.base.CaseFormat;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.text.StringEscapeUtils;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType;

class AstScanner {

  private final FeatureGraph featureGraph;
  private final EndPosTable endPosTable;

  private AstScanner(FeatureGraph featureGraph, EndPosTable endPosTable) {
    this.featureGraph = featureGraph;
    this.endPosTable = endPosTable;
  }

  static void addToGraph(JCTree.JCCompilationUnit compilationUnit, FeatureGraph featureGraph) {

    AstScanner scanner = new AstScanner(featureGraph, compilationUnit.endPositions);
    try {
      scanner.scanOrThrow(compilationUnit, null);
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private void scanOrThrow(JCTree node, GraphProtos.FeatureNode parent)
      throws InvocationTargetException, IllegalAccessException {

    GraphProtos.FeatureNode newNode =
        featureGraph.createFeatureNode(NodeType.AST_ELEMENT, node.getKind().toString(), node);
    if (parent != null) {
      featureGraph.addEdge(parent, newNode, EdgeType.AST_CHILD);
    } else {
      featureGraph.setAstRoot(newNode);
    }
    // TODO(acr31) check this implements Tree
    Class<?> treeInterface = node.getClass().getInterfaces()[0];

    for (Method m : treeInterface.getDeclaredMethods()) {
      if (m.getParameterCount() != 0) {
        continue;
      }

      // avoid methods which would involve visiting some part of the tree twice
      if (CompilationUnitTree.class.isAssignableFrom(treeInterface)) {
        if (m.getName().equals("getPackageName")
            || m.getName().equals("getSourceFile")
            || m.getName().equals("getLineMap")) {
          continue;
        }
      }

      if (NewClassTree.class.isAssignableFrom(treeInterface)) {
        NewClassTree newClassTree = (NewClassTree) node;
        if (newClassTree.getClassBody() != null && m.getName().equals("getIdentifier")) {
          continue;
        }
      }

      Object result = m.invoke(node);

      Deque<JCTree> toProcess = new ArrayDeque<>();
      if (Tree.class.isAssignableFrom(m.getReturnType())) {
        if (result != null && inSource((JCTree) result)) {
          toProcess.add((JCTree) result);
        }
      } else if (List.class.isAssignableFrom(m.getReturnType())) {
        if (result != null) {
          for (Object o : (List<?>) result) {
            if (o instanceof JCTree) {
              if (inSource((JCTree) o)) {
                toProcess.add((JCTree) o);
              }
            } else if (o instanceof List) {
              if (!((List<?>) o).isEmpty()) {
                throw new AssertionError(
                    "Unimplemented traversal of lists of lists " + o.getClass());
              }
            } else if (o != null) {
              throw new AssertionError(
                  "Expected list element to be castable to JCTree or List but it was "
                      + o.getClass());
            }
          }
        }
      } else if (Set.class.isAssignableFrom(m.getReturnType())) {
        GraphProtos.FeatureNode holderNode =
            featureGraph.createFeatureNode(
                NodeType.FAKE_AST, methodNameToNodeType(m.getName()), -1, -1);
        featureGraph.addEdge(newNode, holderNode, EdgeType.AST_CHILD);
        for (Object o : (Set<?>) result) {
          if (o instanceof JCTree) {
            throw new AssertionError(
                "Did not expect to find trees in a set but found " + o.getClass());
          }
          GraphProtos.FeatureNode valueNode =
              featureGraph.createFeatureNode(
                  NodeType.AST_LEAF, StringEscapeUtils.escapeJava(Objects.toString(o)), -1, -1);
          featureGraph.addEdge(holderNode, valueNode, EdgeType.AST_CHILD);
        }
      } else {
        String value = Objects.toString(result);
        if (m.getName().equals("isStatic")) {
          if (value.equals("false")) {
            continue;
          }
          value = "static";
        }
        if (node.getKind() == Tree.Kind.METHOD
            && m.getName().equals("getName")
            && value.equals("<init>")) {
          value = Symbols.getSymbol(node).map(sym -> sym.owner.name.toString()).orElseThrow();
        }
        GraphProtos.FeatureNode holderNode =
            featureGraph.createFeatureNode(
                NodeType.FAKE_AST, methodNameToNodeType(m.getName()), -1, -1);
        featureGraph.addEdge(newNode, holderNode, EdgeType.AST_CHILD);
        GraphProtos.FeatureNode valueNode =
            featureGraph.createFeatureNode(NodeType.AST_LEAF, value, -1, -1);
        featureGraph.addEdge(holderNode, valueNode, EdgeType.AST_CHILD);
      }

      if (!toProcess.isEmpty()) {
        JCTree firstChild = toProcess.peekFirst();
        JCTree lastChild = toProcess.peekLast();
        GraphProtos.FeatureNode holderNode =
            featureGraph.createFeatureNode(
                NodeType.FAKE_AST,
                methodNameToNodeType(m.getName()),
                firstChild.getStartPosition(),
                lastChild.getEndPosition(endPosTable));
        featureGraph.addEdge(newNode, holderNode, EdgeType.AST_CHILD);
        for (JCTree t : toProcess) {
          scanOrThrow(t, holderNode);
        }
      }
    }
  }

  private static String methodNameToNodeType(String name) {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name.replaceAll("^get", ""));
  }

  private static boolean inSource(JCTree child) {
    if (child instanceof JCTree.JCMethodDecl) {
      Symbol sym = ((JCTree.JCMethodDecl) child).sym;
      if (sym != null) {
        boolean synthetic = (sym.flags() & Flags.SYNTHETIC) != 0;
        boolean generatedConstructor = (sym.flags() & Flags.GENERATEDCONSTR) != 0;
        return !synthetic && !generatedConstructor;
      }
    }
    return true;
  }
}
