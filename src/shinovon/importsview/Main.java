package shinovon.importsview;

import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import javax.swing.JFrame;
import javax.swing.JTextField;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.awt.event.ActionEvent;
import javax.swing.JCheckBox;
import java.awt.GridLayout;
import javax.swing.JLabel;
import java.awt.FlowLayout;

public class Main extends SignatureVisitor implements Runnable {

	static Main inst;
	
	private JFrame frame;
	private JTextField textField;
	private JTextField refField;
	private static JTextArea textArea;
	private static List<String> found;
	private static List<String> jarClasses;
	
	private static List<String> knownClasses;
	private static List<String> known;
	
	private StringBuilder sb = new StringBuilder();

	private boolean running;
	
	private static boolean externalOnly;
	private static boolean global;
	private static boolean methods;
	private static boolean fields;

	Object target;
	static String reference;
	private JCheckBox externalCheck;
	private JCheckBox globalCheck;
	private JCheckBox methodsCheck;
	private JCheckBox fieldsCheck;

	public static void main(String[] args) {
		if (args.length > 0) {
			inst = new Main();
			inst.target = args;
			new Thread(inst).start();
			return;
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					inst = new Main();
					inst.initializeUI();
					inst.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public Main() {
		super(Opcodes.ASM4);
	}
	
	public void run() {
		running = true;
		clear();
		known = null;

		if (reference != null && reference.trim().length() != 0) {
			process(reference, true);
//			known = found; // TODO
		}
		reference = null;
		
		Object target = this.target;
		if (target instanceof String) {
			process((String) target, false);
		} else if (target instanceof List) {
			for (Object e: (List) target) {
				if (e == null) continue;
				process(e.toString(), false);
			}
		} else if (target instanceof String[]) {
			for (String s: (String[]) target) {
				process(s, false);
			}
		}
		if (global) {
			Collections.sort(found);
			for (String s: found) {
				log(s, false);
			}
			log("", true);
		}
		log("Done", true);
		running = false;
	}
	
	private void process(String t, boolean ref) {
		if ("-external".equalsIgnoreCase(t)) {
			externalOnly = true;
			return;
		}
		if ("-global".equalsIgnoreCase(t)) {
			found = new ArrayList<String>();
			global = true;
			return;
		}
		if ("-methods".equalsIgnoreCase(t)) {
			methods = true;
			return;
		}
		if ("-fields".equalsIgnoreCase(t)) {
			fields = true;
			return;
		}
		File f = new File(t);
		if (f.isDirectory()) {
			for (File s: f.listFiles()) {
				process(s, ref);
			}
		} else {
			process(f, ref);
		}
	}
	
	private void process(File f, boolean ref) {
		if (f.isDirectory()) {
			for (File s: f.listFiles()) {
				process(s, ref);
			}
			return;
		}
		if (!f.isFile()) return;
		String n = f.getName().toLowerCase();
		if (!n.endsWith(".zip") && !n.endsWith(".jar")) return;

		if (!global) log("File: " + f, true);
		
		try {
			if (ref) {
				found = new ArrayList<String>();
				knownClasses = new ArrayList<String>();
			} else {
				if (!global) found = new ArrayList<String>();
				jarClasses = new ArrayList<String>();
			}
			
			try (ZipFile zipFile = new ZipFile(f)) {
				Enumeration<? extends ZipEntry> e = zipFile.entries();
				while (e.hasMoreElements()) {
					ZipEntry entry = e.nextElement();
					String s = entry.getName();
					if (!s.endsWith(".class")) continue;
					if (ref) {
						knownClasses.add(s.substring(0, s.length() - 6));
					} else {
						jarClasses.add(s.substring(0, s.length() - 6));
					}
				}
				
				e = zipFile.entries();
				while (e.hasMoreElements()) {
					ZipEntry entry = e.nextElement();
					String s = entry.getName();
					if (!s.endsWith(".class")) continue;
					
					s = s.substring(0, s.length() - 6);
					if (s.length() == 0) {
						log("Invalid class name: " + s, true);
						continue;
					}
					try {
						ClassReader classReader = new ClassReader(zipFile.getInputStream(entry));
						ClassWriter classWriter = new ClassWriter(0);
						classReader.accept(new ClassAdapter(classWriter, s), ClassReader.SKIP_DEBUG);
					} catch (Exception ex) {
						ex.printStackTrace();
						log("Error: " + ex.toString() + ", File: " + f + ":" + s + ".class", true);
					}
				}
			}
			
			if (!global && !ref) {
				Collections.sort(found);
				for (String s: found) {
					log(s, false);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (global || ref) {
				log("Error: " + e.toString() + ", File: " + f, true);
			} else {
				log("Error: " + e.toString(), false);
			}
		}
		if (!global && !ref) log("", true);
	}
	
	void log(String s, boolean b) {
		System.out.println(s);
		if (textArea == null) return;
		sb.append(s).append('\n');
		if (b) textArea.setText(sb.toString());
	}
	
	void clear() {
		if (textArea == null) return;
		sb.setLength(0);
		textArea.setText("");
		if (global) {
			found = new ArrayList<String>();
		}
	}

	void initializeUI() {
		frame = new JFrame();
		frame.setTitle("Imports view v2.6");
		frame.setBounds(100, 100, 350, 536);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new GridLayout(0, 1, 0, 0));
		
		JPanel panel_1 = new JPanel();
		frame.getContentPane().add(panel_1);
		panel_1.setLayout(new BorderLayout(5, 5));
		
		JPanel panel = new JPanel();
		panel_1.add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_2 = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel_2.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		panel.add(panel_2, BorderLayout.SOUTH);
		
		externalCheck = new JCheckBox("External only");
		panel_2.add(externalCheck);
		externalCheck.setSelected(externalOnly = true);
		
		externalCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				externalOnly = externalCheck.isSelected();
			}
		});
		
		globalCheck = new JCheckBox("Global");
		panel_2.add(globalCheck);
		globalCheck.setSelected(global);
		globalCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				global = globalCheck.isSelected();
			}
		});
		
		methodsCheck = new JCheckBox("Methods");
		panel_2.add(methodsCheck);
		methodsCheck.setSelected(methods);
		methodsCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				methods = methodsCheck.isSelected();
			}
		});
		
		fieldsCheck = new JCheckBox("Fields");
		panel_2.add(fieldsCheck);
		fieldsCheck.setSelected(fields);
		fieldsCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fields = fieldsCheck.isSelected();
			}
		});
		
		JPanel panel_3 = new JPanel();
		panel.add(panel_3, BorderLayout.NORTH);
		panel_3.setLayout(new BorderLayout(0, 0));
		
		JLabel label = new JLabel("Jar or folder: ");
		panel_3.add(label, BorderLayout.WEST);
		
		textField = new JTextField();
		panel_3.add(textField, BorderLayout.CENTER);
		textField.setColumns(10);
		
		JButton openBtn = new JButton("Open");
		panel_3.add(openBtn, BorderLayout.EAST);
		
		JPanel panel_4 = new JPanel();
		panel.add(panel_4, BorderLayout.CENTER);
		panel_4.setLayout(new BorderLayout(0, 0));
		
		JLabel lblNewLabel = new JLabel("Reference jar: ");
		panel_4.add(lblNewLabel, BorderLayout.WEST);
		
		refField = new JTextField();
		panel_4.add(refField, BorderLayout.CENTER);
		refField.setColumns(10);
		openBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (running) return;
				target = textField.getText();
				reference = refField.getText();
				new Thread(Main.this).start();
			}
		});
		
		JScrollPane scrollPane = new JScrollPane();
		panel_1.add(scrollPane, BorderLayout.CENTER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setText("Drop files here");
		scrollPane.setViewportView(textArea);
		
		DropTarget dropTarget = new DropTarget(textArea, DnDConstants.ACTION_COPY_OR_MOVE, null);
		try {
			dropTarget.addDropTargetListener(new DropTargetListener() {

				@Override
				public void dragEnter(DropTargetDragEvent dtde) {
				}

				@Override
				public void dragOver(DropTargetDragEvent dtde) {
				}

				@Override
				public void dropActionChanged(DropTargetDragEvent dtde) {
				}

				@Override
				public void dragExit(DropTargetEvent dte) {
				}

				@Override
				public void drop(DropTargetDropEvent dtde) {
					if (running) return;
					try {
						Transferable t = dtde.getTransferable();
						if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
							dtde.acceptDrop(dtde.getDropAction());
							List transferData = (List) t.getTransferData(DataFlavor.javaFileListFlavor);
							
	                        if (transferData != null && transferData.size() > 0) {
	                        	target = transferData;
	                            dtde.dropComplete(true);
	            				new Thread(Main.this).start();
	                        }
						} else {
		                    dtde.rejectDrop();
		                }
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addName(String s) {
		if (found.contains(s) || s == null) return;
		if (s.startsWith("[")) {
			addDesc(s);
			return;
		}
		add(s);
	}

	public static void addDesc(String s) {
		new SignatureReader(s).accept(inst);
	}
	
	public void visitFormalTypeParameter(String s) {
		if (found.contains(s)) return;
		add(s);
	}
	
	public void visitTypeVariable(String s) {
		if (found.contains(s)) return;
		add(s);
	}
	
	public void visitClassType(String s) {
		if (found.contains(s)) return;
		add(s);
	}

	public void visitInnerClassType(String s) {
		if (found.contains(s)) return;
		add(s);
	}
	
	private static void add(String s) {
		while (s.startsWith("[")) {
			s = s.substring(1);
		}
		if (found.contains(s)) return;
		if (externalOnly && jarClasses != null && jarClasses.contains(s)) return;
		if (knownClasses != null && knownClasses.contains(s)) return;
		if (known != null && known.contains(s)) return;
		found.add(s);
	}

	public static void addMethod(int op, String cls, String name, String sign) {
		if (externalOnly && jarClasses != null && jarClasses.contains(cls)) return;
		addName(cls);
		addDesc(sign);
		
		if (!methods) return;
		String s;
		if (op == Opcodes.INVOKEVIRTUAL) {
			s = "virtual";
		} else if (op == Opcodes.INVOKESPECIAL) {
			s = "special";
		} else if (op == Opcodes.INVOKESTATIC) {
			s = "static";
		} else if (op == Opcodes.INVOKEINTERFACE) {
			s = "interface";
		} else {
			s = "";
		}
		s = cls+" "+name+sign + " " + s;
		if (found.contains(s)) return;
		if (known != null && known.contains(s)) return;
		found.add(s);
	}

	public static void addField(int op, String cls, String name, String sign) {
		if (externalOnly && jarClasses != null && jarClasses.contains(cls)) return;
		addName(cls);
		addDesc(sign);
		
		if (!fields) return;
		String s;
		if (op == Opcodes.GETSTATIC) {
			s = "getstatic";
		} else if (op == Opcodes.PUTSTATIC) {
			s = "putstatic";
		} else if (op == Opcodes.GETFIELD) {
			s = "getfield";
		} else if (op == Opcodes.PUTFIELD) {
			s = "putfield";
		} else {
			s = "";
		}
		s = cls+" "+sign + " " + name + " " + s;
		if (found.contains(s)) return;
		if (known != null && known.contains(s)) return;
		found.add(s);
	}

}
