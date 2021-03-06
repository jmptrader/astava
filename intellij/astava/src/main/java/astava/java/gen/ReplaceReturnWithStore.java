package astava.java.gen;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.Label;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.Hashtable;
import java.util.Map;

public class ReplaceReturnWithStore extends InstructionAdapter {
    public ReplaceReturnWithStore(GeneratorAdapter generator) {
        super(Opcodes.ASM5, generator);
    }

    private Label returnLabel;
    private int returnVar = -1;
    private Type returnType;

    @Override
    public void areturn(Type type) {
        if(returnLabel == null) {
            returnLabel = ((GeneratorAdapter)mv).newLabel();
            returnType = type;
            if(!returnType.equals(Type.VOID_TYPE))
                returnVar = ((GeneratorAdapter)mv).newLocal(type);
        }

        if(!returnType.equals(Type.VOID_TYPE))
            ((GeneratorAdapter)mv).storeLocal(returnVar);
        mv.visitJumpInsn(Opcodes.GOTO, returnLabel);
    }

    public void visitReturn() {
        if(returnLabel != null)
            mv.visitLabel(returnLabel);
    }

    public void loadValue() {
        if(!returnType.equals(Type.VOID_TYPE))
            ((GeneratorAdapter)mv).loadLocal(returnVar);
    }

    public void returnValue() {
        if(returnLabel != null) {
            loadValue();
            ((GeneratorAdapter)mv).returnValue();
        }
    }
}
