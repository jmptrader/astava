package astava.java.agent;

import astava.java.parser.ClassResolver;
import astava.java.parser.MutableClassDeclaration;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public interface DeclaringClassNodeExtenderElementMethodNodePredicate {
    boolean test(ClassNode classNode, MutableClassDeclaration thisClass, ClassResolver classResolver, MethodNode methodNode);
    default DeclaringClassNodeExtenderElement then(DeclaringMethodNodeExtenderElement element) {
        return new ConditionalDeclaringMethodNodeExtenderElement(this, element);
    }
}
