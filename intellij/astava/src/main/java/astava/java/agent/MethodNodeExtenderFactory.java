package astava.java.agent;

import astava.java.gen.MethodGenerator;
import astava.tree.ParameterInfo;
import astava.tree.StatementDom;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.ListIterator;

public class MethodNodeExtenderFactory {
    public static MethodNodeExtender sequence(MethodNodeExtender... extenders) {
        return (classNode, methodNode) -> {
            Arrays.asList(extenders).forEach(x -> x.transform(classNode, methodNode));
        };
    }

    public static MethodNodeExtender setBody(StatementDom replacement) {
        return (classNode, methodNode) -> {
            InsnList originalInstructions = new InsnList();
            originalInstructions.add(methodNode.instructions);
            methodNode.instructions.clear();

            MethodGenerator.generate(methodNode, (mn, generator) -> {
                Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
                List<ParameterInfo> parameters = IntStream.range(0, argumentTypes.length).mapToObj(i -> new ParameterInfo(
                    argumentTypes[i].getDescriptor(),
                    methodNode.parameters != null ? ((ParameterNode)methodNode.parameters.get(i)).name : "arg" + i
                )).collect(Collectors.toList());
                MethodGenerator methodGenerator = new MethodGenerator(classNode.name, parameters, replacement);
                methodGenerator.populateMethodBody(methodNode, originalInstructions, generator);
            });
        };
    }

    public static MethodNodeExtender append(StatementDom statement) {
        return (classNode, methodNode) -> {
            /*InsnList originalInstructions = new InsnList();
            originalInstructions.add(methodNode.instructions);

            ListIterator it = originalInstructions.iterator();

            Label returnLabel = new Label();

            while(it.hasNext()) {
                AbstractInsnNode insn = (AbstractInsnNode)it.next();

                if(insn.getOpcode()== Opcodes.IRETURN
                    ||insn.getOpcode()==Opcodes.RETURN
                    ||insn.getOpcode()==Opcodes.ARETURN
                    ||insn.getOpcode()==Opcodes.LRETURN
                    ||insn.getOpcode()==Opcodes.DRETURN) {
                    originalInstructions.set();
                }

                originalInstructions.insertBefore();
            }

            methodNode.instructions.clear();*/



            MethodGenerator.generate(methodNode, (mn, generator) -> {
                Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
                List<ParameterInfo> parameters = IntStream.range(0, argumentTypes.length).mapToObj(i -> new ParameterInfo(
                    argumentTypes[i].getDescriptor(),
                    methodNode.parameters != null ? ((ParameterNode)methodNode.parameters.get(i)).name : "arg" + i
                )).collect(Collectors.toList());

                InsnList originalInstructions = new InsnList();
                originalInstructions.add(methodNode.instructions);
                methodNode.instructions.clear();

                ListIterator it = originalInstructions.iterator();

                Label returnLabel = new Label();

                while(it.hasNext()) {
                    AbstractInsnNode insn = (AbstractInsnNode)it.next();

                    if(insn.getOpcode()== Opcodes.IRETURN
                        ||insn.getOpcode()==Opcodes.RETURN
                        ||insn.getOpcode()==Opcodes.ARETURN
                        ||insn.getOpcode()==Opcodes.LRETURN
                        ||insn.getOpcode()==Opcodes.DRETURN) {
                        generator.visitJumpInsn(Opcodes.GOTO, returnLabel);
                    } else {
                        insn.accept(generator);
                    }
                }

                System.out.println("Class name: " + classNode.name);
                System.out.println("Method name: " + methodNode.name);
                System.out.println("Method parameter count: " + (methodNode.parameters != null ? methodNode.parameters.size() : 0));
                Printer printer=new Textifier();
                mn.accept(new TraceMethodVisitor(printer));
                //printer.getText().forEach(x -> System.out.print(x.toString()));

                generator.visitLabel(returnLabel);

                MethodGenerator methodGenerator = new MethodGenerator(classNode.name, parameters, statement);
                methodGenerator.populateMethodBody(methodNode, originalInstructions, generator);

                generator.returnValue();

                printer=new Textifier();
                mn.accept(new TraceMethodVisitor(printer));
                //printer.getText().forEach(x -> System.out.print(x.toString()));
                //generator.ret();
                //generator.visitInsn(Opcodes.ARETURN);
            });
        };
    }
}