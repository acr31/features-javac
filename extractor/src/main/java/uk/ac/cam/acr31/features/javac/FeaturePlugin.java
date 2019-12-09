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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Options;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.graph.ProtoOutput;
import uk.ac.cam.acr31.features.javac.lexical.Tokens;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType;
import uk.ac.cam.acr31.features.javac.semantic.AssignabilityAnalysis;
import uk.ac.cam.acr31.features.javac.semantic.DataflowOutputs;
import uk.ac.cam.acr31.features.javac.semantic.TypeAnalysis;
import uk.ac.cam.acr31.features.javac.semantic.TypeScanner;
import uk.ac.cam.acr31.features.javac.syntactic.ComputedFromScanner;
import uk.ac.cam.acr31.features.javac.syntactic.FormalArgScanner;
import uk.ac.cam.acr31.features.javac.syntactic.GuardedByScanner;
import uk.ac.cam.acr31.features.javac.syntactic.LastLexicalUseScanner;
import uk.ac.cam.acr31.features.javac.syntactic.ReturnsToScanner;
import uk.ac.cam.acr31.features.javac.syntactic.SymbolScanner;

public class FeaturePlugin implements Plugin {

  private static final String FEATURES_OUTPUT_DIRECTORY = "featuresOutputDirectory";
  private static final String ABORT_ON_ERROR = "abortOnError";

  @Override
  public String getName() {
    return "FeaturePlugin";
  }

  @Override
  public void init(JavacTask task, String... args) {

    Context context = ((BasicJavacTask) task).getContext();

    task.addTaskListener(
        new TaskListener() {
          @Override
          public void finished(TaskEvent e) {
            if (e.getKind() != TaskEvent.Kind.ANALYZE) {
              return;
            }

            process(e, context);
          }
        });
  }

  private static void process(TaskEvent taskEvent, Context context) {

    Options options = Options.instance(context);

    boolean abortOnError = options.getBoolean(ABORT_ON_ERROR);
    String featuresOutputDirectory = ".";
    if (options.isSet(FEATURES_OUTPUT_DIRECTORY)) {
      featuresOutputDirectory = options.get(FEATURES_OUTPUT_DIRECTORY);
    }

    JCTree.JCCompilationUnit compilationUnit =
        (JCTree.JCCompilationUnit) taskEvent.getCompilationUnit();

    try {
      FeatureGraph featureGraph = createFeatureGraph(compilationUnit, context);
      writeOutput(featureGraph, featuresOutputDirectory);
    } catch (AssertionError | RuntimeException e) {
      String message = "Feature extraction failed: " + taskEvent.getSourceFile().getName();
      if (abortOnError) {
        throw new RuntimeException(message, e);
      } else {
        System.out.println(message);
      }
    }
  }

  private static void mkdirFor(File file) {
    File directory = file.getParentFile();
    if (directory.exists()) {
      return;
    }
    if (!directory.mkdirs()) {
      throw new IOError(new IOException("Failed to create directory for " + file));
    }
  }

  private static void writeOutput(FeatureGraph featureGraph, String featuresOutputDirectory) {
    File protoFile = new File(featuresOutputDirectory, featureGraph.getSourceFileName() + ".proto");
    mkdirFor(protoFile);
    ProtoOutput.write(protoFile, featureGraph);
  }

