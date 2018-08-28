package uk.ac.cam.acr31.features.javac;

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
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.UsesTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;

public class JavacTreePathScanner extends TreePathScanner<Void, TreeNode> {

  @Override
  public Void scan(Tree tree, TreeNode treeNode) {
    if (tree == null) {
      treeNode.children.add(new TreeNode(null));
    } else {
      tree.accept(this, treeNode);
    }
    return null;
  }

  @Override
  public Void scan(Iterable<? extends Tree> nodes, TreeNode treeNode) {
    TreeNode result = new TreeNode(null);
    treeNode.children.add(result);
    if (nodes != null) {
      for (Tree node : nodes) {
        scan(node, result);
      }
    }
    return null;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitCompilationUnit(node, result);
  }

  @Override
  public Void visitPackage(PackageTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitPackage(node, result);
  }

  @Override
  public Void visitImport(ImportTree node, TreeNode treeNode) {
    TreeNode result =
        new TreeNode(node, node.getKind().name(), node.isStatic() ? "static_import" : "import");
    treeNode.children.add(result);
    return super.visitImport(node, result);
  }

  @Override
  public Void visitClass(ClassTree node, TreeNode treeNode) {
    TreeNode result =
        new TreeNode(node, node.getKind().name(), String.valueOf(node.getSimpleName()));
    treeNode.children.add(result);
    return super.visitClass(node, result);
  }

