package astava.java.gen;

import astava.java.*;
import astava.tree.*;
import javafx.util.Pair;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.TableSwitchGenerator;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import static astava.java.DomFactory.*;

public class MethodGenerator {
    private String thisClassName;
    private StatementDom body;
    //private GenerateScope methodScope;
    private List<ParameterInfo> parameters;

    public MethodGenerator(ClassGenerator classGenerator, List<ParameterInfo> parameters, StatementDom body) {
        this(classGenerator.getClassName(), parameters, body);
    }

    public MethodGenerator(String thisClassName, List<ParameterInfo> parameters, StatementDom body) {
        this.thisClassName = thisClassName;
        this.parameters = parameters;
        this.body = body;
    }

    public void generate(MethodNode methodNode) {
        generate(methodNode, methodNode.instructions);
    }

    public void generate(MethodNode methodNode, InsnList originalInstructions) {
        generate(methodNode, (mn, generator) -> {
            populateMethodBody(methodNode, originalInstructions, generator);
        });
    }

    public static void generate(MethodNode methodNode, BiConsumer<MethodNode, GeneratorAdapter> bodyGenerator) {
        methodNode.visitCode();

        Method m = new Method(methodNode.name, methodNode.desc);
        GeneratorAdapter generator;
        try {
            generator = new GeneratorAdapter(methodNode.access, m, methodNode);
        } catch(Exception e) {
            generator = null;
        }

        bodyGenerator.accept(methodNode, generator);

        methodNode.visitEnd();
        methodNode.visitMaxs(0, 0);
        //labelScope.verify();
    }

    public void populateMethodBody(MethodNode methodNode, InsnList originalInstructions, GeneratorAdapter generator) {
        LabelScope labelScope = new LabelScope();
        populateMethodStatement(methodNode, originalInstructions, generator, body, null, labelScope, new GenerateScope(), new Hashtable<>());
        labelScope.verify();
    }

