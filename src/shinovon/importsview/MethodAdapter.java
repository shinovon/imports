package shinovon.importsview;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodAdapter extends MethodVisitor implements Opcodes {

	public MethodAdapter(MethodVisitor methodVisitor, String className, String methodName, String methodDesc) {
		super(Opcodes.ASM4, methodVisitor);
	}
	
	public void visitMethodInsn(int acc, String cls, String name, String sign) {
		Main.addName(cls);
		Main.addDesc(sign);
		super.visitMethodInsn(acc, cls, name, sign);
	}
	
	public void visitFieldInsn(int n, String s, String s2, String s3) {
		Main.addDesc(s3);
		super.visitFieldInsn(n, s, s2, s3);
	}
	
	public final void visitTypeInsn(final int n, final String s) {
		Main.addName(s);
		super.visitTypeInsn(n, s);
	}

}