  @Override
  public Void visitMethod(MethodTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name(), String.valueOf(node.getName()));
    treeNode.children.add(result);
    return super.visitMethod(node, result);
  }

  @Override
  public Void visitVariable(VariableTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name(), String.valueOf(node.getName()));
    treeNode.children.add(result);
    return super.visitVariable(node, result);
  }

  @Override
  public Void visitEmptyStatement(EmptyStatementTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitEmptyStatement(node, result);
  }

  @Override
  public Void visitBlock(BlockTree node, TreeNode treeNode) {
    TreeNode result =
        new TreeNode(node, node.getKind().name(), node.isStatic() ? "static_block" : "block");
    treeNode.children.add(result);
    return super.visitBlock(node, result);
  }

  @Override
  public Void visitDoWhileLoop(DoWhileLoopTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitDoWhileLoop(node, result);
  }

  @Override
  public Void visitWhileLoop(WhileLoopTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitWhileLoop(node, result);
  }

  @Override
  public Void visitForLoop(ForLoopTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitForLoop(node, result);
  }

  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitEnhancedForLoop(node, result);
  }

  @Override
  public Void visitLabeledStatement(LabeledStatementTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name(), String.valueOf(node.getLabel()));
    treeNode.children.add(result);
    return super.visitLabeledStatement(node, result);
  }

  @Override
  public Void visitSwitch(SwitchTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitSwitch(node, result);
  }

  @Override
  public Void visitCase(CaseTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitCase(node, result);
  }

  @Override
  public Void visitSynchronized(SynchronizedTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitSynchronized(node, result);
  }

  @Override
  public Void visitTry(TryTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitTry(node, result);
  }

  @Override
  public Void visitCatch(CatchTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitCatch(node, result);
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitConditionalExpression(node, result);
  }

  @Override
  public Void visitIf(IfTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitIf(node, result);
  }

  @Override
  public Void visitExpressionStatement(ExpressionStatementTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitExpressionStatement(node, result);
  }

  @Override
  public Void visitBreak(BreakTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name(), String.valueOf(node.getLabel()));
    treeNode.children.add(result);
    return super.visitBreak(node, result);
  }

  @Override
  public Void visitContinue(ContinueTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name(), String.valueOf(node.getLabel()));
    treeNode.children.add(result);
    return super.visitContinue(node, result);
  }

  @Override
  public Void visitReturn(ReturnTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitReturn(node, result);
  }

  @Override
  public Void visitThrow(ThrowTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitThrow(node, result);
  }

  @Override
  public Void visitAssert(AssertTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitAssert(node, result);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitMethodInvocation(node, result);
  }

  @Override
  public Void visitNewClass(NewClassTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitNewClass(node, result);
  }

  @Override
  public Void visitNewArray(NewArrayTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitNewArray(node, result);
  }

  @Override
  public Void visitLambdaExpression(LambdaExpressionTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name(), node.getBodyKind().name());
    treeNode.children.add(result);
    return super.visitLambdaExpression(node, result);
  }

  @Override
  public Void visitParenthesized(ParenthesizedTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitParenthesized(node, result);
  }

  @Override
  public Void visitAssignment(AssignmentTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitAssignment(node, result);
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitCompoundAssignment(node, result);
  }

  @Override
  public Void visitUnary(UnaryTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitUnary(node, result);
  }

  @Override
  public Void visitBinary(BinaryTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitBinary(node, result);
  }

  @Override
  public Void visitTypeCast(TypeCastTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitTypeCast(node, result);
  }

  @Override
  public Void visitInstanceOf(InstanceOfTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitInstanceOf(node, result);
  }

  @Override
  public Void visitArrayAccess(ArrayAccessTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitArrayAccess(node, result);
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree node, TreeNode treeNode) {
    TreeNode result =
        new TreeNode(node, node.getKind().name(), String.valueOf(node.getIdentifier()));
    treeNode.children.add(result);
    return super.visitMemberSelect(node, result);
  }

  @Override
  public Void visitMemberReference(MemberReferenceTree node, TreeNode treeNode) {
    TreeNode result =
        new TreeNode(
            node,
            node.getKind().name(),
            String.valueOf(node.getMode()),
            String.valueOf(node.getName()));
    treeNode.children.add(result);
    return super.visitMemberReference(node, result);
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, TreeNode treeNode) {
    JCTree.JCIdent ident = (JCTree.JCIdent) node;
    Symbol sym = ident.sym;

    String name = String.valueOf(node.getName());
    if (sym instanceof Symbol.ClassSymbol) {
      name = ((Symbol.ClassSymbol) sym).fullname.toString();
    }
    TreeNode result = new TreeNode(node, node.getKind().name(), name);
    treeNode.children.add(result);
    return super.visitIdentifier(node, result);
  }

  @Override
  public Void visitLiteral(LiteralTree node, TreeNode treeNode) {
    String literalValue = String.valueOf(node.getValue());
    // TODO - I'm pretty sure there is a method in javac that will do this for me...
    literalValue = literalValue.replaceAll("([\"\\\\])", "\\$1");
    TreeNode result = new TreeNode(node, node.getKind().name(), "\"" + literalValue + "\"");
    treeNode.children.add(result);
    return super.visitLiteral(node, result);
  }

  @Override
  public Void visitPrimitiveType(PrimitiveTypeTree node, TreeNode treeNode) {
    TreeNode result =
        new TreeNode(node, node.getKind().name(), String.valueOf(node.getPrimitiveTypeKind()));
    treeNode.children.add(result);
    return super.visitPrimitiveType(node, result);
  }

  @Override
  public Void visitArrayType(ArrayTypeTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitArrayType(node, result);
  }

  @Override
  public Void visitParameterizedType(ParameterizedTypeTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitParameterizedType(node, result);
  }

  @Override
  public Void visitUnionType(UnionTypeTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitUnionType(node, result);
  }

  @Override
  public Void visitIntersectionType(IntersectionTypeTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitIntersectionType(node, result);
  }

  @Override
  public Void visitTypeParameter(TypeParameterTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name(), String.valueOf(node.getName()));
    treeNode.children.add(result);
    return super.visitTypeParameter(node, result);
  }

  @Override
  public Void visitWildcard(WildcardTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitWildcard(node, result);
  }

  @Override
  public Void visitModifiers(ModifiersTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    TreeNode flags = new TreeNode(node, "flags");
    node.getFlags().forEach(f -> flags.children.add(new TreeNode(node, String.valueOf(f))));
    result.children.add(flags);
    return super.visitModifiers(node, result);
  }

  @Override
  public Void visitAnnotation(AnnotationTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitAnnotation(node, result);
  }

  @Override
  public Void visitAnnotatedType(AnnotatedTypeTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitAnnotatedType(node, result);
  }

  @Override
  public Void visitModule(ModuleTree node, TreeNode treeNode) {
    TreeNode result =
        new TreeNode(node, node.getKind().name(), String.valueOf(node.getModuleType()));
    treeNode.children.add(result);
    return super.visitModule(node, result);
  }

  @Override
  public Void visitExports(ExportsTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitExports(node, result);
  }

  @Override
  public Void visitOpens(OpensTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitOpens(node, result);
  }

  @Override
  public Void visitProvides(ProvidesTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitProvides(node, result);
  }

  @Override
  public Void visitRequires(RequiresTree node, TreeNode treeNode) {
    TreeNode result =
        new TreeNode(
            node,
            node.getKind().name(),
            node.isStatic() ? "static" : "not_static",
            node.isTransitive() ? "transitive" : "not_transitive");
    treeNode.children.add(result);
    return super.visitRequires(node, result);
  }

  @Override
  public Void visitUses(UsesTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitUses(node, result);
  }

  @Override
  public Void visitOther(Tree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitOther(node, result);
  }

  @Override
  public Void visitErroneous(ErroneousTree node, TreeNode treeNode) {
    TreeNode result = new TreeNode(node, node.getKind().name());
    treeNode.children.add(result);
    return super.visitErroneous(node, result);
  }
}
