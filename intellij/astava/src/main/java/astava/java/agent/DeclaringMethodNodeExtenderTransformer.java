package astava.java.agent;

import astava.java.gen.MethodGenerator;
import astava.java.parser.ClassInspector;
import astava.java.parser.ClassResolver;
import astava.java.parser.MutableClassDeclaration;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.util.List;

public interface DeclaringMethodNodeExtenderTransformer {
    void transform(ClassNode classNode, MutableClassDeclaration thisClass, ClassResolver classResolver, ClassInspector classInspector, MethodNode methodNode, GeneratorAdapter generator, InsnList originalInstructions);

    default void transform(ClassNode classNode, MutableClassDeclaration thisClass, ClassResolver classResolver, ClassInspector classInspector, MethodNode methodNode) {

        {
            Printer printer = new Textifier();
            methodNode.accept(new TraceMethodVisitor(printer));
            System.out.println(methodNode.name + " pre:");
            printer.getText().forEach(x -> System.out.print(x.toString()));
        }

        InsnList originalInstructions = new InsnList();
        originalInstructions.add(methodNode.instructions);
        methodNode.instructions.clear();

        MethodGenerator.generate(methodNode, (mn, generator) -> {
            transform(classNode, thisClass, classResolver, classInspector, methodNode, generator, originalInstructions);
        });

        Printer printer = new Textifier();
        methodNode.accept(new TraceMethodVisitor(printer));
        System.out.println(methodNode.name + " post:");
        printer.getText().forEach(x -> System.out.print(x.toString()));
    }

    default DeclaringClassNodeExtenderTransformer when(DeclaringClassNodeExtenderElementMethodNodePredicate condition) {
        return (classNode, thisClass, classResolver, classInspector) -> {
            ((List<MethodNode>)classNode.methods).stream()
                .filter(m -> condition.test(classNode, thisClass, classResolver, m))
                .forEach(m -> this.transform(classNode, thisClass, classResolver, classInspector, m));
        };
    }

    default DeclaringMethodNodeExtenderTransformer andThen(DeclaringMethodNodeExtenderTransformer next) {
        DeclaringMethodNodeExtenderTransformer self = this;

        return new DeclaringMethodNodeExtenderTransformer() {
            @Override
            public void transform(ClassNode classNode, MutableClassDeclaration thisClass, ClassResolver classResolver, ClassInspector classInspector, MethodNode methodNode) {
                self.transform(classNode, thisClass, classResolver, classInspector, methodNode);
                next.transform(classNode, thisClass, classResolver, classInspector, methodNode);
            }

            @Override
            public void transform(ClassNode classNode, MutableClassDeclaration thisClass, ClassResolver classResolver, ClassInspector classInspector, MethodNode methodNode, GeneratorAdapter generator, InsnList originalInstructions) {

            }
        };
    }
}