    public String populateMethodStatement(MethodNode methodNode, InsnList originalInstructions, GeneratorAdapter generator, StatementDom statement, Label breakLabel, LabelScope labelScope, GenerateScope scope, Hashtable<Object, Label> astLabelToASMLabelMap) {
        statement.accept(new StatementDomVisitor() {
            @Override
            public void visitVariableDeclaration(String type, String name) {
                scope.declareVar(generator, type, name);
            }

            @Override
            public void visitVariableAssignment(String name, ExpressionDom value) {
                String valueType = populateMethodExpression(methodNode, originalInstructions, generator, value, null, true, scope, astLabelToASMLabelMap);
                int id = scope.getVarId(name);
                generator.storeLocal(id, Type.getType(valueType));
            }

            @Override
            public void visitFieldAssignment(ExpressionDom target, String name, String type, ExpressionDom value) {
                String targetType = populateMethodExpression(methodNode, originalInstructions, generator, target, null, true, scope, astLabelToASMLabelMap);
                String valueType = populateMethodExpression(methodNode, originalInstructions, generator, value, null, true, scope, astLabelToASMLabelMap);
                generator.putField(Type.getType(targetType), name, Type.getType(Descriptor.getFieldDescriptor(type)));
            }

            @Override
            public void visitStaticFieldAssignment(String typeName, String name, String type, ExpressionDom value) {
                String valueType = populateMethodExpression(methodNode, originalInstructions, generator, value, null, true, scope, astLabelToASMLabelMap);
                generator.putStatic(Type.getType(typeName), name, Type.getType(Descriptor.getFieldDescriptor(type)));
            }

            @Override
            public void visitIncrement(String name, int amount) {
                int id = scope.getVarId(name);
                generator.iinc(id, amount);
            }

            @Override
            public void visitReturnValue(ExpressionDom expression) {
                String resultType = populateMethodExpression(methodNode, originalInstructions, generator, expression, null, true, scope, astLabelToASMLabelMap);

                if (resultType.equals(Descriptor.VOID))
                    throw new IllegalArgumentException("Expression of return statement results in void.");

                generator.returnValue();
            }

            @Override
            public void visitBlock(List<StatementDom> statements) {
                statements.forEach(s ->
                    populateMethodStatement(methodNode, originalInstructions, generator, s, breakLabel, labelScope, scope, astLabelToASMLabelMap));
            }

            @Override
            public void visitIfElse(ExpressionDom condition, StatementDom ifTrue, StatementDom ifFalse) {
                boolean emptyElse = Util.returnFrom(false, r -> ifFalse.accept(new StatementDomVisitor.Default() {
                    @Override
                    public void visitBlock(List<StatementDom> statements) {
                        r.accept(statements.size() == 0);
                    }
                }));

                Label endLabel = generator.newLabel();
                Label ifFalseLabel = generator.newLabel();

                String resultType = populateMethodExpression(methodNode, originalInstructions, generator, condition, ifFalseLabel, false, scope, astLabelToASMLabelMap);
                populateMethodStatement(methodNode, originalInstructions, generator, ifTrue, breakLabel, labelScope, scope, astLabelToASMLabelMap);
                if(!emptyElse) {
                    generator.goTo(endLabel);
                }
                generator.visitLabel(ifFalseLabel);
                if(!emptyElse) {
                    populateMethodStatement(methodNode, originalInstructions, generator, ifFalse, breakLabel, labelScope, scope, astLabelToASMLabelMap);
                    generator.visitLabel(endLabel);
                }
            }

            @Override
            public void visitBreakCase() {
                generator.goTo(breakLabel);
            }

            @Override
            public void visitReturn() {
                generator.visitInsn(Opcodes.RETURN);
            }

            @Override
            public void visitInvocation(int invocation, ExpressionDom target, String type, String name, String descriptor, List<ExpressionDom> arguments) {
                populateMethodInvocation(methodNode, originalInstructions, generator, scope, invocation, target, type, name, descriptor, arguments, CODE_LEVEL_STATEMENT, astLabelToASMLabelMap);
            }

            @Override
            public void visitNewInstance(String type, List<String> parameterTypes, List<ExpressionDom> arguments) {
                populateMethodNewInstance(methodNode, originalInstructions, generator, scope, type, parameterTypes, arguments, CODE_LEVEL_STATEMENT, astLabelToASMLabelMap);
            }

            @Override
            public void visitLabel(String name) {
                labelScope.label(generator, name);
            }

            @Override
            public void visitGoTo(String name) {
                labelScope.goTo(generator, name);
            }

            @Override
            public void visitSwitch(ExpressionDom expression, Map<Integer, StatementDom> cases, StatementDom defaultBody) {
                populateMethodExpression(methodNode, originalInstructions, generator, expression, null, true, scope, astLabelToASMLabelMap);

                Map<Integer, StatementDom> keyToBodyMap = cases;
                int[] keys = keyToBodyMap.keySet().stream().mapToInt(x -> (int) x).toArray();

                generator.tableSwitch(keys, new TableSwitchGenerator() {
                    Label switchEnd;

                    @Override
                    public void generateCase(int key, Label end) {
                        switchEnd = end;

                        StatementDom body = keyToBodyMap.get(key);
                        populateMethodStatement(methodNode, originalInstructions, generator, body, end, labelScope, scope, astLabelToASMLabelMap);
                    }

                    @Override
                    public void generateDefault() {
                        populateMethodStatement(methodNode, originalInstructions, generator, defaultBody, switchEnd, labelScope, scope, astLabelToASMLabelMap);
                    }
                });
            }

            @Override
            public void visitASM(MethodNode methodNode) {

            }

            @Override
            public void visitMethodBody() {
                ReplaceReturnWithPop instructionAdapter = new ReplaceReturnWithPop(
                    new GeneratorAdapter(methodNode.access, new Method(methodNode.name, methodNode.desc), new RemapLabel(generator)));

                originalInstructions.accept(instructionAdapter);

                instructionAdapter.visitReturn();
            }

            @Override
            public void visitThrow(ExpressionDom expression) {
                populateMethodExpression(methodNode, originalInstructions, generator, expression, null, true, scope, astLabelToASMLabelMap);
                generator.throwException();
            }

            @Override
            public void visitTryCatch(StatementDom tryBlock, List<CodeDom> catchBlocks) {
                Optional<CodeDom> finallyBlock = catchBlocks.stream().filter(cb -> Util.returnFrom(false, r -> cb.accept(new DefaultCodeDomVisitor() {
                    @Override
                    public void visitCatch(String type, String name, StatementDom statementDom) {
                        if(type == null)
                            r.accept(true);
                    }
                }))).findFirst();

                Label tryStart = generator.newLabel();
                Label tryEnd = generator.newLabel();
                Label endAll = generator.newLabel();
                InstructionAdapter instructionAdapter =
                    finallyBlock.isPresent() ? new ReplaceReturnWithStore(generator)
                    : new InstructionAdapter(generator);

                Method m = new Method(methodNode.name, methodNode.desc);
                GeneratorAdapter innerGenerator = new GeneratorAdapter(methodNode.access, m, instructionAdapter);

                generator.visitLabel(tryStart);
                populateMethodStatement(methodNode, originalInstructions, innerGenerator, tryBlock, breakLabel, labelScope, scope, astLabelToASMLabelMap);
                generator.visitLabel(tryEnd);

                if(finallyBlock.isPresent()) {
                    StatementDom statementDom = Util.returnFrom(null, r -> finallyBlock.get().accept(new DefaultCodeDomVisitor() {
                        @Override
                        public void visitCatch(String type, String name, StatementDom statementDom) {
                            r.accept(statementDom);
                        }
                    }));

                    GenerateScope finallyScope = new GenerateScope(scope);

                    ((ReplaceReturnWithStore)instructionAdapter).visitReturn();
                    populateMethodStatement(methodNode, originalInstructions, generator, statementDom, breakLabel, labelScope, finallyScope, astLabelToASMLabelMap);
                    ((ReplaceReturnWithStore)instructionAdapter).returnValue();
                }

                generator.visitJumpInsn(Opcodes.GOTO, endAll);

                ArrayList<Pair<Label, Label>> attempts = new ArrayList<Pair<Label, Label>>();

                attempts.add(new Pair<>(tryStart, tryEnd));

                catchBlocks.forEach(cb -> {
                    cb.accept(new DefaultCodeDomVisitor() {
                        @Override
                        public void visitCatch(String type, String name, StatementDom statementDom) {
                            if (type != null) {
                                // A non-finally catch
                                Label handlerStart = generator.newLabel();
                                Label handlerEnd = generator.newLabel();

                                GenerateScope catchScope = new GenerateScope(scope);

                                catchScope.declareVar(generator, type, name);

                                InstructionAdapter instructionAdapter = finallyBlock.isPresent()
                                    ? new ReplaceReturnWithStore(generator)
                                    : new InstructionAdapter(generator);

                                Method m = new Method(methodNode.name, methodNode.desc);
                                GeneratorAdapter innerGenerator = new GeneratorAdapter(methodNode.access, m, instructionAdapter);

                                generator.visitLabel(handlerStart);
                                generator.storeLocal(catchScope.getVarId(name));
                                populateMethodStatement(methodNode, originalInstructions, innerGenerator, statementDom, breakLabel, labelScope, catchScope, astLabelToASMLabelMap);
                                generator.visitLabel(handlerEnd);

                                generator.visitTryCatchBlock(tryStart, tryEnd, handlerStart, type);

                                attempts.add(new Pair<>(handlerStart, handlerEnd));

                                if (finallyBlock.isPresent()) {
                                    StatementDom finallyStatementDom = Util.returnFrom(null, r -> finallyBlock.get().accept(new DefaultCodeDomVisitor() {
                                        @Override
                                        public void visitCatch(String type, String name, StatementDom statementDom) {
                                            r.accept(statementDom);
                                        }
                                    }));

                                    GenerateScope finallyScope = new GenerateScope(scope);

                                    ((ReplaceReturnWithStore)instructionAdapter).visitReturn();
                                    populateMethodStatement(methodNode, originalInstructions, generator, finallyStatementDom, breakLabel, labelScope, finallyScope, astLabelToASMLabelMap);
                                    ((ReplaceReturnWithStore)instructionAdapter).returnValue();
                                }

                                generator.visitJumpInsn(Opcodes.GOTO, endAll);
                            }
                        }
                    });
                });

                if (finallyBlock.isPresent()) {
                    // Something goes wrong in try block or a catch block
                    attempts.forEach(x -> {
                        StatementDom finallyStatementDom = Util.returnFrom(null, r -> finallyBlock.get().accept(new DefaultCodeDomVisitor() {
                            @Override
                            public void visitCatch(String type, String name, StatementDom statementDom) {
                                r.accept(statementDom);
                            }
                        }));

                        Label finallyHandlerStart = generator.newLabel();

                        GenerateScope finallyScope = new GenerateScope(scope);

                        int finallyExceptionId = generator.newLocal(Type.getType(Exception.class));

                        generator.visitLabel(finallyHandlerStart);
                        generator.storeLocal(finallyExceptionId);
                        populateMethodStatement(methodNode, originalInstructions, generator, finallyStatementDom, breakLabel, labelScope, finallyScope, astLabelToASMLabelMap);
                        generator.loadLocal(finallyExceptionId);
                        generator.throwException();

                        generator.visitTryCatchBlock(x.getKey(), x.getValue(), finallyHandlerStart, null);
                    });
                }

                generator.visitLabel(endAll);
            }

            @Override
            public void visitMark(Object label) {
                Label asmLabel = astLabelToASMLabelMap.computeIfAbsent(label, l -> generator.newLabel());
                generator.visitLabel(asmLabel);
            }

            @Override
            public void visitGoTo(Object label) {
                Label asmLabel = astLabelToASMLabelMap.computeIfAbsent(label, l -> generator.newLabel());
                generator.visitJumpInsn(Opcodes.GOTO, asmLabel);
            }

            @Override
            public void visitArrayStore(ExpressionDom expression, ExpressionDom index, ExpressionDom value) {
                populateMethodExpression(methodNode, originalInstructions, generator, expression, null, true, scope, astLabelToASMLabelMap);
                populateMethodExpression(methodNode, originalInstructions, generator, index, null, true, scope, astLabelToASMLabelMap);
                String valueType = populateMethodExpression(methodNode, originalInstructions, generator, value, null, true, scope, astLabelToASMLabelMap);
                generator.arrayStore(Type.getType(valueType));
            }

            @Override
            public void visitSwitch(ExpressionDom expression, Object dflt, int[] keys, Object[] labels) {
                populateMethodExpression(methodNode, originalInstructions, generator, expression, null, true, scope, astLabelToASMLabelMap);
                Label asmDflt = astLabelToASMLabelMap.computeIfAbsent(dflt, l -> generator.newLabel());
                Label[] asmLabels = Arrays.asList(labels).stream()
                    .map(x -> astLabelToASMLabelMap.computeIfAbsent(x, l -> generator.newLabel()))
                    .toArray(s -> new Label[s]);
                generator.visitLookupSwitchInsn(asmDflt, keys, asmLabels);
            }

            @Override
            public void visitIfJump(ExpressionDom condition, Object label) {
                Label ifFalseLabel =  astLabelToASMLabelMap.computeIfAbsent(label, l -> generator.newLabel());

                String resultType = populateMethodExpression(methodNode, originalInstructions, generator, condition, ifFalseLabel, false, scope, astLabelToASMLabelMap);
            }
        });

        return Descriptor.VOID;
    }

