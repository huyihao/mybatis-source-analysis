package dom;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.event.TreeModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.CDATASection;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class TreeViewer {
	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			JFrame frame = new DOMTreeFrame();
			frame.setTitle("TreeViewer");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			locateCenter(frame);
			frame.setVisible(true);			
		});		
	}
	
	public static void locateCenter(JFrame frame) {
		int frameWidth = frame.getWidth();
		int frameHeight = frame.getHeight();
		
		Toolkit kit = Toolkit.getDefaultToolkit();
		Dimension screen = kit.getScreenSize();
		int screenWidth = screen.width;
		int screenHeight = screen.height;
		
		int x = (screenWidth - frameWidth) / 2;
		int y = (screenHeight - frameHeight) / 2;
		frame.setLocation(x, y);
	}
}

class DOMTreeFrame extends JFrame {
	private static final int DEFAULT_WIDTH = 400;
	private static final int DEFAULT_HEIGHT = 400;
	
	private DocumentBuilder builder;
	
	public DOMTreeFrame() {
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		
		JMenu jmenu = new JMenu("File");
		JMenuItem openItem = new JMenuItem("open");
		openItem.addActionListener(event -> open());
		jmenu.add(openItem);
		
		JMenuItem exitItem = new JMenuItem("exit");
		exitItem.addActionListener(event -> System.exit(0));
		jmenu.add(exitItem);
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(jmenu);
		setJMenuBar(menuBar);
	}
	
	public void open() {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File("F:\\mybatis\\rewriteSource\\mybatis-source-analysis\\XML-Basic\\src"));
		chooser.setFileFilter(new FileNameExtensionFilter("XML Files", "xml"));
		int r = chooser.showOpenDialog(this);
		if (r != JFileChooser.APPROVE_OPTION) return;
		
		final File file = chooser.getSelectedFile();
		new SwingWorker<Document, Void>() {

			@Override
			protected Document doInBackground() throws Exception {
				if (builder == null) {
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
		return getChildCount(node) == 0 ? true : false;
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		Node node = (Node) parent;
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			if (getChild(node, i) == child) return i;
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

class DOMTreeCellRenderer extends DefaultTreeCellRenderer {
	@Override
    public Component getTreeCellRendererComponent(JTree tree, 
    											  Object value,
									              boolean sel,
									              boolean expanded,
									              boolean leaf, 
									              int row,
									              boolean hasFocus) {
		Node node = (Node) value;
		if (node instanceof Element)
			return elementPanel((Element) node);
		
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		if (node instanceof CharacterData) 
			setText(characterString((CharacterData)node));
		else
			setText(node.getClass() + ":" + node.toString());
		return this;
    }
	
	public static JPanel elementPanel(Element e) {
		final NamedNodeMap map = e.getAttributes();
		
		JPanel panel = new JPanel();
		panel.add(new JLabel("Element: " + e.getTagName()));
		panel.add(new JTable(new AbstractTableModel() {
			
			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				return columnIndex == 0 ? map.item(rowIndex).getNodeName() : map.item(rowIndex).getNodeValue();
			}
			
			@Override
			public int getRowCount() {				
				return map.getLength();
			}
			
			@Override
			public int getColumnCount() {
				return 2;
			}
		}));
		
		return panel;
	}
	
	public static String characterString(CharacterData node) {
		StringBuilder builder = new StringBuilder(node.getData());
		for (int i = 0; i < builder.length(); i++) {
			if (builder.charAt(i) == '\r') {
				builder.replace(i, i + 1, "\\r");
				i++;
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
		
		if (node instanceof CDATASection)    builder.insert(0, "CDATASection: ");
		else if (node instanceof Text)       builder.insert(0, "Text: ");
		else if (node instanceof Comment)    builder.insert(0, "Comment: ");
		
		return builder.toString();
	}
}
