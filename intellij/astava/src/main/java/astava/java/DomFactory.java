package astava.java;

import astava.tree.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DomFactory {
    public static ClassDom classDeclaration(int modifiers, String name, String superName, List<String> interfaces, List<FieldDom> fields, List<MethodDom> methods) {
        return new ClassDom() {
            @Override
            public int getModifiers() {
                return modifiers;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getSuperName() {
                return superName;
            }

            @Override
            public List<String> getInterfaces() {
                return interfaces;
            }

            @Override
            public List<FieldDom> getFields() {
                return fields;
            }

            @Override
            public List<MethodDom> getMethods() {
                return methods;
            }
        };
    }

    public static FieldDom fieldDeclaration(int modifiers, String name, String typeName) {
        return new CustomFieldDom() {
            @Override
            public int getModifiers() {
                return modifiers;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getTypeName() {
                return typeName;
            }
        };
    }

    public static MethodDom methodDeclaration(int modifiers, String name, List<ParameterInfo> parameters, String returnType, StatementDom body) {
        return new MethodDom() {
            @Override
            public int getModifier() {
                return modifiers;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public List<ParameterInfo> getParameterTypes() {
                return parameters;
            }

            @Override
            public String getReturnTypeName() {
                return returnType;
            }

            @Override
            public StatementDom getBody() {
                return body;
            }
        };
    }

    public static StatementDom ret() {
        return new AbstractStatementDom() {
            @Override
            public void accept(StatementDomVisitor visitor) {
                visitor.visitReturn();
            }

            @Override
            protected StatementDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultStatementDomVisitor() {
                    @Override
                    public void visitReturn() {
                        r.accept(true);
                    }
                };
            }

            @Override
            public String toString() {
                return "return;";
            }
        };
    }

    public static StatementDom ret(ExpressionDom expression) {
        return new AbstractStatementDom() {
            @Override
            public void accept(StatementDomVisitor visitor) {
                visitor.visitReturnValue(expression);
            }

            @Override
            protected StatementDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultStatementDomVisitor() {
                    @Override
                    public void visitReturnValue(ExpressionDom otherExpression) {
                        r.accept(expression.equals(otherExpression, context));
                    }
                };
            }

            @Override
            public String toString() {
                return "return " + expression;
            }
        };
    }

    public static ExpressionDom literal(boolean value) {
        return new AbstractExpressionDom() {
            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitBooleanLiteral(value);
            }

            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitBooleanLiteral(boolean otherValue) {
                        r.accept(value == otherValue);
                    }
                };
            }

            @Override
            public String toString() {
                return Boolean.toString(value);
            }
        };
    }

    public static ExpressionDom literal(byte value) {
        return v -> v.visitByteLiteral(value);
    }

    public static ExpressionDom literal(short value) {
        return v -> v.visitShortLiteral(value);
    }

    public static ExpressionDom literal(int value) {
        return new AbstractExpressionDom() {
            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitIntLiteral(value);
            }

            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitIntLiteral(int otherValue) {
                        r.accept(value == otherValue);
                    }
                };
            }

            @Override
            public String toString() {
                return "" + value;
            }
        };
    }

    public static ExpressionDom literal(long value) {
        return v -> v.visitLongLiteral(value);
    }

    public static ExpressionDom literal(float value) {
        return v -> v.visitFloatLiteral(value);
    }

    public static ExpressionDom literal(double value) {
        return v -> v.visitDoubleLiteral(value);
    }

    public static ExpressionDom literal(char value) {
        return v -> v.visitCharLiteral(value);
    }

    public static ExpressionDom literal(String value) {
        return new AbstractExpressionDom() {
            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitStringLiteral(String otherValue) {
                        r.accept(value.equals(otherValue));
                    }
                };
            }

            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitStringLiteral(value);
            }

            @Override
            public String toString() {
                return "\"" + value + "\"";
            }
        };
    }

    public static ExpressionDom nil() {
        return new AbstractExpressionDom() {
            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitNull();
            }

            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitNull() {
                        r.accept(true);
                    }
                };
            };

            @Override
            public String toString() {
                return "null";
            }
        };
    }

    public static ExpressionDom add(ExpressionDom lhs, ExpressionDom rhs) {
        return arithmetic(lhs, rhs, ArithmeticOperator.ADD);
    }

    public static ExpressionDom rem(ExpressionDom lhs, ExpressionDom rhs) {
        return arithmetic(lhs, rhs, ArithmeticOperator.REM);
    }

    public static ExpressionDom sub(ExpressionDom lhs, ExpressionDom rhs) {
        return arithmetic(lhs, rhs, ArithmeticOperator.SUB);
    }

    public static ExpressionDom mul(ExpressionDom lhs, ExpressionDom rhs) {
        return arithmetic(lhs, rhs, ArithmeticOperator.MUL);
    }

    public static ExpressionDom div(ExpressionDom lhs, ExpressionDom rhs) {
        return arithmetic(lhs, rhs, ArithmeticOperator.DIV);
    }

    public static ExpressionDom arithmetic(ExpressionDom lhs, ExpressionDom rhs, int operator) {
        return new AbstractExpressionDom() {
            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitArithmetic(operator, lhs, rhs);
            }

            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitArithmetic(int otherOperator, ExpressionDom otherLhs, ExpressionDom otherRhs) {
                        r.accept(
                            operator == otherOperator &&
                            lhs.equals(otherLhs, context) &&
                            rhs.equals(otherRhs, context)
                        );
                    }
                };
            }

            private String[] operatorsToString = new String[]{"+", "-", "*", "/", "%"};

            @Override
            public String toString() {
                return lhs + " " + operatorsToString[operator] + " " + rhs;
            }
        };
    }

    public static ExpressionDom shl(ExpressionDom lhs, ExpressionDom rhs) {
        return shift(lhs, rhs, ShiftOperator.SHL);
    }

    public static ExpressionDom shr(ExpressionDom lhs, ExpressionDom rhs) {
        return shift(lhs, rhs, ShiftOperator.SHR);
    }

    public static ExpressionDom ushr(ExpressionDom lhs, ExpressionDom rhs) {
        return shift(lhs, rhs, ShiftOperator.USHR);
    }

    public static ExpressionDom shift(ExpressionDom lhs, ExpressionDom rhs, int operator) {
        return new AbstractExpressionDom() {
            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitShift(int otherOperator, ExpressionDom otherLhs, ExpressionDom otherRhs) {
                        r.accept(
                            operator == otherOperator &&
                            lhs.equals(otherLhs, context) &&
                            rhs.equals(otherRhs, context)
                        );
                    }
                };
            }

            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitShift(operator, lhs, rhs);
            }

            private String operatorToString() {
                switch (operator) {
                    case ShiftOperator.SHL:
                        return "<<";
                    case ShiftOperator.SHR:
                        return ">>";
                    case ShiftOperator.USHR:
                        return ">>>";
                }

                return null;
            }

            @Override
            public String toString() {
                return lhs + " " + operatorToString() + " " + rhs;
            }
        };
    }

    public static ExpressionDom band(ExpressionDom lhs, ExpressionDom rhs) {
        return bitwise(lhs, rhs, BitwiseOperator.AND);
    }

    public static ExpressionDom bor(ExpressionDom lhs, ExpressionDom rhs) {
        return bitwise(lhs, rhs, BitwiseOperator.OR);
    }

    public static ExpressionDom bxor(ExpressionDom lhs, ExpressionDom rhs) {
        return bitwise(lhs, rhs, BitwiseOperator.XOR);
    }

    public static ExpressionDom bitwise(ExpressionDom lhs, ExpressionDom rhs, int operator) {
        return new AbstractExpressionDom() {
            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitBitwise(int otherOperator, ExpressionDom otherLhs, ExpressionDom otherRhs) {
                        r.accept(
                            operator == otherOperator &&
                            lhs.equals(otherLhs) &&
                            rhs.equals(otherRhs)
                        );
                    }
                };
            }

            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitBitwise(operator, lhs, rhs);
            }

            private String operatorToString() {
                switch (operator) {
                    case BitwiseOperator.AND:
                        return "&";
                    case BitwiseOperator.OR:
                        return "|";
                    case BitwiseOperator.XOR:
                        return "^";
                }

                return null;
            }

            @Override
            public String toString() {
                return lhs + " " + operatorToString() + " " + rhs;
            }
        };
    }

    public static ExpressionDom and(ExpressionDom lhs, ExpressionDom rhs) {
        return logical(lhs, rhs, LogicalOperator.AND);
    }

    public static ExpressionDom or(ExpressionDom lhs, ExpressionDom rhs) {
        return logical(lhs, rhs, LogicalOperator.OR);
    }

    public static ExpressionDom logical(ExpressionDom lhs, ExpressionDom rhs, int operator) {
        return v -> v.visitLogical(operator, lhs, rhs);
    }

    public static ExpressionDom lt(ExpressionDom lhs, ExpressionDom rhs) {
        return compare(lhs, rhs, RelationalOperator.LT);
    }

    public static ExpressionDom le(ExpressionDom lhs, ExpressionDom rhs) {
        return compare(lhs, rhs, RelationalOperator.LE);
    }

    public static ExpressionDom gt(ExpressionDom lhs, ExpressionDom rhs) {
        return compare(lhs, rhs, RelationalOperator.GT);
    }

    public static ExpressionDom ge(ExpressionDom lhs, ExpressionDom rhs) {
        return compare(lhs, rhs, RelationalOperator.GE);
    }

    public static ExpressionDom eq(ExpressionDom lhs, ExpressionDom rhs) {
        return compare(lhs, rhs, RelationalOperator.EQ);
    }

    public static ExpressionDom ne(ExpressionDom lhs, ExpressionDom rhs) {
        return compare(lhs, rhs, RelationalOperator.NE);
    }

    public static ExpressionDom compare(ExpressionDom lhs, ExpressionDom rhs, int operator) {
        return new AbstractExpressionDom() {
            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitCompare(operator, lhs, rhs);
            }

            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitCompare(int otherOperator, ExpressionDom otherLhs, ExpressionDom otherRhs) {
                        r.accept(
                            operator == otherOperator &&
                            lhs.equals(otherLhs, context) &&
                            rhs.equals(otherRhs, context)
                        );
                    }
                };
            }

            private String operatorToString() {
                switch (operator) {
                    case RelationalOperator.EQ:
                        return "==";
                    case RelationalOperator.NE:
                        return "!=";
                    case RelationalOperator.LT:
                        return "<";
                    case RelationalOperator.LE:
                        return "<=";
                    case RelationalOperator.GT:
                        return ">";
                    case RelationalOperator.GE:
                        return ">=";
                }

                return "<NA>";
            }

            @Override
            public String toString() {
                return lhs + " " + operatorToString() + " " + rhs;
            }
        };
    }

    public static ExpressionDom objectEquality(ExpressionDom lhs, ExpressionDom rhs, int operator) {
        return new AbstractExpressionDom() {
            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitObjectEquality(operator, lhs, rhs);
            }

            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitObjectEquality(int otherOperator, ExpressionDom otherLhs, ExpressionDom otherRhs) {
                        r.accept(
                            operator == otherOperator &&
                            lhs.equals(otherLhs, context) &&
                            rhs.equals(otherRhs, context)
                        );
                    }
                };
            }

            private String operatorToString() {
                switch (operator) {
                    case RelationalOperator.EQ:
                        return "==";
                    case RelationalOperator.NE:
                        return "!=";
                }

                return "<NA>";
            }

            @Override
            public String toString() {
                return lhs + " " + operatorToString() + " " + rhs;
            }
        };
    }

    public static StatementDom declareVar(String type, String name) {
        return new AbstractStatementDom() {
            @Override
            public void accept(StatementDomVisitor visitor) {
                visitor.visitVariableDeclaration(type, name);
            }

            @Override
            protected StatementDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultStatementDomVisitor() {
                    @Override
                    public void visitVariableDeclaration(String otherType, String otherName) {
                        r.accept(type.equals(otherType) && name.equals(otherName));
                    }
                };
            }

            @Override
            public String toString() {
                return type + " " + name;
            }
        };
    }

    // Assign variable as statement (not expression)
    public static StatementDom assignVar(String name, ExpressionDom expression) {
        return new AbstractStatementDom() {
            @Override
            public void accept(StatementDomVisitor visitor) {
                visitor.visitVariableAssignment(name, expression);
            }

            @Override
            protected StatementDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultStatementDomVisitor() {
                    @Override
                    public void visitVariableAssignment(String otherName, ExpressionDom otherValue) {
                        r.accept(name.equals(otherName) && expression.equals(otherValue, context));
                    }
                };
            }

            @Override
            public String toString() {
                return name + " = " + expression;
            }
        };
    }

    public static StatementDom assignField(ExpressionDom target, String name, String type, ExpressionDom expression) {
        if(expression == null)
            new String();
        return new AbstractStatementDom() {
            @Override
            public void accept(StatementDomVisitor visitor) {
                visitor.visitFieldAssignment(target, name, type, expression);
            }

            @Override
            protected StatementDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultStatementDomVisitor() {
                    @Override
                    public void visitFieldAssignment(ExpressionDom otherTarget, String otherName, String otherType, ExpressionDom otherValue) {
                        r.accept(
                            target.equals(otherTarget, context) &&
                            name.equals(otherName) &&
                            type.equals(otherType) &&
                            expression.equals(otherValue, context)
                        );
                    }
                };
            }

            @Override
            public String toString() {
                return target + "." + name + " = " + expression;
            }
        };
    }

    public static StatementDom assignStaticField(String targetTypeName, String name, String type, ExpressionDom expression) {
        return new AbstractStatementDom() {
            @Override
            public void accept(StatementDomVisitor visitor) {
                visitor.visitStaticFieldAssignment(targetTypeName, name, type, expression);
            }

            @Override
            protected StatementDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultStatementDomVisitor() {
                    @Override
                    public void visitStaticFieldAssignment(String otherTypeName, String otherName, String otherType, ExpressionDom otherValue) {
                        r.accept(
                            targetTypeName.equals(otherTypeName) &&
                            name.equals(otherName) &&
                            type.equals(otherType) &&
                            expression.equals(otherValue, context)
                        );
                    }
                };
            }

            @Override
            public String toString() {
                return Descriptor.getName(targetTypeName) + "." + name + " = " + expression;
            }
        };
    }

    public static ExpressionDom accessVar(String name) {
        return new AbstractExpressionDom() {
            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitVariableAccess(name);
            }

            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitVariableAccess(String otherName) {
                        r.accept(name.equals(otherName));
                    }
                };
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    public static ExpressionDom accessField(ExpressionDom target, String name, String fieldTypeName) {
        return new AbstractExpressionDom() {
            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitFieldAccess(target, name, fieldTypeName);
            }

            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitFieldAccess(ExpressionDom otherTarget, String otherName, String otherFieldTypeName) {
                        r.accept(
                            target.equals(otherTarget, context) &&
                            name.equals(otherName) &&
                            fieldTypeName.equals(otherFieldTypeName)
                        );
                    }
                };
            }

            @Override
            public String toString() {
                return target + "." + name;
            }
        };
    }

    public static ExpressionDom accessStaticField(String targetTypeName, String name, String typeName) {
        return new AbstractExpressionDom() {
            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitStaticFieldAccess(String otherTypeName, String otherName, String otherFieldTypeName) {
                        r.accept(
                            targetTypeName.equals(otherTypeName) &&
                            name.equals(otherName) &&
                            typeName.equals(otherFieldTypeName)
                        );
                    }
                };
            }

            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitStaticFieldAccess(targetTypeName, name, typeName);
            }

            @Override
            public String toString() {
                return Descriptor.getName(targetTypeName) + "." + name;
            }
        };
    }

    public static ExpressionDom self() {
        return new AbstractExpressionDom() {
            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitThis();
            }

            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitThis() {
                        r.accept(true);
                    }
                };
            }

            @Override
            public String toString() {
                return "this";
            }
        };
    }

    public static StatementDom block(StatementDom... statements) {
        return block(Arrays.asList(statements));
    }

    public static StatementDom block(List<StatementDom> statements) {
        return new AbstractStatementDom() {
            @Override
            public void accept(StatementDomVisitor visitor) {
                visitor.visitBlock(statements);
            }

            @Override
            public List<? extends Dom> getChildren() {
                return statements;
            }

            @Override
            public Dom setChildren(List<? extends Dom> children) {
                return block((List<StatementDom>)children);
            }

            @Override
            protected StatementDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultStatementDomVisitor() {
                    @Override
                    public void visitBlock(List<StatementDom> otherStatements) {
                        List<StatementDom> s = statements;
                        CodeDomComparison ctx = context;
                        r.accept(
                            allStatementsEquals(s, otherStatements, ctx)
                        );
                    }
                };
            }

            @Override
            public String toString() {
                return statements.stream().map(x -> x.toString()).collect(Collectors.joining("\n"));
            }
        };
    }

    private static boolean allStatementsEquals(List<StatementDom> l1, List<StatementDom> l2, CodeDomComparison context) {
        return allEquals(l1, l2, (x, y) -> x.equals(y, context));
    }

    private static boolean allExpressionsEquals(List<ExpressionDom> l1, List<ExpressionDom> l2, CodeDomComparison context) {
        return allEquals(l1, l2, (x, y) -> x.equals(y, context));
    }

    private static <T> boolean allEquals(List<T> l1, List<T> l2, BiPredicate<T, T> comparer) {
        return l1.size() == l2.size() &&
            IntStream.range(0, l1.size()).allMatch(i ->
                comparer.test(l1.get(i), l2.get(i)));
    }

    // At most one expression dom
    public static ExpressionDom blockExpr(List<CodeDom> codeList) {
        return v -> v.visitBlock(codeList);
    }

    public static ExpressionDom top(ExpressionDom expression, BiFunction<ExpressionDom, ExpressionDom, ExpressionDom> usage) {
        return v -> v.visitTop(expression, usage);
    }

    public static StatementDom intIncVar(String name, int amount) {
        return new AbstractStatementDom() {
            @Override
            protected StatementDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultStatementDomVisitor() {
                    @Override
                    public void visitIncrement(String otherName, int otherAmount) {
                        r.accept(
                            name.equals(otherName) &&
                            amount == otherAmount
                        );
                    }
                };
            }

            @Override
            public void accept(StatementDomVisitor visitor) {
                visitor.visitIncrement(name, amount);
            }

            @Override
            public String toString() {
                String operatorOperand;

                if(amount > 0)
                    operatorOperand = amount == 1 ? "++" : " += " + amount;
                else
                    operatorOperand = amount == -1 ? "--" : " -= " + amount;

                return name + operatorOperand;
            }
        };
    }

    public static ExpressionDom not(ExpressionDom expression) {
        return new AbstractExpressionDom() {
            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitNot(ExpressionDom otherExpression) {
                        r.accept(expression.equals(otherExpression));
                    }
                };
            }

            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitNot(expression);
            }

            @Override
            public String toString() {
                return "!" + expression;
            }
        };
    }

    public static StatementDom ifJump(ExpressionDom condition, Object label) {
        return new AbstractStatementDom() {
            @Override
            protected StatementDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultStatementDomVisitor() {
                    @Override
                    public void visitIfJump(ExpressionDom otherCondition, Object otherLabel) {
                        r.accept(
                            condition.equals(otherCondition, context) &&
                            context.isSameLabel(label, otherLabel)
                        );
                    }
                };
            }

            @Override
            public void accept(StatementDomVisitor visitor) {
                visitor.visitIfJump(condition, label);
            }

            @Override
            public String toString() {
                return "if(" + condition + ") goTo " + label;
            }
        };
    }

    // If-else-statement (not expression)
    public static StatementDom ifElse(ExpressionDom condition, StatementDom ifTrue, StatementDom ifFalse) {
        return new AbstractStatementDom() {
            @Override
            public void accept(StatementDomVisitor visitor) {
                visitor.visitIfElse(condition, ifTrue, ifFalse);
            }

            @Override
            protected StatementDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultStatementDomVisitor() {
                    @Override
                    public void visitIfElse(ExpressionDom otherCondition, StatementDom otherIfTrue, StatementDom otherIfFalse) {
                        r.accept(
                            condition.equals(otherCondition, context) &&
                            ifTrue.equals(otherIfTrue, context) &&
                            ifFalse.equals(otherIfFalse, context)
                        );
                    }
                };
            }

            @Override
            public List<? extends Dom> getChildren() {
                return Arrays.asList(condition, ifTrue, ifFalse);
            }

            @Override
            public Dom setChildren(List<? extends Dom> children) {
                return ifElse((ExpressionDom)children.get(0), (StatementDom)children.get(1), (StatementDom)children.get(2));
            }

            @Override
            public String toString() {
                return "if(" + condition + ") " + ifTrue + " else " + ifFalse;
            }
        };
    }

    // If-else-expression (not statement)
    public static ExpressionDom ifElseExpr(ExpressionDom condition, ExpressionDom ifTrue, ExpressionDom ifFalse) {
        return new AbstractExpressionDom() {
            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitIfElse(condition, ifTrue, ifFalse);
            }

            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitIfElse(ExpressionDom otherCondition, ExpressionDom otherIfTrue, ExpressionDom otherIfFalse) {
                        r.accept(
                            condition.equals(otherCondition, context) &&
                            ifTrue.equals(otherIfTrue, context) &&
                            ifFalse.equals(otherIfFalse, context)
                        );
                    }
                };
            }

            @Override
            public String toString() {
                return condition + " ? " + ifTrue + " : " + ifFalse;
            }
        };
    }

    public static StatementDom breakOption() {
        return v -> v.visitBreakCase();
    }

    public static ExpressionDom instanceOf(ExpressionDom expression, String type) {
        return new AbstractExpressionDom() {
            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitInstanceOf(ExpressionDom otherExpression, String otherType) {
                        r.accept(
                            expression.equals(otherExpression, context) &&
                            type.equals(otherType)
                        );
                    }
                };
            }

            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitInstanceOf(expression, type);
            }

            @Override
            public String toString() {
                return expression + " instanceof " + Descriptor.getName(type);
            }
        };
    }

    public static StatementDom invokeInterface(String type, String name, String methodDescriptor, ExpressionDom target, List<ExpressionDom> arguments) {
        return invoke(Invocation.INTERFACE, type, name, methodDescriptor, target, arguments);
    }

    public static StatementDom invokeStatic(String type, String name, String methodDescriptor, List<ExpressionDom> arguments) {
        return invoke(Invocation.STATIC, type, name, methodDescriptor, null, arguments);
    }

    public static StatementDom invokeVirtual(String type, String name, String methodDescriptor, ExpressionDom target, List<ExpressionDom> arguments) {
        return invoke(Invocation.VIRTUAL, type, name, methodDescriptor, target, arguments);
    }

    public static StatementDom invokeSpecial(String type, String name, String methodDescriptor, ExpressionDom target, List<ExpressionDom> arguments) {
        return invoke(Invocation.SPECIAL, type, name, methodDescriptor, target, arguments);
    }

    public static StatementDom invoke(int invocation, String type, String name, String methodDescriptor, ExpressionDom target /*null for static*/, List<ExpressionDom> arguments) {
        return new AbstractStatementDom() {
            @Override
            protected StatementDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultStatementDomVisitor() {
                    @Override
                    public void visitInvocation(int otherInvocation, ExpressionDom otherTarget, String otherType, String otherName, String otherDescriptor, List<ExpressionDom> otherArguments) {
                        r.accept(
                            invocation == otherInvocation &&
                            target.equals(otherTarget, context) &&
                            type.equals(otherType) &&
                            methodDescriptor.equals(otherDescriptor) &&
                            allExpressionsEquals(arguments, otherArguments, context)
                        );
                    }
                };
            }

            @Override
            public void accept(StatementDomVisitor visitor) {
                visitor.visitInvocation(invocation, target, type, name, methodDescriptor, arguments);
            }

            @Override
            public String toString() {
                String targetStr = invocation == Invocation.STATIC ? Descriptor.getName(type) : target.toString();

                return targetStr + "." + name +
                    "(" +
                    arguments.stream().map(x -> x.toString()).collect(Collectors.joining(", "))
                    + ")";
            }
        };
    }

    public static ExpressionDom invokeStaticExpr(String type, String name, String methodDescriptor, List<ExpressionDom> arguments) {
        return invokeExpr(Invocation.STATIC, type, name, methodDescriptor, null, arguments);
    }

    public static ExpressionDom invokeVirtualExpr(String type, String name, String methodDescriptor, ExpressionDom target, List<ExpressionDom> arguments) {
        return invokeExpr(Invocation.VIRTUAL, type, name, methodDescriptor, target, arguments);
    }

    public static ExpressionDom invokeSpecialExpr(String type, String name, String methodDescriptor, ExpressionDom target, List<ExpressionDom> arguments) {
        return invokeExpr(Invocation.SPECIAL, type, name, methodDescriptor, target, arguments);
    }

    public static ExpressionDom invokeExpr(int invocation, String type, String name, String methodDescriptor, ExpressionDom target /*null for static*/, List<ExpressionDom> arguments) {
        return new AbstractExpressionDom() {
            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitInvocation(int otherInvocation, ExpressionDom otherTarget, String otherType, String otherName, String otherDescriptor, List<ExpressionDom> otherArguments) {
                        r.accept(
                            invocation == otherInvocation &&
                            target.equals(otherTarget, context) &&
                            type.equals(otherType) &&
                            methodDescriptor.equals(otherDescriptor) &&
                            allExpressionsEquals(arguments, otherArguments, context)
                        );
                    }
                };
            }

            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitInvocation(invocation, target, type, name, methodDescriptor, arguments);
            }

            @Override
            public String toString() {
                String targetStr = invocation == Invocation.STATIC ? Descriptor.getName(type) : target.toString();

                return targetStr + "." + name +
                    "(" +
                    arguments.stream().map(x -> x.toString()).collect(Collectors.joining(", "))
                    + ")";
            }
        };
    }

    public static StatementDom newInstance(String type, List<String> parameterTypes, List<ExpressionDom> arguments) {
        return new AbstractStatementDom() {
            @Override
            protected StatementDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultStatementDomVisitor() {
                    @Override
                    public void visitNewInstance(String otherType, List<String> otherParameterTypes, List<ExpressionDom> otherArguments) {
                        r.accept(
                            type.equals(otherType) &&
                            parameterTypes.equals(otherParameterTypes) &&
                            allExpressionsEquals(arguments, otherArguments, context)
                        );
                    }
                };
            }

            @Override
            public void accept(StatementDomVisitor visitor) {
                visitor.visitNewInstance(type, parameterTypes, arguments);
            }

            @Override
            public String toString() {
                return "new " + Descriptor.getName(type) +
                    "(" +
                    arguments.stream().map(x -> x.toString()).collect(Collectors.joining(", "))
                    + ")";
            }
        };
    }

    public static ExpressionDom newInstanceExpr(String type, List<String> parameterTypes, List<ExpressionDom> arguments) {
        return new AbstractExpressionDom() {
            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitNewInstance(String otherType, List<String> otherParameterTypes, List<ExpressionDom> otherArguments) {
                        r.accept(
                            type.equals(otherType) &&
                            parameterTypes.equals(otherParameterTypes) &&
                            allExpressionsEquals(arguments, otherArguments, context)
                        );
                    }
                };
            }

            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitNewInstance(type, parameterTypes, arguments);
            }

            @Override
            public String toString() {
                return "new " + Descriptor.getName(type) +
                    "(" +
                    arguments.stream().map(x -> x.toString()).collect(Collectors.joining(", "))
                    + ")";
            }
        };
    }

    public static StatementDom labelOLD(String name) {
        return v -> v.visitLabel(name);
    }

    public static StatementDom goToOLD(String name) {
        return v -> v.visitGoTo(name);
    }


    public static StatementDom select(ExpressionDom expression, Object dflt, int[] keys, Object[] labels) {
        return new AbstractStatementDom() {
            @Override
            protected StatementDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultStatementDomVisitor() {
                    @Override
                    public void visitSwitch(ExpressionDom otherEpression, Object otherDflt, int[] otherKeys, Object[] otherLabels) {
                        r.accept(
                            expression.equals(otherEpression, context) &&
                            context.isSameLabel(dflt, otherDflt) &&
                            Arrays.equals(keys, otherKeys) &&
                            IntStream.range(0, labels.length).allMatch(i -> context.isSameLabel(labels[i], otherLabels[i]))
                        );
                    }
                };
            }

            @Override
            public void accept(StatementDomVisitor visitor) {
                visitor.visitSwitch(expression, dflt, keys, labels);
            }

            @Override
            public String toString() {
                return "switch(" + expression + ") {\n" +
                    IntStream.range(0, keys.length).mapToObj(i -> "case " + keys[i] + ": " + labels[i]).collect(Collectors.joining("\n")) + "\n" +
                    "default: " + dflt + "\n" +
                    "}";
            }
        };
    }

    // What about support for select expressions?
    public static StatementDom select(ExpressionDom expression, List<Map.Entry<Integer, StatementDom>> cases, StatementDom defaultBody) {
        Map<Integer, StatementDom> casesMap = cases.stream().collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
        return v -> v.visitSwitch(expression, casesMap, defaultBody);
    }

    public static Map.Entry<Integer, StatementDom> option(int key, StatementDom body) {
        return new AbstractMap.SimpleImmutableEntry<Integer, StatementDom>(key, body);
    }

    public static StatementDom methodBodyStatement() {
        return v -> v.visitMethodBody();
    }

    public static ExpressionDom methodBodyExpression() {
        return v ->
            v.visitMethodBody();
    }

    public static String arithmeticResultType(String lhsType, String rhsType) {
        switch(lhsType) {
            case Descriptor.BYTE:
                switch (rhsType) {
                    case Descriptor.BYTE: return lhsType;
                    case Descriptor.SHORT: return rhsType;
                    case Descriptor.INT: return rhsType;
                }
                break;
            case Descriptor.SHORT:
                switch (rhsType) {
                    case Descriptor.BYTE: return lhsType;
                    case Descriptor.SHORT: return lhsType;
                    case Descriptor.INT: return rhsType;
                }
                break;
            case Descriptor.INT:
                switch (rhsType) {
                    case Descriptor.BYTE: return lhsType;
                    case Descriptor.SHORT: return lhsType;
                    case Descriptor.INT: return lhsType;
                }
                break;
            case Descriptor.LONG:
                switch (rhsType) {
                    case Descriptor.LONG: return lhsType;
                }
                break;
            case Descriptor.FLOAT:
                switch (rhsType) {
                    case Descriptor.FLOAT: return lhsType;
                }
                break;
            case Descriptor.DOUBLE:
                switch (rhsType) {
                    case Descriptor.DOUBLE: return rhsType;
                }
                break;
        }

        return null;
    }

    public static String shiftResultType(String lhsType, String rhsType) {
        if(rhsType.equals(Descriptor.INT)) {
            switch (lhsType) {
                case Descriptor.BYTE:
                case Descriptor.SHORT:
                case Descriptor.INT:
                    return Descriptor.INT;
                case Descriptor.LONG:
                    return Descriptor.LONG;
            }
        }

        return null;
    }

    public static String bitwiseResultType(String lhsType, String rhsType) {
        switch (lhsType) {
            case Descriptor.BYTE:
            case Descriptor.SHORT:
            case Descriptor.INT:
                switch (rhsType) {
                    case Descriptor.BYTE:
                    case Descriptor.SHORT:
                    case Descriptor.INT:
                        return Descriptor.INT;
                }
                break;
            case Descriptor.LONG:
                switch (rhsType) {
                    case Descriptor.LONG:
                        return Descriptor.LONG;
                }
                break;
        }

        return null;
    }

    public static String logicalResultType(String lhsType, String rhsType) {
        return lhsType.equals(Descriptor.BOOLEAN) && rhsType.equals(Descriptor.BOOLEAN) ? Descriptor.BOOLEAN : null;
    }

    public static String compareResultType(String lhsType, String rhsType) {
        switch(lhsType) {
            case Descriptor.BYTE:
            case Descriptor.SHORT:
            case Descriptor.INT:
                switch (rhsType) {
                    case Descriptor.BYTE:
                    case Descriptor.SHORT:
                    case Descriptor.INT:
                        return Descriptor.BOOLEAN;
                }
        }

        return lhsType.equals(rhsType) ? Descriptor.BOOLEAN : null;
    }

    public static ExpressionDom typeCast(ExpressionDom expression, String targetTypeName) {
        return new AbstractExpressionDom() {
            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitTypeCast(ExpressionDom otherExpression, String otherTargetType) {
                        r.accept(
                            expression.equals(otherExpression, context) &&
                            targetTypeName.equals(otherTargetType)
                        );
                    }
                };
            }

            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitTypeCast(expression, targetTypeName);
            }

            @Override
            public String toString() {
                return "(" + Descriptor.getName(targetTypeName) + ") " + expression;
            }
        };
    }

    public static StatementDom throwStatement(ExpressionDom expression) {
        return v -> v.visitThrow(expression);
    }

    public static StatementDom tryCatchStatement(StatementDom tryBlock, List<CodeDom> catchBlocks) {
        return v -> v.visitTryCatch(tryBlock, catchBlocks);
    }

    public static CodeDom catchBlock(String type, String name, StatementDom block) {
        return v -> v.visitCatch(type, name, block);
    }

    public static ExpressionDom classLiteral(String type) {
        return new AbstractExpressionDom() {
            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitClassLiteral(String otherType) {
                        r.accept(type.equals(otherType));
                    }
                };
            }

            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitClassLiteral(type);
            }

            @Override
            public String toString() {
                return Descriptor.getName(type) + ".class";
            }
        };
    }

    public static StatementDom mark(Object label) {
        return new AbstractStatementDom() {
            @Override
            protected StatementDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultStatementDomVisitor() {
                    @Override
                    public void visitMark(Object otherLabel) {
                        r.accept(context.isSameLabel(label, otherLabel));
                    }
                };
            }

            @Override
            public void accept(StatementDomVisitor visitor) {
                visitor.visitMark(label);
            }

            @Override
            public String toString() {
                return "mark " + label;
            }
        };
    }

    public static StatementDom goTo(Object label) {
        return new AbstractStatementDom() {
            @Override
            protected StatementDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultStatementDomVisitor() {
                    @Override
                    public void visitGoTo(Object otherLabel) {
                        r.accept(context.isSameLabel(label, otherLabel));
                    }
                };
            }

            @Override
            public void accept(StatementDomVisitor visitor) {
                visitor.visitGoTo(label);
            }

            @Override
            public String toString() {
                return "goTo " + label;
            }
        };
    }

    public static ExpressionDom arrayLength(ExpressionDom expression) {
        return new AbstractExpressionDom() {
            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitArrayLength(ExpressionDom otherExpression) {
                        r.accept(expression.equals(otherExpression));
                    }
                };
            }

            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitArrayLength(expression);
            }

            @Override
            public String toString() {
                return expression + ".length";
            }
        };
    }

    public static StatementDom arrayStore(ExpressionDom expression, ExpressionDom index, ExpressionDom value) {
        return new AbstractStatementDom() {
            @Override
            protected StatementDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultStatementDomVisitor() {
                    @Override
                    public void visitArrayStore(ExpressionDom otherExpression, ExpressionDom otherIndex, ExpressionDom otherValue) {
                        r.accept(
                            expression.equals(otherExpression, context) &&
                            index.equals(otherIndex, context) &&
                            value.equals(otherValue, context)
                        );
                    }
                };
            }

            @Override
            public void accept(StatementDomVisitor visitor) {
                visitor.visitArrayStore(expression, index, value);
            }

            @Override
            public String toString() {
                return expression + "[" + index + "] = " + value;
            }
        };
    }

    public static ExpressionDom neg(ExpressionDom expression) {
        return new AbstractExpressionDom() {
            @Override
            protected ExpressionDomVisitor compare(CodeDomComparison context, Consumer<Boolean> r) {
                return new DefaultExpressionDomVisitor() {
                    @Override
                    public void visitNeg(ExpressionDom otherExpression) {
                        r.accept(expression.equals(otherExpression, context));
                    }
                };
            }

            @Override
            public void accept(ExpressionDomVisitor visitor) {
                visitor.visitNeg(expression);
            }

            @Override
            public String toString() {
                return "-" + expression;
            }
        };
    }
}