    public String populateMethodExpression(MethodNode methodNode, InsnList originalInstructions, GeneratorAdapter generator, ExpressionDom expression, Label ifFalseLabel, boolean reifyCondition, GenerateScope scope, Hashtable<Object, Label> astLabelToASMLabelMap) {
        return new ExpressionDomVisitor.Return<String>() {
            @Override
            public void visitBooleanLiteral(boolean value) {
                if(!value) {
                    if(reifyCondition)
                        generator.push(value);
                    else //if(ifFalseLabel != null)
                        generator.goTo(ifFalseLabel);
                } else {
                    if(reifyCondition)
                        generator.push(value);
                }

                setResult(Descriptor.BOOLEAN);
            }

            @Override
            public void visitByteLiteral(byte value) {
                generator.push(value);

                setResult(Descriptor.BYTE);
            }

            @Override
            public void visitShortLiteral(short value) {
                generator.push(value);

                setResult(Descriptor.SHORT);
            }

            @Override
            public void visitIntLiteral(int value) {
                generator.push(value);

                setResult(Descriptor.INT);
            }

            @Override
            public void visitLongLiteral(long value) {
                generator.push(value);

                setResult(Descriptor.LONG);
            }

            @Override
            public void visitFloatLiteral(float value) {
                generator.push(value);

                setResult(Descriptor.FLOAT);
            }

            @Override
            public void visitDoubleLiteral(double value) {
                generator.push(value);

                setResult(Descriptor.DOUBLE);
            }

            @Override
            public void visitCharLiteral(char value) {
                generator.push(value);

                setResult(Descriptor.CHAR);
            }

            @Override
            public void visitStringLiteral(String value) {
                generator.push(value);

                setResult(Descriptor.STRING);
            }

            @Override
            public void visitNull() {
                generator.visitInsn(Opcodes.ACONST_NULL);

                // Cannot determine type? // Object is the most specific type?
                setResult(Descriptor.get(Object.class));
            }

            @Override
            public void visitArithmetic(int operator, ExpressionDom lhs, ExpressionDom rhs) {
                int op;

                switch(operator) {
                    case ArithmeticOperator.ADD: op = GeneratorAdapter.ADD; break;
                    case ArithmeticOperator.SUB: op = GeneratorAdapter.SUB; break;
                    case ArithmeticOperator.MUL: op = GeneratorAdapter.MUL; break;
                    case ArithmeticOperator.DIV: op = GeneratorAdapter.DIV; break;
                    case ArithmeticOperator.REM: op = GeneratorAdapter.REM; break;
                    default: op = -1;
                }

                String lhsResultType = populateMethodExpression(methodNode, originalInstructions, generator, lhs, ifFalseLabel, false, scope, astLabelToASMLabelMap);
                String rhsResultType = populateMethodExpression(methodNode, originalInstructions, generator, rhs, ifFalseLabel, reifyCondition, scope, astLabelToASMLabelMap);

                String resultType = arithmeticResultType(lhsResultType, rhsResultType);
                Type t = Type.getType(resultType);
                generator.math(op, t);

                setResult(resultType);
            }

            @Override
            public void visitShift(int operator, ExpressionDom lhs, ExpressionDom rhs) {
                int op;

                switch(operator) {
                    case ShiftOperator.SHL: op = GeneratorAdapter.SHL; break;
                    case ShiftOperator.SHR: op = GeneratorAdapter.SHR; break;
                    case ShiftOperator.USHR: op = GeneratorAdapter.USHR; break;
                    default: op = -1;
                }

                String lhsResultType = populateMethodExpression(methodNode, originalInstructions, generator, lhs, ifFalseLabel, reifyCondition, scope, astLabelToASMLabelMap);
                String rhsResultType = populateMethodExpression(methodNode, originalInstructions, generator, rhs, ifFalseLabel, reifyCondition, scope, astLabelToASMLabelMap);
                String resultType = shiftResultType(lhsResultType, rhsResultType);
                Type t = Type.getType(resultType);
                generator.math(op, t);

                setResult(resultType);
            }

            @Override
            public void visitBitwise(int operator, ExpressionDom lhs, ExpressionDom rhs) {
                int op;

                switch(operator) {
                    case BitwiseOperator.AND: op = GeneratorAdapter.AND; break;
                    case BitwiseOperator.OR: op = GeneratorAdapter.OR; break;
                    case BitwiseOperator.XOR: op = GeneratorAdapter.XOR; break;
                    default: op = -1;
                }

                String lhsResultType = populateMethodExpression(methodNode, originalInstructions, generator, lhs, ifFalseLabel, reifyCondition, scope, astLabelToASMLabelMap);
                String rhsResultType = populateMethodExpression(methodNode, originalInstructions, generator, rhs, ifFalseLabel, reifyCondition, scope, astLabelToASMLabelMap);
                String resultType = bitwiseResultType(lhsResultType, rhsResultType);
                Type t = Type.getType(resultType);
                generator.math(op, t);

                setResult(resultType);
            }

            @Override
            public void visitCompare(int operator, ExpressionDom lhs, ExpressionDom rhs) {
                String lhsResultType = populateMethodExpression(methodNode, originalInstructions, generator, lhs, ifFalseLabel, true, scope, astLabelToASMLabelMap);
                String rhsResultType = populateMethodExpression(methodNode, originalInstructions, generator, rhs, ifFalseLabel, true, scope, astLabelToASMLabelMap);

                int op;

                switch (operator) {
                    case RelationalOperator.LT: op = GeneratorAdapter.GE; break;
                    case RelationalOperator.LE: op = GeneratorAdapter.GT; break;
                    case RelationalOperator.GT: op = GeneratorAdapter.LE; break;
                    case RelationalOperator.GE: op = GeneratorAdapter.LT; break;
                    case RelationalOperator.EQ: op = GeneratorAdapter.NE; break;
                    case RelationalOperator.NE: op = GeneratorAdapter.EQ; break;
                    default: op = -1;
                }

                Type t = Type.getType(lhsResultType);

                if (reifyCondition) {
                    Label endLabel = generator.newLabel();
                    Label innerIfFalseLabel = generator.newLabel();

                    generator.ifCmp(t, op, innerIfFalseLabel);
                    generator.push(true);
                    generator.goTo(endLabel);
                    generator.visitLabel(innerIfFalseLabel);
                    generator.push(false);
                    generator.visitLabel(endLabel);
                } else {
                    generator.ifCmp(t, op, ifFalseLabel);
                }

                setResult(Descriptor.BOOLEAN);
            }

            @Override
            public void visitObjectEquality(int operator, ExpressionDom lhs, ExpressionDom rhs) {
                String lhsResultType = populateMethodExpression(methodNode, originalInstructions, generator, lhs, ifFalseLabel, reifyCondition, scope, astLabelToASMLabelMap);
                String rhsResultType = populateMethodExpression(methodNode, originalInstructions, generator, rhs, ifFalseLabel, reifyCondition, scope, astLabelToASMLabelMap);

                int opcode;

                switch (operator) {
                    case RelationalOperator.EQ: opcode = Opcodes.IF_ACMPEQ; break;
                    case RelationalOperator.NE: opcode = Opcodes.IF_ACMPNE; break;
                    default: opcode = -1;
                }

                Type t = Type.getType(lhsResultType);

                if (reifyCondition) {
                    Label endLabel = generator.newLabel();
                    Label innerIfFalseLabel = generator.newLabel();

                    generator.visitJumpInsn(opcode, innerIfFalseLabel);
                    generator.push(true);
                    generator.goTo(endLabel);
                    generator.visitLabel(innerIfFalseLabel);
                    generator.push(false);
                    generator.visitLabel(endLabel);
                } else {
                    generator.visitJumpInsn(opcode, ifFalseLabel);
                }

                setResult(Descriptor.BOOLEAN);
            }

            @Override
            public void visitLogical(int operator, ExpressionDom lhs, ExpressionDom rhs) {
                String resultType = null;

                switch(operator) {
                    case LogicalOperator.AND: {
                        Label lhsIfFalseLabel = ifFalseLabel != null ? ifFalseLabel : generator.newLabel();
                        boolean lhsReify = ifFalseLabel != null ? false : true;
                        String lhsResultType = populateMethodExpression(methodNode, originalInstructions, generator, lhs, lhsIfFalseLabel, lhsReify, scope, astLabelToASMLabelMap);
                        String rhsResultType = populateMethodExpression(methodNode, originalInstructions, generator, rhs, ifFalseLabel, reifyCondition, scope, astLabelToASMLabelMap);

                        if(ifFalseLabel == null) {
                            generator.visitLabel(lhsIfFalseLabel);
                        }

                        resultType = logicalResultType(lhsResultType, rhsResultType);

                        break;
                    }
                    case LogicalOperator.OR: {
                        Label endLabel = generator.newLabel();
                        Label nextTestLabel = generator.newLabel();
                        String lhsResultType = populateMethodExpression(methodNode, originalInstructions, generator, lhs, nextTestLabel, false, scope, astLabelToASMLabelMap);
                        if(reifyCondition)
                            generator.push(true);
                        generator.goTo(endLabel);
                        generator.visitLabel(nextTestLabel);
                        String rhsResultType = populateMethodExpression(methodNode, originalInstructions, generator, rhs, ifFalseLabel, reifyCondition, scope, astLabelToASMLabelMap);
                        generator.visitLabel(endLabel);
                        resultType = logicalResultType(lhsResultType, rhsResultType);

                        break;
                    }
                }

                setResult(resultType);
            }

            @Override
            public void visitVariableAccess(String name) {
                OptionalInt parameterOrdinal = IntStream.range(0, parameters.size()).filter(x -> parameters.get(x).name.equals(name)).findFirst();

                if(parameterOrdinal.isPresent()) {
                    generator.loadArg(parameterOrdinal.getAsInt());
                    setResult(parameters.get(parameterOrdinal.getAsInt()).descriptor);
                } else {
                    int id = scope.getVarId(name);
                    generator.loadLocal(id);

                    setResult(scope.getVarType(name));
                }
            }

            @Override
            public void visitFieldAccess(ExpressionDom target, String name, String fieldTypeName) {
                String targetType = populateMethodExpression(methodNode, originalInstructions, generator, target, null, true, scope, astLabelToASMLabelMap);
                generator.getField(Type.getType(targetType), name, Type.getType(Descriptor.getFieldDescriptor(fieldTypeName)));

                setResult(fieldTypeName);
            }

            @Override
            public void visitStaticFieldAccess(String typeName, String name, String fieldTypeName) {
                generator.getStatic(Type.getType(typeName), name, Type.getType(Descriptor.getFieldDescriptor(fieldTypeName)));

                setResult(fieldTypeName);
            }

            @Override
            public void visitNot(ExpressionDom expression) {
                String resultType = populateMethodExpression(methodNode, originalInstructions, generator, expression, null, true, scope, astLabelToASMLabelMap);

                if(reifyCondition)
                    generator.not();
                if(ifFalseLabel != null)
                    generator.ifZCmp(GeneratorAdapter.NE, ifFalseLabel);

                setResult(Descriptor.BOOLEAN);
            }

            @Override
            public void visitInstanceOf(ExpressionDom expression, String type) {
                String resultType = populateMethodExpression(methodNode, originalInstructions, generator, expression, null, true, scope, astLabelToASMLabelMap);
                Type t = Type.getType(type);

                generator.instanceOf(t);

                setResult(Descriptor.BOOLEAN);
            }

            @Override
            public void visitBlock(List<CodeDom> codeList) {
                // Exactly one expression should be contained within codeList
                List<String> expressionResultTypes = new ArrayList<>();

                LabelScope labelScope = new LabelScope();

                codeList.forEach(code -> {
                    code.accept(new DefaultCodeDomVisitor() {
                        @Override
                        public void visitStatement(StatementDom statementDom) {
                            populateMethodStatement(methodNode, originalInstructions, generator, statementDom, null, labelScope, scope, astLabelToASMLabelMap);
                        }

                        @Override
                        public void visitExpression(ExpressionDom expressionDom) {
                            String resultType = populateMethodExpression(methodNode, originalInstructions, generator, expressionDom, null, true, scope, astLabelToASMLabelMap);
                            expressionResultTypes.add(resultType);
                        }
                    });
                });

                labelScope.verify();

                if(expressionResultTypes.size() > 1)
                    throw new IllegalArgumentException("Expression block has multiple expressions.");
                else if(expressionResultTypes.size() == 0)
                    throw new IllegalArgumentException("Expression block has no expressions.");

                setResult(expressionResultTypes.get(0));
            }

            @Override
            public void visitIfElse(ExpressionDom condition, ExpressionDom ifTrue, ExpressionDom ifFalse) {
                Label endLabel = generator.newLabel();
                Label testIfFalseLabel = generator.newLabel();

                String resultType = populateMethodExpression(methodNode, originalInstructions, generator, condition, testIfFalseLabel, false, scope, astLabelToASMLabelMap);
                String ifTrueResultType = populateMethodExpression(methodNode, originalInstructions, generator, ifTrue, null, true, scope, astLabelToASMLabelMap);
                generator.goTo(endLabel);
                generator.visitLabel(testIfFalseLabel);
                String ifFalseResultType = populateMethodExpression(methodNode, originalInstructions, generator, ifFalse, null, true, scope, astLabelToASMLabelMap);
                generator.visitLabel(endLabel);

                if(!ifTrueResultType.equals(ifFalseResultType))
                    throw new IllegalArgumentException("Inconsistent result types in test: ifTrue => " + ifTrueResultType + ", ifFalse => " + ifFalseResultType);

                setResult(ifTrueResultType);
            }

            @Override
            public void visitInvocation(int invocation, ExpressionDom target, String type, String name, String descriptor, List<ExpressionDom> arguments) {
                String resultType = populateMethodInvocation(methodNode, originalInstructions, generator, scope, invocation, target, type, name, descriptor, arguments, CODE_LEVEL_EXPRESSION, astLabelToASMLabelMap);

                if(!reifyCondition && ifFalseLabel != null && resultType.equals(Descriptor.get(boolean.class))) {
                    generator.ifZCmp(GeneratorAdapter.EQ, ifFalseLabel);
                }

                setResult(resultType);
            }

            @Override
            public void visitNewInstance(String type, List<String> parameterTypes, List<ExpressionDom> arguments) {
                String resultType = populateMethodNewInstance(methodNode, originalInstructions, generator, scope, type, parameterTypes, arguments, CODE_LEVEL_EXPRESSION, astLabelToASMLabelMap);
                setResult(resultType);
            }

            @Override
            public void visitThis() {
                generator.loadThis();
                setResult(Descriptor.get(thisClassName));
            }

            @Override
            public void visitTop(ExpressionDom expression, BiFunction<ExpressionDom, ExpressionDom, ExpressionDom> usage) {
                String topResultType = populateMethodExpression(methodNode, originalInstructions, generator, expression, ifFalseLabel, reifyCondition, scope, astLabelToASMLabelMap);
                ExpressionDom dup = v -> v.visitDup(topResultType);
                ExpressionDom last = v -> v.visitLetBe(topResultType);
                ExpressionDom usageExpression = usage.apply(dup, last);
                String resultType = populateMethodExpression(methodNode, originalInstructions, generator, usageExpression, ifFalseLabel, reifyCondition, scope, astLabelToASMLabelMap);
                setResult(resultType);
            }

            @Override
            public void visitDup(String type) {
                generator.dup();
                setResult(type);
            }

            @Override
            public void visitLetBe(String type) {
                setResult(type);
            }

            @Override
            public void visitTypeCast(ExpressionDom expression, String targetType) {
                String resultType = populateMethodExpression(methodNode, originalInstructions, generator, expression, null, true, scope, astLabelToASMLabelMap);
                // What about type casting primitive values?
                //generator.cast(Type.getType(resultType), Type.getType(targetType));
                generator.checkCast(Type.getType(targetType));
                setResult(targetType);
            }

            @Override
            public void visitMethodBody() {
                if(Type.getReturnType(methodNode.desc).equals(Type.VOID_TYPE))
                    throw new IllegalArgumentException("Illegal method body expression; method returns void.");

                ReplaceReturnWithStore instructionAdapter = new ReplaceReturnWithStore(
                    new GeneratorAdapter(methodNode.access, new Method(methodNode.name, methodNode.desc), new RemapLabel(generator)));

                originalInstructions.accept(instructionAdapter);

                instructionAdapter.visitReturn();
                instructionAdapter.loadValue();

                setResult(Type.getReturnType(methodNode.desc).getDescriptor());
            }

            @Override
            public void visitClassLiteral(String type) {
                generator.push(Type.getType(type));
                setResult(Type.getDescriptor(Class.class));
            }

            @Override
            public void visitArrayLength(ExpressionDom expression) {
                String resultType = populateMethodExpression(methodNode, originalInstructions, generator, expression, null, true, scope, astLabelToASMLabelMap);
                generator.arrayLength();
                setResult(Descriptor.get(int.class));
            }

            @Override
            public void visitNeg(ExpressionDom expression) {
                String resultType = populateMethodExpression(methodNode, originalInstructions, generator, expression, null, true, scope, astLabelToASMLabelMap);
                generator.visitInsn(Opcodes.INEG);
                setResult(resultType);

            }
        }.returnFrom(expression);
    }

