package shinovon.importsview;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodAdapter extends MethodVisitor implements Opcodes {

	public MethodAdapter(MethodVisitor methodVisitor, String className, String methodName, String methodDesc) {
		super(Opcodes.ASM4, methodVisitor);
	}
	
	public void visitMethodInsn(int op, String cls, String name, String sign) {
		Main.addMethod(op, cls, name, sign);
		super.visitMethodInsn(op, cls, name, sign);
	}
	
	public void visitFieldInsn(int op, String owner, String name, String desc) {
		Main.addField(op, owner, name, desc);
		super.visitFieldInsn(op, owner, name, desc);
	}
	
	public final void visitTypeInsn(final int n, final String s) {
		Main.addName(s);
		super.visitTypeInsn(n, s);
	}

}