  static FeatureGraph createFeatureGraph(
      JCTree.JCCompilationUnit compilationUnit, Context context) {
    FeatureGraph featureGraph =
        new FeatureGraph(
            compilationUnit.getSourceFile().getName(),
            compilationUnit.endPositions,
            compilationUnit.lineMap);
    AstScanner.addToGraph(compilationUnit, featureGraph);
    Tokens.addToGraph(compilationUnit.getSourceFile(), context, featureGraph);
    linkTokensToAstNodes(featureGraph);
    // prune all ast nodes with no successors (these are leaves not connected to tokens)
    featureGraph.pruneAstNodes();
    //    removeIdentifierAstNodes(featureGraph);

    JavacProcessingEnvironment processingEnvironment = JavacProcessingEnvironment.instance(context);
    ImmutableMap<ClassTree, ImmutableMap<MethodTree, DataflowOutputs>> analysisResults =
        DataflowOutputs.create(compilationUnit, processingEnvironment);
    DataflowOutputsScanner.addToGraph(compilationUnit, analysisResults, featureGraph);

    TypeAnalysis typeAnalysis = new TypeAnalysis(compilationUnit, processingEnvironment);
    TypeScanner.addToGraph(compilationUnit, featureGraph, typeAnalysis);
    AssignabilityAnalysis.addToGraph(featureGraph, typeAnalysis);

    ComputedFromScanner.addToGraph(compilationUnit, featureGraph);
    LastLexicalUseScanner.addToGraph(compilationUnit, featureGraph);
    ReturnsToScanner.addToGraph(compilationUnit, featureGraph);
    FormalArgScanner.addToGraph(compilationUnit, featureGraph);
    GuardedByScanner.addToGraph(compilationUnit, featureGraph);
    SymbolScanner.addToGraph(compilationUnit, featureGraph);
    linkCommentsToAstNodes(featureGraph);
    checkSymbols(featureGraph);

    return featureGraph;
  }

  /**
   * Check that all identifier tokens have a symbol associated.
   *
   * <p>This excludes tokens within the package declaration since javac only allocates a symbol to
   * the whole package.
   */
  private static void checkSymbols(FeatureGraph graph) {
    graph.nodes().stream()
        .filter(n -> n.getType().equals(NodeType.IDENTIFIER_TOKEN))
        .filter(n -> !graph.hasAncestor(n, NodeType.AST_ELEMENT, "PACKAGE"))
        .filter(n -> !graph.hasAncestor(n, NodeType.AST_ELEMENT, "LABELED_STATEMENT"))
        .forEach(
            token -> {
              if (graph.predecessors(token, EdgeType.ASSOCIATED_SYMBOL).isEmpty()) {
                throw new AssertionError(token + " has no associated symbol");
              }
            });
  }

  private static void removeIdentifierAstNodes(FeatureGraph graph) {
    for (FeatureNode node : graph.astNodes()) {
      if (node.getContents().equals("IDENTIFIER")) {
        Set<FeatureNode> sources = graph.predecessors(node, EdgeType.AST_CHILD);
        for (EdgeType edgeType : ImmutableList.of(EdgeType.ASSOCIATED_TOKEN, EdgeType.AST_CHILD)) {
          if (removeNode(graph, node, sources, edgeType)) {
            break;
          }
        }
      }
    }
  }

  private static boolean removeNode(
      FeatureGraph graph, FeatureNode node, Set<FeatureNode> sources, EdgeType edgeType) {
    Set<FeatureNode> successors = graph.successors(node, edgeType);
    if (successors.isEmpty()) {
      return false;
    }
    FeatureNode dest = Iterables.getOnlyElement(successors);
    graph.removeNode(node);
    for (FeatureNode source : sources) {
      graph.addEdge(source, dest, edgeType);
    }
    graph.replaceNodeInNodeMap(node, dest);
    return true;
  }

  private static final Comparator<FeatureNode> TOKENS_LAST =
      Comparator.comparing(
          n -> {
            switch (n.getType()) {
              case TOKEN:
                return 1;
              default:
                return 0;
            }
          });

  private static final Comparator<FeatureNode> BY_START_POSITION =
      Comparator.comparing(FeatureNode::getStartPosition);

  private static final Comparator<FeatureNode> BY_ID = Comparator.comparing(FeatureNode::getId);