    private static final int CODE_LEVEL_STATEMENT = 0;
    private static final int CODE_LEVEL_EXPRESSION = 1;

    private String populateMethodInvocation(
        MethodNode methodNode, InsnList originalInstructions, GeneratorAdapter generator, GenerateScope scope,
        int invocation, ExpressionDom target, String type, String name, String descriptor, List<ExpressionDom> arguments,
        int codeLevel, Hashtable<Object, Label> astLabelToASMLabelMap) {
        String returnType = descriptor.substring(descriptor.indexOf(")") + 1);

        if(codeLevel == CODE_LEVEL_EXPRESSION && returnType.equals(Descriptor.VOID))
            throw new IllegalArgumentException("Invocations at expression level must return non-void occurrences.");

        // Push target for instance invocations
        if(target != null)
            populateMethodExpression(methodNode, originalInstructions, generator, target, null, true, scope, astLabelToASMLabelMap);

        arguments.forEach(a ->
            populateMethodExpression(methodNode, originalInstructions, generator, a, null, true, scope, astLabelToASMLabelMap));

        switch (invocation) {
            case Invocation.INTERFACE:
                generator.invokeInterface(Type.getType(type), new Method(name, descriptor));
                break;
            case Invocation.STATIC:
                generator.invokeStatic(Type.getType(type), new Method(name, descriptor));
                break;
            case Invocation.VIRTUAL:
                generator.invokeVirtual(Type.getType(type), new Method(name, descriptor));
                break;
            case Invocation.SPECIAL:
                generator.invokeConstructor(Type.getType(type), new Method(name, descriptor));
                break;
        }

        if(codeLevel == CODE_LEVEL_STATEMENT && !returnType.equals(Descriptor.VOID))
            generator.pop(); // Pop unused return occurrences

        return returnType;
    }

    private String populateMethodNewInstance(
        MethodNode methodNode, InsnList originalInstructions, GeneratorAdapter generator, GenerateScope scope,
        String type, List<String> parameterTypes, List<ExpressionDom> arguments,
        int codeLevel, Hashtable<Object, Label> astLabelToASMLabelMap) {
        String returnType = type;

        generator.newInstance(Type.getType(type));
        generator.dup();
        arguments.forEach(a ->
            populateMethodExpression(methodNode, originalInstructions, generator, a, null, true, scope, astLabelToASMLabelMap));
        generator.invokeConstructor(Type.getType(type), new Method("<init>", Descriptor.getMethodDescriptor(parameterTypes, Descriptor.VOID)));

        if(codeLevel == CODE_LEVEL_STATEMENT) {
            generator.pop();
            returnType = Descriptor.VOID;
        }

        return returnType;
    }
}
