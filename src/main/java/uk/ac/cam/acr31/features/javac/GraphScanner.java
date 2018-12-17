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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.ExportsTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.OpensTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ProvidesTree;
import com.sun.source.tree.RequiresTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.UsesTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Position;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import uk.ac.cam.acr31.features.javac.graph.EdgeType;
import uk.ac.cam.acr31.features.javac.graph.FeatureGraph;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode;
import uk.ac.cam.acr31.features.javac.proto.GraphProtos.FeatureNode.NodeType;

public class GraphScanner implements TreeVisitor<Void, FeatureNode> {

  private final FeatureGraph featureGraph;
  private final ImmutableRangeMap<Integer, FeatureNode> tokens;
  private final EndPosTable endPosTable;

  GraphScanner(
      FeatureGraph featureGraph,
      ImmutableRangeMap<Integer, FeatureNode> tokens,
      EndPosTable endPosTable) {
    this.featureGraph = featureGraph;
    this.tokens = tokens;
    this.endPosTable = endPosTable;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree node, FeatureNode featureNode) {
    addNodes(
        node,
        Arrays.asList(node.getPackage(), node.getImports(), node.getTypeDecls()),
        featureNode);
    return null;
  }

  @Override
  public Void visitClass(ClassTree node, FeatureNode featureNode) {
    addNodes(
        node,
        Arrays.asList(
            node.getModifiers(),
            node.getTypeParameters(),
            node.getExtendsClause(),
            node.getImplementsClause(),
            node.getMembers()),
        featureNode);
    return null;
  }

  @Override
  public Void visitMethod(MethodTree node, FeatureNode featureNode) {
    addNodes(
        node,
        Arrays.asList(
            node.getModifiers(),
            node.getReturnType(),
            node.getTypeParameters(),
            node.getParameters(),
            node.getReceiverParameter(),
            node.getThrows(),
            node.getBody(),
            node.getDefaultValue()),
        featureNode);
    return null;
  }