  private static void linkTokensToAstNodes(FeatureGraph featureGraph) {

    ImmutableSortedSet<FeatureNode> astNodes =
        ImmutableSortedSet.orderedBy(
                BY_START_POSITION.thenComparing(TOKENS_LAST).thenComparing(BY_ID))
            .addAll(featureGraph.astNodes())
            .addAll(featureGraph.tokens())
            .build();

    // First of all we consider variable trees and associate them with the first token within their
    // span that has a matching name. This is necessary because there are various ways that javac
    // creates a smaller node that swallows the variable identifier.
    astNodes.stream()
        .filter(node -> node.getType().equals(NodeType.AST_ELEMENT))
        .filter(node -> node.getContents().equals("VARIABLE"))
        .forEach(
            node -> {
              JCTree.JCVariableDecl variableTree =
                  (JCTree.JCVariableDecl) featureGraph.lookupTree(node);
              Name expectedName = variableTree.getName();
              Optional<FeatureNode> matchingToken =
                  astNodes.tailSet(node).stream()
                      .filter(target -> target.getType().equals(NodeType.IDENTIFIER_TOKEN))
                      .filter(target -> expectedName.contentEquals(target.getContents()))
                      .findFirst();
              matchingToken.ifPresent(
                  token ->
                      findMatchingLeaf(node, token, featureGraph)
                          .ifPresent(
                              leaf ->
                                  featureGraph.addEdge(leaf, token, EdgeType.ASSOCIATED_TOKEN)));
            });

    for (FeatureNode token : featureGraph.tokens()) {
      if (!featureGraph.predecessors(token, EdgeType.ASSOCIATED_TOKEN).isEmpty()) {
        continue;
      }
      Comparator<FeatureNode> nodeSpan =
          Comparator.comparing(n -> n.getEndPosition() - n.getStartPosition());
      Comparator<FeatureNode> nodeId = Comparator.comparingLong(FeatureNode::getId).reversed();
      Optional<FeatureNode> smallestEncompassingNode =
          astNodes.headSet(token).stream()
              .filter(n -> n.getType().equals(NodeType.AST_ELEMENT))
              .filter(n -> n.getEndPosition() >= token.getEndPosition())
              .min(nodeSpan.thenComparing(nodeId));
      if (smallestEncompassingNode.isPresent()) {
        Optional<FeatureNode> matchingLeaf =
            findMatchingLeaf(smallestEncompassingNode.get(), token, featureGraph);
        if (matchingLeaf.isPresent()) {
          featureGraph.addEdge(matchingLeaf.get(), token, EdgeType.ASSOCIATED_TOKEN);
        } else {
          featureGraph.addEdge(smallestEncompassingNode.get(), token, EdgeType.ASSOCIATED_TOKEN);
        }
      }
    }
  }

  private static Optional<FeatureNode> findMatchingLeaf(
      FeatureNode current, FeatureNode token, FeatureGraph graph) {
    for (FeatureNode child : graph.successors(current, EdgeType.AST_CHILD)) {
      if (child.getType() == NodeType.FAKE_AST) {
        for (FeatureNode possibleLeaf : graph.successors(child, EdgeType.AST_CHILD)) {
          if (possibleLeaf.getType() == NodeType.AST_LEAF
              && possibleLeaf.getContents().equalsIgnoreCase(token.getContents())) {
            return Optional.of(possibleLeaf);
          }
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Find all comments which start on a different line to the token they precede and move them to
   * associate with the largest ast node.
   */
  private static void linkCommentsToAstNodes(FeatureGraph featureGraph) {
    for (FeatureNode comment : featureGraph.comments()) {
      FeatureNode successor =
          Iterables.getOnlyElement(featureGraph.successors(comment, EdgeType.COMMENT));
      if (comment.getEndLineNumber() == successor.getStartLineNumber()) {
        continue;
      }
      FeatureNode match = null;
      for (FeatureNode astNode : featureGraph.nodes()) {
        if (astNode.getType().equals(NodeType.FAKE_AST)) {
          continue;
        }
        int distance = astNode.getStartPosition() - comment.getEndPosition();
        if (distance < 0) {
          continue;
        }
        int size = astNode.getEndPosition() - astNode.getStartPosition();
        if (match != null) {
          int matchDistance = match.getStartPosition() - comment.getEndPosition();
          int matchSize = match.getEndPosition() - match.getStartPosition();
          if (distance < matchDistance) {
            match = astNode;
          } else if (distance == matchDistance && size > matchSize) {
            match = astNode;
          }
        } else {
          match = astNode;
        }
      }
      if (match != null) {
        featureGraph.removeEdge(
            FeatureEdge.newBuilder()
                .setSourceId(comment.getId())
                .setDestinationId(successor.getId())
                .setType(EdgeType.COMMENT)
                .build());

        featureGraph.addEdge(comment, match, EdgeType.COMMENT);
      }
    }
  }
}
