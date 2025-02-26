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

public class Main extends SignatureVisitor implements Runnable {

	static Main inst;
	
	private JFrame frame;
	private JTextField textField;
	private static JTextArea textArea;
	private static List<String> found;
	private static List<String> jarClasses;
	
	private StringBuilder sb = new StringBuilder();

	private boolean running;
	private static boolean externalOnly;

	Object target;
	private JCheckBox externalCheck;
	private JPanel panel_1;
	private JLabel label;

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
		Object target = this.target;
		if (target instanceof String) {
			process((String) target);
		} else if (target instanceof List) {
			for (Object e: (List) target) {
				if (e == null) continue;
				process(e.toString());
			}
		} else if (target instanceof String[]) {
			for (String s: (String[]) target) {
				process(s);
			}
		}
		running = false;
	}
	
	private void process(String t) {
		if ("-external".equalsIgnoreCase(t)) {
			externalOnly = true;
			return;
		}
		File f = new File(t);
		if (f.isDirectory()) {
			for (File s: f.listFiles()) {
				process(s);
			}
		} else {
			process(f);
		}
	}
	
	private void process(File f) {
		if (f.isDirectory()) {
			for (File s: f.listFiles()) {
				process(s);
			}
			return;
		}
		if (!f.isFile()) return;
		String n = f.getName().toLowerCase();
		if (!n.endsWith(".zip") && !n.endsWith(".jar")) return;

		log("File: " + f, true);
		
		try {
			found = new ArrayList<String>();
			jarClasses = new ArrayList<String>();
			
			try (ZipFile zipFile = new ZipFile(f)) {
				Enumeration<? extends ZipEntry> e = zipFile.entries();
				while (e.hasMoreElements()) {
					ZipEntry entry = e.nextElement();
					String s = entry.getName();
					if (!s.endsWith(".class")) continue;
					
					jarClasses.add(s.substring(0, s.length() - 6));
				}
				
				e = zipFile.entries();
				while (e.hasMoreElements()) {
					ZipEntry entry = e.nextElement();
					String s = entry.getName();
					if (!s.endsWith(".class")) continue;
					
					s = s.substring(0, s.length() - 6);
					ClassReader classReader = new ClassReader(zipFile.getInputStream(entry));
					ClassWriter classWriter = new ClassWriter(0);
					classReader.accept(new ClassAdapter(classWriter, s), ClassReader.SKIP_DEBUG);
				}
			}
			
			Collections.sort(found);
			for (String s: found) {
				log(s, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log("Error: " + e.toString(), false);
		}
		log("", true);
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
	}

	void initializeUI() {
		frame = new JFrame();
		frame.setTitle("Imports view");
		frame.setBounds(100, 100, 350, 536);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new GridLayout(0, 1, 0, 0));
		
		panel_1 = new JPanel();
		frame.getContentPane().add(panel_1);
		panel_1.setLayout(new BorderLayout(5, 5));
		
		JPanel panel = new JPanel();
		panel_1.add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));
		
		JButton openBtn = new JButton("Open");
		openBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (running) return;
				target = textField.getText();
				new Thread(Main.this).start();
			}
		});
		panel.add(openBtn, BorderLayout.EAST);
		
		textField = new JTextField();
		panel.add(textField, BorderLayout.CENTER);
		textField.setColumns(10);
		
		externalCheck = new JCheckBox("External only");
		externalCheck.setSelected(externalOnly = true);
		externalCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				externalOnly = externalCheck.isSelected();
			}
		});
		panel.add(externalCheck, BorderLayout.SOUTH);
		
		label = new JLabel("Jar or folder: ");
		panel.add(label, BorderLayout.WEST);
		
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
		if (found.contains(s)) return;
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
		if (externalOnly && jarClasses.contains(s)) return;
		found.add(s);
	}

}
