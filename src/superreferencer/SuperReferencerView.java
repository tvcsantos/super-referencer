/*
 * SuperReferencerView.java
 */
package superreferencer;

import bibtex.dom.BibtexAbstractEntry;
import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexEntry;
import bibtex.dom.BibtexFile;
import bibtex.dom.BibtexString;
import bibtex.parser.BibtexParser;
import bibtex.parser.ParseException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import org.jdesktop.application.Action;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import javax.swing.TransferHandler;
import net.sourceforge.tuned.FileUtilities;
import pt.unl.fct.di.tsantos.util.app.AppUtils;
import pt.unl.fct.di.tsantos.util.app.DefaultTrayedFrameView;
import pt.unl.fct.di.tsantos.util.bibtex.BibtexUtilities;
import pt.unl.fct.di.tsantos.util.string.StringUtils;
import superreferencer.SuperReferencerApp.BibDocuments.BibDocument;

/**
 * The application's main frame.
 */
public class SuperReferencerView extends
        DefaultTrayedFrameView<SuperReferencerApp> {

    private static String DOCUMENT_TEMPLATE = "%d documents (%d selected)";
    private DropTarget dropTarget;
    private EfficientListModel docListModel;
    private BibDocumentListCellRenderer docListRenderer;
    private DefaultListModel tagListModel;
    private DefaultListModel docTagListModel;
    private JPopupMenu popupMenu;
    private static String ALL_TAG = "All";
    private static String UNTAGGED_TAG = "Untagged";
    private static String UNATTACHED_TAG = "Unattached";
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private JFileChooser fileChooser = new JFileChooser();
    private List<SuperReferencerApp.BibDocumentComparator> comparatorList;
    private CyclicIterator<SuperReferencerApp.BibDocumentComparator>
            comparatorIterator;
    private int count = 0;
    private int selected = 0;

    static interface ResetableIterator<E> extends Iterator<E> {
        public void reset();
    }
    
    static class CyclicIterator<E> implements Iterator<E> {
        protected Iterable<E> iterable;
        protected Iterator<E> iterator;
        
        public CyclicIterator(Iterable<E> iterable) {
            this.iterable = iterable;
            this.iterator = iterable.iterator();
        }

        public boolean hasNext() { return true; }

        public E next() {
            if (!this.iterator.hasNext())
                this.iterator = this.iterable.iterator();
            return this.iterator.next();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }

    public SuperReferencerView(SuperReferencerApp app) {
        super(app);
        initComponents();
        
        docListModel = new EfficientListModel();
        docListRenderer = new BibDocumentListCellRenderer();
        docListRenderer.setKeyFont(
                getTheApplication().getDocumentsKeyFont());
        docListRenderer.setTitleFont(
                getTheApplication().getDocumentsTitleFont());
        docListRenderer.setYearFont(
                getTheApplication().getDocumentsYearFont());
        docListRenderer.setAuthorFont(
                getTheApplication().getDocumentsAuthorFont());
        docListRenderer.setKeyColor(
                getTheApplication().getDocumentsKeyColor());
        docListRenderer.setTitleColor(
                getTheApplication().getDocumentsTitleColor());
        docListRenderer.setYearColor(
                getTheApplication().getDocumentsYearColor());
        docListRenderer.setAuthorColor(
                getTheApplication().getDocumentsAuthorColor());
        docList.setCellRenderer(docListRenderer);
        docList.setModel(docListModel);
        dropTarget = new DropTarget(docList, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                docListDropTargetDrop(dtde);
            }
        });
        docList.setDragEnabled(true);
        docList.setTransferHandler(new FileTransferHandler());
        /*docList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        docList.setVisibleRowCount(-1);*/
        comparatorList = 
                new LinkedList<SuperReferencerApp.BibDocumentComparator>();
        comparatorList.add(
                new SuperReferencerApp.AlphabeticFieldComparator("title"));
        comparatorList.add(
                new SuperReferencerApp.AlphabeticFieldComparator("author"));
        comparatorList.add(new SuperReferencerApp.KeyComparator());
        comparatorIterator =
                new CyclicIterator<SuperReferencerApp.BibDocumentComparator>(
                comparatorList);

        fillList(getTheApplication().getDocuments());

        tagListModel = new DefaultListModel();
        tagList.setModel(tagListModel);
        class DelegateListCellRenderer implements ListCellRenderer {
            protected ListCellRenderer renderer;

            public DelegateListCellRenderer(ListCellRenderer renderer) {
                this.renderer = renderer;
            }

            public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                return renderer.getListCellRendererComponent(list,
                        value, index, isSelected, cellHasFocus);
            }   
        }
        tagList.setCellRenderer(
                new DelegateListCellRenderer(tagList.getCellRenderer()) {
            @Override
            public Component getListCellRendererComponent(JList list, 
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (label.getHorizontalAlignment() != JLabel.CENTER)
                    label.setHorizontalAlignment(JLabel.CENTER);
                return label;
            } 
        });
        tagListModel.addElement(ALL_TAG);
        tagListModel.addElement(UNTAGGED_TAG);
        tagListModel.addElement(UNATTACHED_TAG);
        for (String tag : getTheApplication().getTags())
            tagListModel.addElement(tag);

        docTagListModel = new DefaultListModel();
        docTagList.setModel(docTagListModel);

        popupMenu = new JPopupMenu();
        JMenuItem open = new JMenuItem("Open");
        open.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    BibDocument doc = getDocSelectedValue();
                    if (doc == null || doc.getFile() == null) return;
                    java.awt.Desktop.getDesktop().open(doc.getFile());
                } catch (IOException ex) {
                    Logger.getLogger(SuperReferencerView.class.getName()).log(
                            Level.SEVERE, null, ex);
                }
            }
        });
        JMenuItem addTag = new JMenuItem("Add Tag");
        addTag.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BibDocument doc = getDocSelectedValue();
                if (doc == null) return;
                addTagDialog(doc);
            }
        });
        JMenuItem remove = new JMenuItem("Remove");
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BibDocument doc = getDocSelectedValue();
                if (doc == null) return;
                removeDocument(doc);
            }
        });
        JMenuItem properties = new JMenuItem("Properties");
        properties.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showSelectedItem();
            }
        });
        popupMenu.add(open);
        popupMenu.add(addTag);
        popupMenu.add(remove);
        popupMenu.add(properties);

        databaseLabel.setText(String.format(DOCUMENT_TEMPLATE,
                count = getTheApplication().getDocuments().size(), selected));
    }

    private void fillList(Collection<BibDocument> list) {
        if (!(list instanceof SortedSet)) {
            SortedSet<BibDocument> unsorted = new TreeSet<BibDocument>(
                    getTheApplication().getComparator());
            unsorted.addAll(list);
            //Collections.sort(unsorted, getTheApplication().getComparator());
            list = unsorted;
        }
        //Collections.sort(docs, getTheApplication().getComparator());
        docListModel.removeAllElements();
        docListModel.addElements(list);
        //for (BibDocument d : list) {
            //docListModel.addElement(d);
        //}
        docList.repaint();
    }

    private void fillDocTagList(BibDocument... docs) {
        docTagListModel.removeAllElements();
        Set<String> res = new HashSet<String>();
        if (docs != null)
            for (BibDocument doc : docs)
                if (doc != null)
                    res.addAll(doc.getTags());
        for (String tag : res)
            docTagListModel.addElement(tag);
        docTagList.repaint();
    }

    @Action
    public void showDocumentProperties(BibDocument doc) {
        JFrame mainFrame =
                    SuperReferencerApp.getApplication().getMainFrame();
        /*FrameView view =
                SuperReferencerApp.getApplication().getMainView();*/
        if (docProperties == null) {
            docProperties = new DocumentPropertiesDialog(this, true);
            docProperties.addPropertyChangeListener("documentChanged",
                    new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("documentChanged")) {
                        BibDocument doc = (BibDocument) evt.getNewValue();
                        changedItemLayout(doc);
                    }
                }
            });
        }
        docProperties.setDocument(doc);
        docProperties.setLocationRelativeTo(mainFrame);
        docProperties.pack();
        SuperReferencerApp.getApplication().show(docProperties);
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame =
                    SuperReferencerApp.getApplication().getMainFrame();
            aboutBox = new SuperReferencerAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        SuperReferencerApp.getApplication().show(aboutBox);
    }

    private void addTagDialog(BibDocument ... docs) {
        String message = JOptionPane.showInputDialog(getFrame(),
                "New Tag name:");
        if (message != null) {
            if (!message.isEmpty()) {
                if (docs != null) addTag(message, docs);
                else addTag(message);
            }
        }
    }

    private void addTag(String tag) {
        boolean added = getTheApplication().addTag(tag);
        if (added) tagListModel.addElement(tag);
    }

    private void addTag(String tag, BibDocument ... docs) {
        if (!getTheApplication().hasTag(tag)) tagListModel.addElement(tag);
        for (BibDocument doc : docs)
            getTheApplication().addTag(doc, tag);
        fillDocTagList(docs);
    }

    private void removeTag(String tag) {
        boolean removed = getTheApplication().removeTag(tag);
        if (removed) tagListModel.removeElement(tag);
    }

    private void removeTag(String tag, BibDocument ... docs) {
        for (BibDocument doc : docs)
            getTheApplication().removeTag(doc, tag);
        fillDocTagList(docs);
        /* stupid but necessary to force refreshing the listing
         * of documents with the tag
         */
        tagList.setSelectedValue(ALL_TAG, true);
        tagList.setSelectedValue(tag, true);
    }

    private void showSelectedItem() {
        BibDocument doc = getDocSelectedValue();
        if (doc != null) {
            showDocumentProperties(doc);
        }
    }

    private BibDocument getDocSelectedValue() {
        return (BibDocument) docList.getSelectedValue();
    }

    private BibDocument[] getDocSelectedValues() {
        Object[] array = docList.getSelectedValues();
        BibDocument[] copyOf =
                Arrays.copyOf(array, array.length,
                new BibDocument[]{}.getClass());
        return copyOf;
    }

    private String getDocTagSelectedValue() {
        return (String) docTagList.getSelectedValue();
    }

    private String getTagSelectedValue() {
        return (String) tagList.getSelectedValue();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        tagScrollPane = new javax.swing.JScrollPane();
        tagList = new javax.swing.JList();
        docScrollPane = new javax.swing.JScrollPane();
        docList = new javax.swing.JList();
        docTagScrollPane = new javax.swing.JScrollPane();
        docTagList = new javax.swing.JList();
        jToolBar1 = new javax.swing.JToolBar();
        newEntryButton = new javax.swing.JButton();
        importBibButton = new javax.swing.JButton();
        exportBibButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        addTagButton = new javax.swing.JButton();
        deleteTagButton = new javax.swing.JButton();
        fontShapeButton = new javax.swing.JButton();
        fontColorButton = new javax.swing.JButton();
        sortButton = new javax.swing.JButton();
        jToolBar2 = new javax.swing.JToolBar();
        addDocTagButton = new javax.swing.JButton();
        deleteDocTagButton = new javax.swing.JButton();
        searchTextField = new pt.unl.fct.di.tsantos.util.swing.JSearchTextField();
        databaseLabel = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        exportBibMenuItem = new javax.swing.JMenuItem();
        importBibMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        extraMenu = new javax.swing.JMenu();
        settingsMenu = new javax.swing.JMenu();
        fontShapeMenuItem = new javax.swing.JMenuItem();
        fontColorMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        fontDialog = new javax.swing.JDialog();
        fontChooser = new say.swing.JFontChooser();
        jToolBar3 = new javax.swing.JToolBar();
        keyToggleButton = new javax.swing.JToggleButton();
        titleToggleButton = new javax.swing.JToggleButton();
        yearToggleButton = new javax.swing.JToggleButton();
        authorToggleButton = new javax.swing.JToggleButton();
        saveFontButton = new javax.swing.JButton();
        cancelFontButton = new javax.swing.JButton();
        buttonGroup1 = new javax.swing.ButtonGroup();
        fontColorDialog = new javax.swing.JDialog();
        colorChooser = new javax.swing.JColorChooser();
        jToolBar4 = new javax.swing.JToolBar();
        keyColorToggleButton = new javax.swing.JToggleButton();
        titleColorToggleButton = new javax.swing.JToggleButton();
        yearColorToggleButton = new javax.swing.JToggleButton();
        authorColorToggleButton = new javax.swing.JToggleButton();
        saveColorFontButton = new javax.swing.JButton();
        cancelColorFontButton = new javax.swing.JButton();
        buttonGroup2 = new javax.swing.ButtonGroup();

        mainPanel.setName("mainPanel"); // NOI18N

        tagScrollPane.setName("tagScrollPane"); // NOI18N

        tagList.setName("tagList"); // NOI18N
        tagList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tagListMouseClicked(evt);
            }
        });
        tagList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                tagListValueChanged(evt);
            }
        });
        tagScrollPane.setViewportView(tagList);

        docScrollPane.setName("docScrollPane"); // NOI18N

        docList.setLayoutOrientation(javax.swing.JList.HORIZONTAL_WRAP);
        docList.setName("docList"); // NOI18N
        docList.setVisibleRowCount(-1);
        docList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                docListMouseClicked(evt);
            }
        });
        docList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                docListValueChanged(evt);
            }
        });
        docList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                docListKeyReleased(evt);
            }
        });
        docScrollPane.setViewportView(docList);

        docTagScrollPane.setName("docTagScrollPane"); // NOI18N

        docTagList.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        docTagList.setLayoutOrientation(javax.swing.JList.HORIZONTAL_WRAP);
        docTagList.setName("docTagList"); // NOI18N
        docTagList.setVisibleRowCount(-1);
        docTagScrollPane.setViewportView(docTagList);

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);
        jToolBar1.setName("jToolBar1"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(superreferencer.SuperReferencerApp.class).getContext().getResourceMap(SuperReferencerView.class);
        newEntryButton.setIcon(resourceMap.getIcon("newEntryButton.icon")); // NOI18N
        newEntryButton.setText(resourceMap.getString("newEntryButton.text")); // NOI18N
        newEntryButton.setToolTipText(resourceMap.getString("newEntryButton.toolTipText")); // NOI18N
        newEntryButton.setFocusable(false);
        newEntryButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        newEntryButton.setName("newEntryButton"); // NOI18N
        newEntryButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        newEntryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newEntryButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(newEntryButton);

        importBibButton.setIcon(resourceMap.getIcon("importBibButton.icon")); // NOI18N
        importBibButton.setText(resourceMap.getString("importBibButton.text")); // NOI18N
        importBibButton.setToolTipText(resourceMap.getString("importBibButton.toolTipText")); // NOI18N
        importBibButton.setFocusable(false);
        importBibButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        importBibButton.setName("importBibButton"); // NOI18N
        importBibButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        importBibButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importBibButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(importBibButton);

        exportBibButton.setIcon(resourceMap.getIcon("exportBibButton.icon")); // NOI18N
        exportBibButton.setText(resourceMap.getString("exportBibButton.text")); // NOI18N
        exportBibButton.setToolTipText(resourceMap.getString("exportBibButton.toolTipText")); // NOI18N
        exportBibButton.setFocusable(false);
        exportBibButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        exportBibButton.setName("exportBibButton"); // NOI18N
        exportBibButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        exportBibButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportBibButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(exportBibButton);

        saveButton.setIcon(resourceMap.getIcon("saveButton.icon")); // NOI18N
        saveButton.setText(resourceMap.getString("saveButton.text")); // NOI18N
        saveButton.setToolTipText(resourceMap.getString("saveButton.toolTipText")); // NOI18N
        saveButton.setFocusable(false);
        saveButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        saveButton.setName("saveButton"); // NOI18N
        saveButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(saveButton);

        addTagButton.setIcon(resourceMap.getIcon("addTagButton.icon")); // NOI18N
        addTagButton.setText(resourceMap.getString("addTagButton.text")); // NOI18N
        addTagButton.setToolTipText(resourceMap.getString("addTagButton.toolTipText")); // NOI18N
        addTagButton.setFocusable(false);
        addTagButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addTagButton.setName("addTagButton"); // NOI18N
        addTagButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addTagButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addTagButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(addTagButton);

        deleteTagButton.setIcon(resourceMap.getIcon("deleteTagButton.icon")); // NOI18N
        deleteTagButton.setText(resourceMap.getString("deleteTagButton.text")); // NOI18N
        deleteTagButton.setToolTipText(resourceMap.getString("deleteTagButton.toolTipText")); // NOI18N
        deleteTagButton.setFocusable(false);
        deleteTagButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        deleteTagButton.setName("deleteTagButton"); // NOI18N
        deleteTagButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        deleteTagButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteTagButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(deleteTagButton);

        fontShapeButton.setIcon(resourceMap.getIcon("fontShapeButton.icon")); // NOI18N
        fontShapeButton.setText(resourceMap.getString("fontShapeButton.text")); // NOI18N
        fontShapeButton.setToolTipText(resourceMap.getString("fontShapeButton.toolTipText")); // NOI18N
        fontShapeButton.setFocusable(false);
        fontShapeButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        fontShapeButton.setName("fontShapeButton"); // NOI18N
        fontShapeButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        fontShapeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fontShapeButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(fontShapeButton);

        fontColorButton.setIcon(resourceMap.getIcon("fontColorButton.icon")); // NOI18N
        fontColorButton.setText(resourceMap.getString("fontColorButton.text")); // NOI18N
        fontColorButton.setToolTipText(resourceMap.getString("fontColorButton.toolTipText")); // NOI18N
        fontColorButton.setFocusable(false);
        fontColorButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        fontColorButton.setName("fontColorButton"); // NOI18N
        fontColorButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        fontColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fontColorButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(fontColorButton);

        sortButton.setIcon(resourceMap.getIcon("sortButton.icon")); // NOI18N
        sortButton.setText(resourceMap.getString("sortButton.text")); // NOI18N
        sortButton.setFocusable(false);
        sortButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        sortButton.setName("sortButton"); // NOI18N
        sortButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        sortButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sortButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(sortButton);

        jToolBar2.setFloatable(false);
        jToolBar2.setOrientation(1);
        jToolBar2.setRollover(true);
        jToolBar2.setName("jToolBar2"); // NOI18N

        addDocTagButton.setIcon(resourceMap.getIcon("addDocTagButton.icon")); // NOI18N
        addDocTagButton.setText(resourceMap.getString("addDocTagButton.text")); // NOI18N
        addDocTagButton.setToolTipText(resourceMap.getString("addDocTagButton.toolTipText")); // NOI18N
        addDocTagButton.setFocusable(false);
        addDocTagButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addDocTagButton.setName("addDocTagButton"); // NOI18N
        addDocTagButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addDocTagButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDocTagButtonActionPerformed(evt);
            }
        });
        jToolBar2.add(addDocTagButton);

        deleteDocTagButton.setIcon(resourceMap.getIcon("deleteDocTagButton.icon")); // NOI18N
        deleteDocTagButton.setText(resourceMap.getString("deleteDocTagButton.text")); // NOI18N
        deleteDocTagButton.setToolTipText(resourceMap.getString("deleteDocTagButton.toolTipText")); // NOI18N
        deleteDocTagButton.setFocusable(false);
        deleteDocTagButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        deleteDocTagButton.setName("deleteDocTagButton"); // NOI18N
        deleteDocTagButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        deleteDocTagButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteDocTagButtonActionPerformed(evt);
            }
        });
        jToolBar2.add(deleteDocTagButton);

        searchTextField.setText(resourceMap.getString("searchTextField.text")); // NOI18N
        searchTextField.setIcon(resourceMap.getIcon("searchTextField.icon")); // NOI18N
        searchTextField.setName("searchTextField"); // NOI18N
        searchTextField.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                searchTextFieldCaretUpdate(evt);
            }
        });
        searchTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchTextFieldActionPerformed(evt);
            }
        });

        databaseLabel.setIcon(resourceMap.getIcon("databaseLabel.icon")); // NOI18N
        databaseLabel.setText(resourceMap.getString("databaseLabel.text")); // NOI18N
        databaseLabel.setName("databaseLabel"); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 260, Short.MAX_VALUE)
                        .addComponent(searchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(tagScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addComponent(jToolBar2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(docTagScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 449, Short.MAX_VALUE))
                            .addComponent(docScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE)))
                    .addComponent(databaseLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(searchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(docScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 234, Short.MAX_VALUE)
                        .addGap(11, 11, 11)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(docTagScrollPane)
                            .addComponent(jToolBar2, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(tagScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 293, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(databaseLabel)
                .addContainerGap())
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        exportBibMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
        exportBibMenuItem.setText(resourceMap.getString("exportBibMenuItem.text")); // NOI18N
        exportBibMenuItem.setName("exportBibMenuItem"); // NOI18N
        exportBibMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportBibMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exportBibMenuItem);

        importBibMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_MASK));
        importBibMenuItem.setText(resourceMap.getString("importBibMenuItem.text")); // NOI18N
        importBibMenuItem.setName("importBibMenuItem"); // NOI18N
        importBibMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importBibMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(importBibMenuItem);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setText(resourceMap.getString("saveMenuItem.text")); // NOI18N
        saveMenuItem.setName("saveMenuItem"); // NOI18N
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveMenuItem);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(superreferencer.SuperReferencerApp.class).getContext().getActionMap(SuperReferencerView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        extraMenu.setText(resourceMap.getString("extraMenu.text")); // NOI18N
        extraMenu.setName("extraMenu"); // NOI18N

        settingsMenu.setText(resourceMap.getString("settingsMenu.text")); // NOI18N
        settingsMenu.setName("settingsMenu"); // NOI18N

        fontShapeMenuItem.setText(resourceMap.getString("fontShapeMenuItem.text")); // NOI18N
        fontShapeMenuItem.setName("fontShapeMenuItem"); // NOI18N
        fontShapeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fontShapeMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(fontShapeMenuItem);

        fontColorMenuItem.setText(resourceMap.getString("fontColorMenuItem.text")); // NOI18N
        fontColorMenuItem.setName("fontColorMenuItem"); // NOI18N
        fontColorMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fontColorMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(fontColorMenuItem);

        extraMenu.add(settingsMenu);

        menuBar.add(extraMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        fontDialog.setModal(true);
        fontDialog.setName("fontDialog"); // NOI18N
        fontDialog.setResizable(false);

        fontChooser.setName("fontChooser"); // NOI18N

        jToolBar3.setFloatable(false);
        jToolBar3.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jToolBar3.setName("jToolBar3"); // NOI18N

        buttonGroup1.add(keyToggleButton);
        keyToggleButton.setText(resourceMap.getString("keyToggleButton.text")); // NOI18N
        keyToggleButton.setFocusable(false);
        keyToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        keyToggleButton.setName("keyToggleButton"); // NOI18N
        keyToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        keyToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keyToggleButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(keyToggleButton);

        buttonGroup1.add(titleToggleButton);
        titleToggleButton.setText(resourceMap.getString("titleToggleButton.text")); // NOI18N
        titleToggleButton.setFocusable(false);
        titleToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        titleToggleButton.setName("titleToggleButton"); // NOI18N
        titleToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        titleToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                titleToggleButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(titleToggleButton);

        buttonGroup1.add(yearToggleButton);
        yearToggleButton.setText(resourceMap.getString("yearToggleButton.text")); // NOI18N
        yearToggleButton.setFocusable(false);
        yearToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        yearToggleButton.setName("yearToggleButton"); // NOI18N
        yearToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        yearToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yearToggleButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(yearToggleButton);

        buttonGroup1.add(authorToggleButton);
        authorToggleButton.setText(resourceMap.getString("authorToggleButton.text")); // NOI18N
        authorToggleButton.setFocusable(false);
        authorToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        authorToggleButton.setName("authorToggleButton"); // NOI18N
        authorToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        authorToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                authorToggleButtonActionPerformed(evt);
            }
        });
        jToolBar3.add(authorToggleButton);

        saveFontButton.setText(resourceMap.getString("saveFontButton.text")); // NOI18N
        saveFontButton.setName("saveFontButton"); // NOI18N
        saveFontButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveFontButtonActionPerformed(evt);
            }
        });

        cancelFontButton.setText(resourceMap.getString("cancelFontButton.text")); // NOI18N
        cancelFontButton.setName("cancelFontButton"); // NOI18N

        javax.swing.GroupLayout fontDialogLayout = new javax.swing.GroupLayout(fontDialog.getContentPane());
        fontDialog.getContentPane().setLayout(fontDialogLayout);
        fontDialogLayout.setHorizontalGroup(
            fontDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fontDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fontDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fontChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fontDialogLayout.createSequentialGroup()
                        .addComponent(cancelFontButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(saveFontButton))
                    .addComponent(jToolBar3, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE))
                .addContainerGap())
        );
        fontDialogLayout.setVerticalGroup(
            fontDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fontDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jToolBar3, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fontChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(fontDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(saveFontButton)
                    .addComponent(cancelFontButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        fontColorDialog.setModal(true);
        fontColorDialog.setName("fontColorDialog"); // NOI18N
        fontColorDialog.setResizable(false);

        colorChooser.setName("colorChooser"); // NOI18N

        jToolBar4.setFloatable(false);
        jToolBar4.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jToolBar4.setName("jToolBar4"); // NOI18N

        buttonGroup2.add(keyColorToggleButton);
        keyColorToggleButton.setSelected(true);
        keyColorToggleButton.setText(resourceMap.getString("keyColorToggleButton.text")); // NOI18N
        keyColorToggleButton.setFocusable(false);
        keyColorToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        keyColorToggleButton.setName("keyColorToggleButton"); // NOI18N
        keyColorToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        keyColorToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keyColorToggleButtonActionPerformed(evt);
            }
        });
        jToolBar4.add(keyColorToggleButton);

        buttonGroup2.add(titleColorToggleButton);
        titleColorToggleButton.setText(resourceMap.getString("titleColorToggleButton.text")); // NOI18N
        titleColorToggleButton.setFocusable(false);
        titleColorToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        titleColorToggleButton.setName("titleColorToggleButton"); // NOI18N
        titleColorToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        titleColorToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                titleColorToggleButtonActionPerformed(evt);
            }
        });
        jToolBar4.add(titleColorToggleButton);

        buttonGroup2.add(yearColorToggleButton);
        yearColorToggleButton.setText(resourceMap.getString("yearColorToggleButton.text")); // NOI18N
        yearColorToggleButton.setFocusable(false);
        yearColorToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        yearColorToggleButton.setName("yearColorToggleButton"); // NOI18N
        yearColorToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        yearColorToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yearColorToggleButtonActionPerformed(evt);
            }
        });
        jToolBar4.add(yearColorToggleButton);

        buttonGroup2.add(authorColorToggleButton);
        authorColorToggleButton.setText(resourceMap.getString("authorColorToggleButton.text")); // NOI18N
        authorColorToggleButton.setFocusable(false);
        authorColorToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        authorColorToggleButton.setName("authorColorToggleButton"); // NOI18N
        authorColorToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        authorColorToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                authorColorToggleButtonActionPerformed(evt);
            }
        });
        jToolBar4.add(authorColorToggleButton);

        saveColorFontButton.setText(resourceMap.getString("saveColorFontButton.text")); // NOI18N
        saveColorFontButton.setName("saveColorFontButton"); // NOI18N
        saveColorFontButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveColorFontButtonActionPerformed(evt);
            }
        });

        cancelColorFontButton.setText(resourceMap.getString("cancelColorFontButton.text")); // NOI18N
        cancelColorFontButton.setName("cancelColorFontButton"); // NOI18N

        javax.swing.GroupLayout fontColorDialogLayout = new javax.swing.GroupLayout(fontColorDialog.getContentPane());
        fontColorDialog.getContentPane().setLayout(fontColorDialogLayout);
        fontColorDialogLayout.setHorizontalGroup(
            fontColorDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fontColorDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fontColorDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fontColorDialogLayout.createSequentialGroup()
                        .addComponent(cancelColorFontButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(saveColorFontButton)
                        .addContainerGap())
                    .addGroup(fontColorDialogLayout.createSequentialGroup()
                        .addGroup(fontColorDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jToolBar4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(colorChooser, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap())))
        );
        fontColorDialogLayout.setVerticalGroup(
            fontColorDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fontColorDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jToolBar4, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(colorChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(fontColorDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(saveColorFontButton)
                    .addComponent(cancelColorFontButton))
                .addContainerGap())
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
    }// </editor-fold>//GEN-END:initComponents

    private void docListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_docListValueChanged
        System.out.println(evt);
        //if (evt.getValueIsAdjusting()) return;
        BibDocument[] docs = getDocSelectedValues();
        databaseLabel.setText(String.format(DOCUMENT_TEMPLATE, count,
                selected = docs.length));
        if (docs != null && docs.length == 0 && docTagListModel.isEmpty())
            return;
        fillDocTagList(docs);
}//GEN-LAST:event_docListValueChanged

    private void docListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_docListMouseClicked
        int index = docList.locationToIndex(evt.getPoint());
        if (index < 0) return;
        if (!docList.getCellBounds(index, index).contains(evt.getPoint())) {
            docList.clearSelection();
            return;
        }
        if(evt.getButton() == MouseEvent.BUTTON1) {
            if (evt.getClickCount() == 2) {
                showSelectedItem();
            }
        } else if (evt.getButton() == MouseEvent.BUTTON3) {
            //if (evt.isPopupTrigger()) {
            //int index = docList.locationToIndex(evt.getPoint());
            //if (index < 0) return;
            //if (docList.getCellBounds(index, index).contains(evt.getPoint())) {
                docList.setSelectedIndex(index);
                popupMenu.show(docList, evt.getX(), evt.getY());
            //}
        }
}//GEN-LAST:event_docListMouseClicked

    private void exportBibMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportBibMenuItemActionPerformed
        // TODO add your handling code here:
        //fileChooser.setFi
        if (getTheApplication().getDocuments().isEmpty()) {
            JOptionPane.showMessageDialog(getFrame(),
                    "Nothing to export", "Export", 
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int state = fileChooser.showSaveDialog(getFrame());
        if (state == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                PrintWriter writer = new PrintWriter(file);
                String tag = getTagSelectedValue();
                if (tag == ALL_TAG)
                    getTheApplication().printBibtexFile(writer);
                else if (tag == UNTAGGED_TAG)
                    getTheApplication().printUntaggedBibtexFile(writer);
                else if (tag == UNATTACHED_TAG)
                    getTheApplication().printUnattachedBibtexFile(writer);
                else if (tag != null)
                    getTheApplication().printBibtexFile(writer, tag);
                else {
                    BibDocument[] docs = getDocSelectedValues();
                    if (docs != null)
                        getTheApplication().printBibtexFile(writer, docs);
                    else getTheApplication().printBibtexFile(writer);
                }
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(SuperReferencerView.class.getName()).log(
                        Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_exportBibMenuItemActionPerformed

    private void tagListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_tagListValueChanged
        String tag = getTagSelectedValue();
        docList.clearSelection();
        if (tag == null) return;
        if (tag == ALL_TAG) fillList(getTheApplication().getDocuments());
        else if (tag == UNTAGGED_TAG)
            fillList(getTheApplication().getUntaggedDocuments());
        else if (tag == UNATTACHED_TAG)
            fillList(getTheApplication().getUnattachedDocuments());
        else fillList(getTheApplication().getTaggedDocuments(tag));
    }//GEN-LAST:event_tagListValueChanged

    private void docListKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_docListKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_DELETE
                && evt.getModifiers() == 0) {
            BibDocument[] docs = getDocSelectedValues();
            if (docs != null)
                /*for (BibDocument doc : docs) {
                    if (doc != null) removeDocument(doc);
                }*/
                removeDocuments(docs);
        }            
    }//GEN-LAST:event_docListKeyReleased

    private void importBibMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importBibMenuItemActionPerformed
        int state = fileChooser.showOpenDialog(getFrame());
        if (state == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                BibtexFile tmp = new BibtexFile();
                BibtexParser parser = new BibtexParser(true);
                InputStreamReader reader = new InputStreamReader(
                        new FileInputStream(file));
                parser.parse(tmp, reader);
                List<BibtexAbstractEntry> entries = tmp.getEntries();
                for (BibtexAbstractEntry entry : entries) {
                    if (entry instanceof BibtexEntry) {
                        BibtexEntry theEntry = (BibtexEntry)entry;
                        BibtexAbstractValue fieldValue = 
                                theEntry.getFieldValue("file");
                        File f = null;
                        if (fieldValue != null) {
                            String fileString = fieldValue.toString();
                            if (fieldValue instanceof BibtexString) {
                                BibtexString bibString =
                                        (BibtexString)fieldValue;
                                fileString = bibString.getContent();
                            }
                            f = new File(fileString);
                        }
                        theEntry = BibtexUtilities.clone(
                                    getTheApplication().getBibtexFile(),
                                    theEntry);
                        if (f != null && f.exists()) addFile(f, theEntry);
                        else addFile(null, theEntry);
                    }
                }
                reader.close();
            } catch (CloneNotSupportedException ex) {
                Logger.getLogger(SuperReferencerView.class.getName()).log(
                        Level.SEVERE, null, ex);
            } catch (ParseException ex) {
                Logger.getLogger(SuperReferencerView.class.getName()).log(
                        Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(SuperReferencerView.class.getName()).log(
                        Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_importBibMenuItemActionPerformed

    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        saveButtonActionPerformed(evt);
    }//GEN-LAST:event_saveMenuItemActionPerformed

    private void newEntryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newEntryButtonActionPerformed
        // TODO add your handling code here:
        showDocumentProperties(null);
    }//GEN-LAST:event_newEntryButtonActionPerformed

    private void importBibButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importBibButtonActionPerformed
        // TODO add your handling code here:
        importBibMenuItemActionPerformed(evt);
    }//GEN-LAST:event_importBibButtonActionPerformed

    private void exportBibButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportBibButtonActionPerformed
        // TODO add your handling code here:
        exportBibMenuItemActionPerformed(evt);
    }//GEN-LAST:event_exportBibButtonActionPerformed

    private void saveFontButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveFontButtonActionPerformed
        Font font = fontChooser.getSelectedFont();
        if (keyToggleButton.isSelected()) {
            docListRenderer.setKeyFont(font);
            getTheApplication().setDocumentsKeyFont(font);
        }  else if (titleToggleButton.isSelected()) {
            docListRenderer.setTitleFont(font);
            getTheApplication().setDocumentsTitleFont(font);
        } else if (yearToggleButton.isSelected()) {
            docListRenderer.setYearFont(font);
            getTheApplication().setDocumentsYearFont(font);
        } else if (authorToggleButton.isSelected()) {
            docListRenderer.setAuthorFont(font);
            getTheApplication().setDocumentsAuthorFont(font);
        }
        docList.invalidate();
        docList.repaint();
    }//GEN-LAST:event_saveFontButtonActionPerformed

    private void fontShapeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fontShapeMenuItemActionPerformed
        fontShapeButtonActionPerformed(evt);
    }//GEN-LAST:event_fontShapeMenuItemActionPerformed

    private void keyToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keyToggleButtonActionPerformed
        fontChooser.setSelectedFont(
                getTheApplication().getDocumentsKeyFont());
    }//GEN-LAST:event_keyToggleButtonActionPerformed

    private void titleToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_titleToggleButtonActionPerformed
        fontChooser.setSelectedFont(
                getTheApplication().getDocumentsTitleFont());
    }//GEN-LAST:event_titleToggleButtonActionPerformed

    private void yearToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yearToggleButtonActionPerformed
        fontChooser.setSelectedFont(
                getTheApplication().getDocumentsYearFont());
    }//GEN-LAST:event_yearToggleButtonActionPerformed

    private void authorToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_authorToggleButtonActionPerformed
        fontChooser.setSelectedFont(
                getTheApplication().getDocumentsAuthorFont());
    }//GEN-LAST:event_authorToggleButtonActionPerformed

    private void keyColorToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keyColorToggleButtonActionPerformed
        colorChooser.setColor(getTheApplication().getDocumentsKeyColor());
    }//GEN-LAST:event_keyColorToggleButtonActionPerformed

    private void titleColorToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_titleColorToggleButtonActionPerformed
        colorChooser.setColor(getTheApplication().getDocumentsTitleColor());
    }//GEN-LAST:event_titleColorToggleButtonActionPerformed

    private void yearColorToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yearColorToggleButtonActionPerformed
        colorChooser.setColor(getTheApplication().getDocumentsYearColor());
    }//GEN-LAST:event_yearColorToggleButtonActionPerformed

    private void authorColorToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_authorColorToggleButtonActionPerformed
        colorChooser.setColor(getTheApplication().getDocumentsAuthorColor());
    }//GEN-LAST:event_authorColorToggleButtonActionPerformed

    private void saveColorFontButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveColorFontButtonActionPerformed
        Color color = colorChooser.getColor();
        if (keyColorToggleButton.isSelected()) {
            docListRenderer.setKeyColor(color);
            getTheApplication().setDocumentsKeyColor(color);
        }  else if (titleColorToggleButton.isSelected()) {
            docListRenderer.setTitleColor(color);
            getTheApplication().setDocumentsTitleColor(color);
        } else if (yearColorToggleButton.isSelected()) {
            docListRenderer.setYearColor(color);
            getTheApplication().setDocumentsYearColor(color);
        } else if (authorColorToggleButton.isSelected()) {
            docListRenderer.setAuthorColor(color);
            getTheApplication().setDocumentsAuthorColor(color);
        }
        docList.invalidate();
        docList.repaint();
    }//GEN-LAST:event_saveColorFontButtonActionPerformed

    private void fontColorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fontColorButtonActionPerformed
        keyColorToggleButton.setSelected(true);
        colorChooser.setColor(getTheApplication().getDocumentsKeyColor());
        fontColorDialog.pack();
        fontColorDialog.setLocationRelativeTo(getFrame());
        fontColorDialog.setVisible(true);
    }//GEN-LAST:event_fontColorButtonActionPerformed

    private void fontShapeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fontShapeButtonActionPerformed
        keyToggleButton.setSelected(true);
        fontChooser.setSelectedFont(getTheApplication().getDocumentsKeyFont());
        fontDialog.pack();
        fontDialog.setLocationRelativeTo(getFrame());
        fontDialog.setVisible(true);
    }//GEN-LAST:event_fontShapeButtonActionPerformed

    private void sortButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sortButtonActionPerformed
        // TODO add your handling code here:
        getTheApplication().setComparator(comparatorIterator.next());
        SortedSet<BibDocument> docs = new TreeSet<BibDocument>(
                getTheApplication().getComparator());
        Enumeration<?> elements = docListModel.elements();
        while (elements.hasMoreElements()) {
            docs.add((BibDocument) elements.nextElement());
        }
        //Collections.sort(docs, getTheApplication().getComparator());
        fillList(docs);
    }//GEN-LAST:event_sortButtonActionPerformed

    private void tagListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tagListMouseClicked
        int index = tagList.locationToIndex(evt.getPoint());
        if (index < 0) return;
        if (!tagList.getCellBounds(index, index).contains(evt.getPoint())) {
            tagList.clearSelection();
            return;
        }
    }//GEN-LAST:event_tagListMouseClicked

    private void fontColorMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fontColorMenuItemActionPerformed
        fontColorButtonActionPerformed(evt);
    }//GEN-LAST:event_fontColorMenuItemActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        try {
            getTheApplication().saveData();
        } catch (Exception ex) {
            Logger.getLogger(SuperReferencerView.class.getName()).log(
                    Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }//GEN-LAST:event_saveButtonActionPerformed

    private void addTagButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addTagButtonActionPerformed
        BibDocument[] dummy = null;
        addTagDialog(dummy);
    }//GEN-LAST:event_addTagButtonActionPerformed

    private void deleteTagButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteTagButtonActionPerformed
        String tag = getTagSelectedValue();
        if (tag == ALL_TAG || tag == UNTAGGED_TAG || tag == UNATTACHED_TAG)
            return;
        if (tag != null) {
            removeTag(tag);
            tagList.setSelectedValue(ALL_TAG, true);
        }
    }//GEN-LAST:event_deleteTagButtonActionPerformed

    private void addDocTagButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addDocTagButtonActionPerformed
        BibDocument[] docs = getDocSelectedValues();
        if (docs != null && docs.length > 0) addTagDialog(docs);
    }//GEN-LAST:event_addDocTagButtonActionPerformed

    private void deleteDocTagButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteDocTagButtonActionPerformed
        BibDocument[] docs = getDocSelectedValues();
        String tag = getDocTagSelectedValue();
        if (docs != null && docs.length > 0 &&
                tag != null) removeTag(tag, docs);
    }//GEN-LAST:event_deleteDocTagButtonActionPerformed

    private void searchTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchTextFieldActionPerformed
        String text = searchTextField.getText();
        if (text == null || (text = text.trim()).isEmpty()) return ;
        try {
            fillList(getTheApplication().getDocuments(text));
        } catch (superreferencer.javacc.ParseException ex) {
            /*Logger.getLogger(SuperReferencerView.class.getName()).log(
                    Level.SEVERE, null, ex);*/
            JOptionPane.showMessageDialog(getFrame(), "Invalid Search", "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (superreferencer.javacc.TokenMgrError ex) {
            JOptionPane.showMessageDialog(getFrame(), "Invalid Search", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_searchTextFieldActionPerformed

    private void searchTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_searchTextFieldCaretUpdate
        String text = searchTextField.getText();
        if (text.isEmpty()) fillList(getTheApplication().getDocuments());
    }//GEN-LAST:event_searchTextFieldCaretUpdate

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addDocTagButton;
    private javax.swing.JButton addTagButton;
    private javax.swing.JToggleButton authorColorToggleButton;
    private javax.swing.JToggleButton authorToggleButton;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton cancelColorFontButton;
    private javax.swing.JButton cancelFontButton;
    private javax.swing.JColorChooser colorChooser;
    private javax.swing.JLabel databaseLabel;
    private javax.swing.JButton deleteDocTagButton;
    private javax.swing.JButton deleteTagButton;
    private javax.swing.JList docList;
    private javax.swing.JScrollPane docScrollPane;
    private javax.swing.JList docTagList;
    private javax.swing.JScrollPane docTagScrollPane;
    private javax.swing.JButton exportBibButton;
    private javax.swing.JMenuItem exportBibMenuItem;
    private javax.swing.JMenu extraMenu;
    private say.swing.JFontChooser fontChooser;
    private javax.swing.JButton fontColorButton;
    private javax.swing.JDialog fontColorDialog;
    private javax.swing.JMenuItem fontColorMenuItem;
    private javax.swing.JDialog fontDialog;
    private javax.swing.JButton fontShapeButton;
    private javax.swing.JMenuItem fontShapeMenuItem;
    private javax.swing.JButton importBibButton;
    private javax.swing.JMenuItem importBibMenuItem;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JToolBar jToolBar3;
    private javax.swing.JToolBar jToolBar4;
    private javax.swing.JToggleButton keyColorToggleButton;
    private javax.swing.JToggleButton keyToggleButton;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JButton newEntryButton;
    private javax.swing.JButton saveButton;
    private javax.swing.JButton saveColorFontButton;
    private javax.swing.JButton saveFontButton;
    private javax.swing.JMenuItem saveMenuItem;
    private pt.unl.fct.di.tsantos.util.swing.JSearchTextField searchTextField;
    private javax.swing.JMenu settingsMenu;
    private javax.swing.JButton sortButton;
    private javax.swing.JList tagList;
    private javax.swing.JScrollPane tagScrollPane;
    private javax.swing.JToggleButton titleColorToggleButton;
    private javax.swing.JToggleButton titleToggleButton;
    private javax.swing.JToggleButton yearColorToggleButton;
    private javax.swing.JToggleButton yearToggleButton;
    // End of variables declaration//GEN-END:variables
    private JDialog aboutBox;
    private DocumentPropertiesDialog docProperties;

    private void removeDocumentsWithName(String name) {
        List<BibDocument> result =
                getTheApplication().removeDocumentsWithName(name);
        for (BibDocument doc : result) {
            docListModel.removeElement(doc);
            count--;
        }
        docList.repaint();
    }

    private void removeDocuments(BibDocument ... docs) {
        for (BibDocument doc : docs)
            getTheApplication().removeDocument(doc);
        docListModel.clear();
        Collection<BibDocument> documents = getTheApplication().getDocuments();
        fillList(documents);
        count = documents.size();
        databaseLabel.setText(
                String.format(DOCUMENT_TEMPLATE, count, selected));
    }
    
    private void removeDocument(BibDocument doc) {
        getTheApplication().removeDocument(doc);
        docListModel.removeElement(doc);
        docList.repaint();
        count--;
        databaseLabel.setText(
                String.format(DOCUMENT_TEMPLATE, count, selected));
    }

    private void changedItemLayout(BibDocument doc) {
        synchronized (docList) {
            synchronized (docListModel) {
                int index = docListModel.indexOf(doc);
                if (index < 0) return;
                docListModel.remove(index);
                docList.revalidate();
                docList.repaint();
                docListModel.add(index, doc);
                docList.setSelectedIndex(index);
                docList.revalidate();
                docList.repaint();
            }
        }
    }

    protected void addDocument(String key, File file, BibtexEntry entry) {
        BibDocument doc = getTheApplication().addDocument(key, file, entry);
        addDocument(doc, key);
    }

    protected void updateDocument(BibDocument doc, File file,
            BibtexEntry entry) {
        getTheApplication().updateDocument(doc, file, entry);
        doc.grabText();
        doc.grabThumbnail();
        docList.repaint();
    }

    /*synchronized*/ private void addDocument(BibDocument doc, String key) {
        getTheApplication().addDocumentPropertyChangeListener(key,
                new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() instanceof BibDocument) {
                    final BibDocument source = (BibDocument)evt.getSource();
                    if (evt.getPropertyName().equals("thumbnail")) {
                        java.awt.EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                changedItemLayout(source);
                                SuperReferencerView.this.getFrame().repaint();
                            }
                        });
                    }
                }
            }
        });
        doc.grabText();
        doc.grabThumbnail();
        docListModel.addElement(doc);
        docList.repaint();
        count++;
        databaseLabel.setText(String.format(DOCUMENT_TEMPLATE, count, selected));
    }

    private boolean addFile(File file, BibtexEntry entry) {
        //try {
        if (file != null &&
                getTheApplication().hasDocumentsWithName(file.getName())) {
            int showConfirmDialog =
                    JOptionPane.showConfirmDialog(getFrame(),
                    "There is a file with the same name in the library.\n"
                    + "Do you wan't to replace it with this file?\n\n"
                    + "Note: Files with the same name are not allowed!");
            if (showConfirmDialog == JOptionPane.YES_OPTION) {
                removeDocumentsWithName(file.getName());
            } else {
                return false;
            }
        }
        String key = entry == null ? 
            FileUtilities.getNameWithoutExtension(file.getName()) :
            entry.getEntryKey();
        BibDocument doc = getTheApplication().addDocument(key, file, entry);
        addDocument(doc, key);
        return true;
    }

    private class FileTransferHandler extends TransferHandler {

        @Override
        protected Transferable createTransferable(JComponent c) {
            JList list = (JList) c;
            List<File> files = new ArrayList<File>();
            for (Object obj : list.getSelectedValues()) {
                files.add(((BibDocument) obj).getFile());
            }
            return new FileTransferable(files);
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }


    }

    private class FileTransferable implements Transferable {

        private List<File> files;

        public FileTransferable(List<File> files) {
            this.files = files;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.javaFileListFlavor};
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(DataFlavor.javaFileListFlavor);
        }

        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return files;
        }
    }

    private void docListDropTargetDrop(DropTargetDropEvent evt) {
        int action = evt.getDropAction();
        if (evt.isLocalTransfer()) {
            /*if (!evt.getDropTargetContext().getComponent().equals(docList)) {
                BibDocument d = (BibDocument) docList.getSelectedValue();
                if (d == null) return;
                docListModel.removeElement(d);
                SuperReferencerView.this.app.removeDocument(d);
            }*/
            return;
        }
        evt.acceptDrop(action);
        try {
            Transferable data = evt.getTransferable();
            DataFlavor flavor = DataFlavor.stringFlavor;
            if (AppUtils.osIsWindows()) {
                flavor = DataFlavor.javaFileListFlavor;
            }
            if (data.isDataFlavorSupported(flavor)) {
                Object obj = data.getTransferData(flavor);
                if (flavor.equals(DataFlavor.stringFlavor)) {
                    String uriList = (String) obj;
                    obj = StringUtils.textURIListToFileList(uriList);
                }
                final List<File> files = (List<File>) obj;
                executor.submit(new Thread() {
                    @Override
                    public void run() {                        
                        List<File> reaLFiles = FileUtilities.filter(files,
                                new FileUtilities.ExtensionFileFilter("pdf"));
                        for (File file : reaLFiles) {
                            System.out.println("Dropped " + file.getName());
                            /*boolean added =*/ addFile(file, null);
                            /*if (!added) continue;*/
                        }
                    }
                });
            }
        } catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            evt.dropComplete(true);
        }
    }
}
