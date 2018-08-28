package uk.ac.cam.acr31.features.javac;

import com.google.common.collect.ImmutableList;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.dataflow.analysis.AnalysisResult;

class TreeNode {

  final ImmutableList<String> contents;
  final List<TreeNode> children;
  final Tree node;

  TreeNode(Tree node) {
    this(node, ImmutableList.of());
  }

  TreeNode(Tree node, String... contents) {
    this(node, Arrays.asList(contents));
  }

  private TreeNode(Tree node, Iterable<String> contents) {
    this.contents = ImmutableList.copyOf(contents);
    this.children = new ArrayList<>();
    this.node = node;
  }

  @Override
  public String toString() {
    String children =
        Stream.concat(contents.stream(), this.children.stream().map(TreeNode::toString))
            .filter(Objects::nonNull)
            .collect(Collectors.joining(","));
    return String.format("(%s)", children);
  }

  String asString(
      AnalysisResult<PossibleTreeSet, PossibleTreeSetStore> lastWrites,
      AnalysisResult<PossibleTreeSet, PossibleTreeSetStore> lastReads) {
    String children =
        Stream.concat(
                contents.stream(),
                this.children.stream().map(treeNode -> treeNode.asString(lastWrites, lastReads)))
            .filter(Objects::nonNull)
            .collect(Collectors.joining(","));
    String writeResult = getAnalysisResult("W", lastWrites);
    String readResult = getAnalysisResult("R", lastReads);
    return String.format("(%s%s%s)", children, writeResult, readResult);
  }

  private String getAnalysisResult(
      String desc, AnalysisResult<PossibleTreeSet, PossibleTreeSetStore> lastWrites) {
    if (node != null) {
      PossibleTreeSet possibleTreeSet = lastWrites.getValue(node);
      if (possibleTreeSet != null) {
        return String.format(" [%s:%s]", desc, possibleTreeSet.toString());
      }
    }
    return "";
  }
}
