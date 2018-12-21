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

package uk.ac.cam.acr31.features.javac;

import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import uk.ac.cam.acr31.features.javac.graph.DotOutput;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.graph.ProtoOutput;
import uk.ac.cam.acr31.features.javac.lexical.Tokens;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureEdge.EdgeType;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType;
import uk.ac.cam.acr31.features.javac.semantic.DataflowOutputs;
import uk.ac.cam.acr31.features.javac.syntactic.ComputedFromScanner;
import uk.ac.cam.acr31.features.javac.syntactic.FormalArgScanner;
import uk.ac.cam.acr31.features.javac.syntactic.GuardedByScanner;
import uk.ac.cam.acr31.features.javac.syntactic.LastLexicalUseScanner;
import uk.ac.cam.acr31.features.javac.syntactic.ReturnsToScanner;

public class FeaturePlugin implements Plugin {

  private static final String FEATURES_OUTPUT_DIRECTORY = "featuresOutputDirectory";

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
    try {
      JCTree.JCCompilationUnit compilationUnit =
          (JCTree.JCCompilationUnit) taskEvent.getCompilationUnit();
      FeatureGraph featureGraph = createFeatureGraph(compilationUnit, context);
      Options options = Options.instance(context);
      String featuresOutputDirectory = ".";
      if (options.isSet(FEATURES_OUTPUT_DIRECTORY)) {
        featuresOutputDirectory = options.get(FEATURES_OUTPUT_DIRECTORY);
      }
      writeOutput(featureGraph, featuresOutputDirectory);
    } catch (AssertionError e) {
      throw new RuntimeException(
          "Failed to extract features from " + taskEvent.getSourceFile().getName(), e);
    }
  }

  private static void writeOutput(FeatureGraph featureGraph, String featuresOutputDirectory) {

    File outputFile = new File(featuresOutputDirectory, featureGraph.getSourceFileName() + ".dot");
    if (!outputFile.getParentFile().mkdirs()) {
      throw new IOError(new IOException("Failed to create directory " + outputFile.getParent()));
    }
    DotOutput.writeToDot(outputFile, featureGraph);
    System.out.println("Wrote: " + outputFile);

    File protoFile = new File(featuresOutputDirectory, featureGraph.getSourceFileName() + ".proto");
    if (!protoFile.getParentFile().mkdirs()) {
      throw new IOError(new IOException("Failed to create directory " + protoFile.getParent()));
    }
    ProtoOutput.write(protoFile, featureGraph);
    System.out.println("Wrote: " + protoFile);
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
    removeIdentifierAstNodes(featureGraph);

    JavacProcessingEnvironment processingEnvironment = JavacProcessingEnvironment.instance(context);
    var analysisResults = DataflowOutputs.create(compilationUnit, processingEnvironment);
    DataflowOutputsScanner.addToGraph(compilationUnit, analysisResults, featureGraph);

    ComputedFromScanner.addToGraph(compilationUnit, featureGraph);
    LastLexicalUseScanner.addToGraph(compilationUnit, featureGraph);
    ReturnsToScanner.addToGraph(compilationUnit, featureGraph);
    FormalArgScanner.addToGraph(compilationUnit, featureGraph);
    GuardedByScanner.addToGraph(compilationUnit, featureGraph);

    // prune all ast nodes with no successors (these are leaves not connected to tokens)
    featureGraph.pruneLeaves(NodeType.AST_ELEMENT);

    linkCommentsToAstNodes(featureGraph);

    return featureGraph;
  }

  private static void removeIdentifierAstNodes(FeatureGraph graph) {
    for (FeatureNode node : graph.astNodes()) {
      if (node.getContents().equals("IDENTIFIER")) {
        FeatureNode source = Iterables.getOnlyElement(graph.predecessors(node, EdgeType.AST_CHILD));
        FeatureNode dest =
            Iterables.getOnlyElement(graph.successors(node, EdgeType.ASSOCIATED_TOKEN));
        graph.removeNode(node);
        graph.addEdge(source, dest, EdgeType.ASSOCIATED_TOKEN);
        graph.replaceNodeInNodeMap(node, dest);
      }
    }
  }

  private static void linkTokensToAstNodes(FeatureGraph featureGraph) {

    RangeMap<Integer, FeatureNode> astRanges = TreeRangeMap.create();
    Deque<FeatureNode> work = new ArrayDeque<>();
    work.add(featureGraph.root());
    Set<FeatureNode> visited = new HashSet<>();
    while (!work.isEmpty()) {
      FeatureNode next = Objects.requireNonNull(work.poll());
      if (visited.contains(next)) {
        continue;
      }
      visited.add(next);
      astRanges.put(Range.closedOpen(next.getStartPosition(), next.getEndPosition()), next);
      work.addAll(featureGraph.successors(next, EdgeType.AST_CHILD));
    }

    for (FeatureNode token : featureGraph.tokens()) {
      FeatureNode smallestNode =
          astRanges
              .subRangeMap(Range.closedOpen(token.getStartPosition(), token.getEndPosition()))
              .asMapOfRanges()
              .entrySet()
              .stream()
              .min(
                  Comparator.comparing(
                      entry -> entry.getKey().upperEndpoint() - entry.getKey().lowerEndpoint()))
              .orElseThrow(AssertionError::new)
              .getValue();
      featureGraph.addEdge(smallestNode, token, EdgeType.ASSOCIATED_TOKEN);
    }
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
        if (astNode.getType().equals(NodeType.SYNTHETIC_AST_ELEMENT)) {
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
