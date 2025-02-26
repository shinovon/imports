package shinovon.importsview;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class ClassAdapter extends ClassVisitor implements Opcodes {

	private String className;

	public ClassAdapter(ClassVisitor classVisitor, String s) {
		super(ASM4, classVisitor);
		this.className = s;
	}
	
	public final MethodVisitor visitMethod(int acc, String name, String desc, final String sign, final String[] array) {
		Main.addDesc(desc);
		final MethodVisitor visitMethod;
		if ((visitMethod = super.visitMethod(acc, name, desc, sign, array)) != null) {
			return new MethodAdapter(visitMethod, this.className, name, desc);
		}
		return null;
	}
	
	public final void visit(final int n, final int n2, final String s, final String s2, String s3, final String[] array) {
		Main.addName(s3);
		super.visit(n, n2, s, s2, s3, array);
	}
	
	public final FieldVisitor visitField(final int n, final String s, String s2, final String s3, final Object o) {
		Main.addDesc(s2);
		return super.visitField(n, s, s2, s3, o);
	}
	
}