  @Override
  public Void visitModifiers(ModifiersTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getAnnotations()), featureNode);
    return null;
  }

  @Override
  public Void visitTypeParameter(TypeParameterTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getBounds(), node.getAnnotations()), featureNode);
    return null;
  }

  @Override
  public Void visitVariable(VariableTree node, FeatureNode featureNode) {
    addNodes(
        node,
        Arrays.asList(
            node.getModifiers(), node.getNameExpression(), node.getType(), node.getInitializer()),
        featureNode);
    return null;
  }

  @Override
  public Void visitAnnotatedType(AnnotatedTypeTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getAnnotations(), node.getUnderlyingType()), featureNode);
    return null;
  }

  @Override
  public Void visitAnnotation(AnnotationTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getAnnotationType(), node.getArguments()), featureNode);
    return null;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, FeatureNode featureNode) {
    addNodes(
        node,
        Arrays.asList(node.getTypeArguments(), node.getMethodSelect(), node.getArguments()),
        featureNode);
    return null;
  }

  @Override
  public Void visitAssert(AssertTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getCondition(), node.getDetail()), featureNode);
    return null;
  }

  @Override
  public Void visitAssignment(AssignmentTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getVariable(), node.getExpression()), featureNode);
    return null;
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getVariable(), node.getExpression()), featureNode);
    return null;
  }

  @Override
  public Void visitBinary(BinaryTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getLeftOperand(), node.getRightOperand()), featureNode);
    return null;
  }

  @Override
  public Void visitBlock(BlockTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getStatements()), featureNode);
    return null;
  }

  @Override
  public Void visitBreak(BreakTree node, FeatureNode featureNode) {
    addNodes(node, Collections.emptyList(), featureNode);
    return null;
  }

  @Override
  public Void visitCase(CaseTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getExpression(), node.getStatements()), featureNode);
    return null;
  }

  @Override
  public Void visitCatch(CatchTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getParameter(), node.getBlock()), featureNode);
    return null;
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree node, FeatureNode featureNode) {
    addNodes(
        node,
        Arrays.asList(node.getCondition(), node.getTrueExpression(), node.getFalseExpression()),
        featureNode);
    return null;
  }

  @Override
  public Void visitContinue(ContinueTree node, FeatureNode featureNode) {
    addNodes(node, Collections.emptyList(), featureNode);
    return null;
  }

  @Override
  public Void visitDoWhileLoop(DoWhileLoopTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getCondition(), node.getStatement()), featureNode);
    return null;
  }

  @Override
  public Void visitErroneous(ErroneousTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getErrorTrees()), featureNode);
    return null;
  }

  @Override
  public Void visitExpressionStatement(ExpressionStatementTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getExpression()), featureNode);
    return null;
  }

  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree node, FeatureNode featureNode) {
    addNodes(
        node,
        Arrays.asList(node.getVariable(), node.getExpression(), node.getStatement()),
        featureNode);
    return null;
  }

  @Override
  public Void visitForLoop(ForLoopTree node, FeatureNode featureNode) {
    addNodes(
        node,
        Arrays.asList(
            node.getInitializer(), node.getCondition(), node.getUpdate(), node.getStatement()),
        featureNode);
    return null;
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, FeatureNode featureNode) {
    addNodes(node, Collections.emptyList(), featureNode);
    return null;
  }

  @Override
  public Void visitIf(IfTree node, FeatureNode featureNode) {
    addNodes(
        node,
        Arrays.asList(node.getCondition(), node.getThenStatement(), node.getElseStatement()),
        featureNode);
    return null;
  }

  @Override
  public Void visitImport(ImportTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getQualifiedIdentifier()), featureNode);
    return null;
  }

  @Override
  public Void visitArrayAccess(ArrayAccessTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getExpression(), node.getIndex()), featureNode);
    return null;
  }

  @Override
  public Void visitLabeledStatement(LabeledStatementTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getStatement()), featureNode);
    return null;
  }

  @Override
  public Void visitLiteral(LiteralTree node, FeatureNode featureNode) {
    addNodes(node, Collections.emptyList(), featureNode);
    return null;
  }

  @Override
  public Void visitNewArray(NewArrayTree node, FeatureNode featureNode) {
    addNodes(
        node,
        Arrays.asList(
            node.getType(),
            node.getInitializers(),
            node.getAnnotations(),
            node.getDimAnnotations()),
        featureNode);
    return null;
  }

  @Override
  public Void visitNewClass(NewClassTree node, FeatureNode featureNode) {
    addNodes(
        node,
        Arrays.asList(
            node.getEnclosingExpression(),
            node.getTypeArguments(),
            node.getIdentifier(),
            node.getArguments(),
            node.getClassBody()),
        featureNode);
    return null;
  }

  @Override
  public Void visitLambdaExpression(LambdaExpressionTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getParameters(), node.getBody()), featureNode);
    return null;
  }

  @Override
  public Void visitPackage(PackageTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getAnnotations(), node.getPackageName()), featureNode);
    return null;
  }

  @Override
  public Void visitParenthesized(ParenthesizedTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getExpression()), featureNode);
    return null;
  }

  @Override
  public Void visitReturn(ReturnTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getExpression()), featureNode);
    return null;
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getExpression()), featureNode);
    return null;
  }

  @Override
  public Void visitMemberReference(MemberReferenceTree node, FeatureNode featureNode) {
    addNodes(
        node, Arrays.asList(node.getQualifierExpression(), node.getTypeArguments()), featureNode);
    return null;
  }

  @Override
  public Void visitEmptyStatement(EmptyStatementTree node, FeatureNode featureNode) {
    addNodes(node, Collections.emptyList(), featureNode);
    return null;
  }

  @Override
  public Void visitSwitch(SwitchTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getExpression(), node.getCases()), featureNode);
    return null;
  }

  @Override
  public Void visitSynchronized(SynchronizedTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getExpression(), node.getBlock()), featureNode);
    return null;
  }

  @Override
  public Void visitThrow(ThrowTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getExpression()), featureNode);
    return null;
  }

  @Override
  public Void visitTry(TryTree node, FeatureNode featureNode) {
    addNodes(
        node,
        Arrays.asList(
            node.getBlock(), node.getCatches(), node.getFinallyBlock(), node.getResources()),
        featureNode);
    return null;
  }

  @Override
  public Void visitParameterizedType(ParameterizedTypeTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getType(), node.getTypeArguments()), featureNode);
    return null;
  }

  @Override
  public Void visitUnionType(UnionTypeTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getTypeAlternatives()), featureNode);
    return null;
  }

  @Override
  public Void visitIntersectionType(IntersectionTypeTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getBounds()), featureNode);
    return null;
  }

  @Override
  public Void visitArrayType(ArrayTypeTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getType()), featureNode);
    return null;
  }

  @Override
  public Void visitTypeCast(TypeCastTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getType(), node.getExpression()), featureNode);
    return null;
  }

  @Override
  public Void visitPrimitiveType(PrimitiveTypeTree node, FeatureNode featureNode) {
    addNodes(node, Collections.emptyList(), featureNode);
    return null;
  }

  @Override
  public Void visitInstanceOf(InstanceOfTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getExpression(), node.getType()), featureNode);
    return null;
  }

  @Override
  public Void visitUnary(UnaryTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getExpression()), featureNode);
    return null;
  }

  @Override
  public Void visitWhileLoop(WhileLoopTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getCondition(), node.getStatement()), featureNode);
    return null;
  }

  @Override
  public Void visitWildcard(WildcardTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getBound()), featureNode);
    return null;
  }

  @Override
  public Void visitModule(ModuleTree node, FeatureNode featureNode) {
    addNodes(
        node,
        Arrays.asList(node.getAnnotations(), node.getName(), node.getDirectives()),
        featureNode);
    return null;
  }

  @Override
  public Void visitExports(ExportsTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getPackageName(), node.getModuleNames()), featureNode);
    return null;
  }

  @Override
  public Void visitOpens(OpensTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getPackageName(), node.getModuleNames()), featureNode);
    return null;
  }

  @Override
  public Void visitProvides(ProvidesTree node, FeatureNode featureNode) {
    addNodes(
        node, Arrays.asList(node.getServiceName(), node.getImplementationNames()), featureNode);
    return null;
  }

  @Override
  public Void visitRequires(RequiresTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getModuleName()), featureNode);
    return null;
  }

  @Override
  public Void visitUses(UsesTree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node.getServiceName()), featureNode);
    return null;
  }

  @Override
  public Void visitOther(Tree node, FeatureNode featureNode) {
    addNodes(node, Arrays.asList(node), featureNode);
    return null;
  }

  static boolean isSynthetic(JCTree child) {
    if (child instanceof JCTree.JCMethodDecl) {
      Symbol sym = ((JCTree.JCMethodDecl) child).sym;
      if (sym != null) {
        boolean synthetic = (sym.flags() & Flags.SYNTHETIC) != 0;
        boolean generatedConstructor = (sym.flags() & Flags.GENERATEDCONSTR) != 0;
        return synthetic || generatedConstructor;
      }
    }
    return false;
  }

  static Optional<Integer> getStartPosition(JCTree child) {
    if (child == null || isSynthetic(child)) {
      return Optional.empty();
    }
    int pos = child.getStartPosition();
    if (pos == Position.NOPOS) {
      return Optional.empty();
    }
    return Optional.of(pos);
  }

  Optional<Integer> getEndPosition(JCTree child) {
    if (child == null || isSynthetic(child)) {
      return Optional.empty();
    }
    int pos = child.getEndPosition(endPosTable);
    if (pos == Position.NOPOS) {
      return Optional.empty();
    }
    return Optional.of(pos);
  }

  private void addNodes(Tree astNode, List<Object> childNodes, FeatureNode graphParent) {
    addNodesInner(
        (JCTree) astNode,
        childNodes
            .stream()
            .map(
                n -> {
                  if (n instanceof JCTree) {
                    return (JCTree) n;
                  }
                  if (n instanceof List) {
                    return new PseudoTreeForList((List) n, featureGraph);
                  }
                  return null;
                })
            .collect(Collectors.toList()),
        graphParent);
  }

  private void addNodesInner(JCTree astNode, List<JCTree> childNodes, FeatureNode graphParent) {
    if (childNodes.isEmpty()) {
      Optional<Integer> nodeStartPos = getStartPosition(astNode);
      Optional<Integer> nodeEndPos = getEndPosition(astNode);
      if (!nodeStartPos.isPresent() || !nodeEndPos.isPresent()) {
        return;
      }
    }
    FeatureNode myNode =
        featureGraph.createFeatureNode(NodeType.AST_ELEMENT, astNode.getKind().toString(), astNode);
    featureGraph.putEdgeValue(graphParent, myNode, EdgeType.AST_CHILD);
    Optional<Integer> lastEndPos = getStartPosition(astNode);
    for (JCTree child : childNodes) {
      Optional<Integer> startPosition = getStartPosition(child);
      if (lastEndPos.isPresent() && startPosition.isPresent()) {
        addTokens(myNode, lastEndPos.get(), startPosition.get());
      }
      if (child != null) {
        child.accept(this, myNode);
      }
      Optional<Integer> endPosition = getEndPosition(child);
      if (endPosition.isPresent()) {
        lastEndPos = endPosition;
      }
    }
    Optional<Integer> nodeEndPos = getEndPosition(astNode);
    if (lastEndPos.isPresent() && nodeEndPos.isPresent()) {
      addTokens(myNode, lastEndPos.get(), nodeEndPos.get());
    }
  }

  void addTokens(FeatureNode node, int lower, int upper) {
    if (lower >= upper) {
      return;
    }
    Range<Integer> range = Range.closedOpen(lower, upper);
    ImmutableCollection<FeatureNode> tokens =
        this.tokens.subRangeMap(range).asMapOfRanges().values();
    for (FeatureNode tokenNode : tokens) {
      featureGraph.putEdgeValue(node, tokenNode, EdgeType.ASSOCIATED_TOKEN);
    }
  }
}
