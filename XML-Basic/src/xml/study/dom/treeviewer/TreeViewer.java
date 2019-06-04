package xml.study.dom.treeviewer;

import java.awt.*;
import java.io.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.tree.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.w3c.dom.CharacterData;

/**
 * ��ȡXML�ļ���չʾ�����νṹ��С����
 */
public class TreeViewer {
	public static void main(String[] args) {
		EventQueue.invokeLater(() -> 
		{
			JFrame frame = new DOMTreeFrame();
			frame.setTitle("TreeViewer");			
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			localCenter(frame);
			frame.setVisible(true);					
		});
	}	
	
	// �ô�����ʾ����Ļ��������
	public static void localCenter(JFrame frame) {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();
		int screenWidth = screenSize.width;
		int screenHeight = screenSize.height;		
		
		int windowWidth = frame.getWidth(); //��ô��ڿ�
		int windowHeight = frame.getHeight(); //��ô��ڸ�
		
		int x = (screenWidth - windowWidth) / 2;
		int y = (screenHeight - windowHeight) / 2;
		frame.setLocation(x, y);		
	}
}

/**
 * �����ڰ�����һ����ʾXML�ĵ����ݵ���
 */
class DOMTreeFrame extends JFrame {
	private static final int DEFAULT_WIDTH = 400;
	private static final int DEFAULT_HEIGHT = 400;
	
	private DocumentBuilder builder;
	
	public DOMTreeFrame() {
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		
		JMenu fileMenu = new JMenu("File");
		JMenuItem openItem = new JMenuItem("Open");
		openItem.addActionListener(event -> openFile());
		fileMenu.add(openItem);
		
		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener(event -> System.exit(0));
		fileMenu.add(exitItem);
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		setJMenuBar(menuBar);
	}
	
	/**
	 * ���ļ��������ĵ�
	 */
	public void openFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File("F:\\mybatis\\rewriteSource\\mybatis-source-analysis\\XML-Basic\\src"));
		chooser.setFileFilter(new FileNameExtensionFilter("XML files", "xml"));
		int r = chooser.showOpenDialog(this);
		// ���û��ѡ���ļ�����������ֱ�ӷ���
		if (r != JFileChooser.APPROVE_OPTION)
			return ;
		final File file = chooser.getSelectedFile();
		
		new SwingWorker<Document, Void>() {
			@Override
			protected Document doInBackground() throws Exception {
				if (builder == null)
				{
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					builder = factory.newDocumentBuilder();
				}				
				return builder.parse(file);
			}
			
			@Override
			protected void done() {
				try {
					Document doc = get();
					JTree tree = new JTree(new DOMTreeModel(doc));
					tree.setCellRenderer(new DOMTreeCellRenderer());
					
					setContentPane(new JScrollPane(tree));
					validate();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(DOMTreeFrame.this, e);
				}
			}
		}.execute();
	}
}

/**
 * ����ģ��������XML�ĵ������νṹ
 */
class DOMTreeModel implements TreeModel {
	private Document doc;
	
	public DOMTreeModel(Document doc) {
		this.doc = doc;
	}
	
	@Override
	public Object getRoot() {
		return doc.getDocumentElement();
	}

	@Override
	public Object getChild(Object parent, int index) {
		Node node = (Node) parent;
		NodeList list = node.getChildNodes();
		return list.item(index);
	}

	@Override
	public int getChildCount(Object parent) {
		Node node = (Node) parent;
		NodeList list = node.getChildNodes();
		return list.getLength();
	}

	@Override
	public boolean isLeaf(Object node) {
		return getChildCount(node) == 0;
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		Node node = (Node) parent;
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			if (getChild(node, i) == child)
				return i;
		}
		return -1;
	}	
	
	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {}

	@Override
	public void addTreeModelListener(TreeModelListener l) {}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {}
}

/**
 * ����������Ⱦһ��XML�ڵ�
 */
class DOMTreeCellRenderer extends DefaultTreeCellRenderer {
	@Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
										          boolean sel,
										          boolean expanded,
										          boolean leaf, int row,
										          boolean hasFocus) {
		Node node = (Node) value;
		if (node instanceof Element)
			return elementPanel((Element) node);
    	
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		if (node instanceof CharacterData)
			setText(characterString((CharacterData) node));
		else 
			setText(node.getClass() + ":" + node.toString());
		return this;
    }
	
	public static JPanel elementPanel(Element e) {
		final NamedNodeMap map = e.getAttributes();
		
		JPanel panel = new JPanel();
		panel.add(new JLabel("Element: " + e.getTagName()));
		panel.add(new JTable(new AbstractTableModel() {
			
			// ��rowIndex + 1�У���columnIndex + 1��Ҫ��ʾʲô����
			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				return columnIndex == 0 ? map.item(rowIndex).getNodeName() : map.item(rowIndex).getNodeValue();
			}
			
			// ��ʾ���JTableҪռ������
			@Override
			public int getRowCount() {
				return map.getLength();
			}
			
			
			// ��ʾ���JTableҪռ������
			@Override
			public int getColumnCount() {
				return 2;
			}
		}));
		return panel;
	}
	
	private static String characterString(CharacterData node) {
		StringBuilder builder = new StringBuilder(node.getData());
		for (int i = 0; i < builder.length(); i++) {
			if (builder.charAt(i) == '\r') {
				builder.replace(i, i + 1, "\\r");
				i++;   // ��һ���ַ��滻���˸�����������Ҫ+1
			}
			else if (builder.charAt(i) == '\n') {
				builder.replace(i, i + 1, "\\n");
				i++;
			}
			else if (builder.charAt(i) == '\t') {
				builder.replace(i, i + 1, "\\t");
				i++;
			}
		}
		// �ڿ�ͷ����ڵ�����
		if (node instanceof CDATASection) builder.insert(0, "CDATASection: ");
		else if (node instanceof Text)    builder.insert(0, "Text: ");
		else if (node instanceof Comment) builder.insert(0, "Comment: ");
		return builder.toString();
	}
}