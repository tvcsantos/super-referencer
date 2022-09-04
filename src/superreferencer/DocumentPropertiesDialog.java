/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * DocumentPropertiesDialog.java
 *
 * Created on 27/Out/2011, 12:28:52
 */

package superreferencer;

import java.util.List;
import pt.unl.fct.di.tsantos.util.swing.ProgressEvent;
import pt.unl.fct.di.tsantos.util.swing.ProgressListener;
import pt.unl.fct.di.tsantos.util.swing.ProgressBar;
import bibtex.dom.BibtexAbstractEntry;
import bibtex.dom.BibtexEntry;
import bibtex.dom.BibtexFile;
import bibtex.parser.BibtexParser;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.sourceforge.tuned.FileUtilities;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.Task;
import org.jdesktop.application.TaskMonitor;
import org.jdesktop.application.TaskService;
import pt.unl.fct.di.tsantos.util.google.GoogleScholar;
import superreferencer.EntryTypeDataPanel.BibtexEntryType;
import superreferencer.EntryTypeDataPanel.BibtexField;
import pt.unl.fct.di.tsantos.util.google.GoogleScholar.GoogleScholarResult;
import superreferencer.SuperReferencerApp.BibDocuments.BibDocument;

/**
 *
 * @author tvcsantos
 */
public class DocumentPropertiesDialog extends javax.swing.JDialog {
    protected BibDocument doc;
    protected File file;
    protected SuperReferencerView view;

    /** Creates new form DocumentPropertiesDialog */
    public DocumentPropertiesDialog(SuperReferencerView view, boolean modal) {
        this(view.getFrame(), modal);
        this.view = view;
        new ProgressBar(view, progressBar,
                statusAnimationLabel, 4) {
            @Override
            protected String initMessageTimout() {
                return "StatusBar.messageTimeout";
            }

            @Override
            protected String initBusyAnimationRate() {
                return "StatusBar.busyAnimationRate";
            }

            @Override
            protected String initBusyIcons(int i) {
                return "StatusBar.busyIcons[" + i + "]";
            }

            @Override
            protected String initIdleIcon() {
                return "StatusBar.idleIcon";
            }
        };
    }

    private DocumentPropertiesDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setTitle("Document Properties");
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        typeComboBox.setModel(model);
        for (EntryTypeDataPanel.BibtexEntryType type : 
            EntryTypeDataPanel.BibtexEntryType.values()) {
            model.addElement(type);
        }
        setEntryType(BibtexEntryType.ARTICLE);
        pack();
    }

    private void setEntryType(BibtexEntryType type) {
        int count = typeComboBox.getItemCount();
        for (int i = 0; i < count; i++) {
            if (((BibtexEntryType)
                    typeComboBox.getItemAt(i)).equals(type)) {
                typeComboBox.setSelectedIndex(i);
                break;
            }
        }
        entryTypeDataPanel.setEntryType(type);
        repaint();
        pack();
    }

    public void setDocument(BibDocument doc) {
        this.doc = doc;
        loadDocument(this.doc);
    }

    private void setThumbnailIcon(BibDocument doc) {
        thumbnailLabel.setIcon(doc.getThumbnail());
        if (!doc.hasNonDefaultThumbnail())
            thumbnailLabel.setBorder(null);
        else thumbnailLabel.setBorder(javax.swing.BorderFactory.
            createLineBorder(Color.BLACK));
    }

    private void setThumbnailIcon(ImageIcon icon) {
        thumbnailLabel.setIcon(icon);
        if (icon.equals(SuperReferencerApp.getDefaultDocumentThumbnail()))
            thumbnailLabel.setBorder(null);
        else thumbnailLabel.setBorder(javax.swing.BorderFactory.
            createLineBorder(Color.BLACK));
    }

    private void loadDocument(BibDocument doc) {
        if (doc != null) {
            if (!doc.hasNonDefaultThumbnail())
            setThumbnailIcon(doc);
            BibtexEntry entry = doc.getEntry();
            loadEntry(entry);
            file = doc.getFile();
            if (file != null) fileTextField.setText(file.getName());
        } else {
            file = null;
            setThumbnailIcon(SuperReferencerApp.
                    getDefaultDocumentThumbnail());
            loadEntry(null);
        }
        repaint();
        pack();
    }
    
    private void loadEntry(BibtexEntry entry) {
        if (entry != null) {
            String strType = entry.getEntryType();
            BibtexEntryType entryType =
                    BibtexEntryType.valueOf(strType.toUpperCase());
            setEntryType(entryType);
            keyTextField.setText(entry.getEntryKey());
        } else {
            setEntryType(BibtexEntryType.ARTICLE);
            if (doc != null && doc.getFile() != null)
                keyTextField.setText(FileUtilities.getNameWithoutExtension(
                    doc.getFile().getName()));
            else keyTextField.setText("");
        }
        entryTypeDataPanel.setEntry(entry);
    }

    @Action
    public Task myTask() {
        return new ThisIsMyTask(view.getApplication(), this,
                getCurrentTitle(), file);
    }

    protected String getCurrentTitle() {
        return entryTypeDataPanel.getCurrentTitle();
    }

    private class ThisIsMyTask extends Task<Void, Void> {
        protected DocumentPropertiesDialog dd;
        protected String title;
        protected File file;

        ThisIsMyTask(Application app, DocumentPropertiesDialog dd,
                String title, File file) {
            super(app);
            this.dd = dd;
            this.title = title;
            this.file = file;
        }

        @Override
        protected Void doInBackground() throws Exception {
            if (file == null && (title == null || title.isEmpty()))
                return null;
            ProgressListener listener = new ProgressListener() {

                    public void progressStart(ProgressEvent pe) {
                        setMessage("Searching...");
                    }

                    public void progressUpdate(ProgressEvent pe) {

                    }

                    public void progressFinish(ProgressEvent pe) {
                        setMessage("Finished Search");
                    }

                    public void progressInterrupt(ProgressEvent pe) {
                        setMessage("Search Interrupted");
                    }
                };
            GoogleScholar finder = GoogleScholar.getInstance();
            try {
                finder.addProgressListener(listener);
                GoogleScholarResult result = null;
                if (title != null && !title.isEmpty()) {
                    List<GoogleScholarResult> search = 
                            finder.search(title, true);
                    if (!search.isEmpty()) result = search.get(0);
                }
                if (result == null && file != null) {
                    Map<File, GoogleScholarResult> search =
                        finder.search(file);
                    result = search.get(file);
                }
                if (result == null) return null;
                BibtexEntry entry = result.getBibtex();
                dd.loadEntry(entry);
                dd.repaint();
                dd.pack();
            } catch (IOException ex) {
                Logger.getLogger(DocumentPropertiesDialog.class.getName()).log(
                        Level.SEVERE, null, ex);
                ex.printStackTrace();
            } finally {
                finder.removeProgressListener(listener);
            }
            return null;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooser = new javax.swing.JFileChooser();
        documentLabel = new javax.swing.JLabel();
        thumbnailLabel = new javax.swing.JLabel();
        keyLabel = new javax.swing.JLabel();
        fileLabel = new javax.swing.JLabel();
        metadataLabel = new javax.swing.JLabel();
        typeLabel = new javax.swing.JLabel();
        typeComboBox = new javax.swing.JComboBox();
        cancelButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        keyTextField = new javax.swing.JTextField();
        fileTextField = new javax.swing.JTextField();
        entryTypeDataScrollPane = new javax.swing.JScrollPane();
        entryTypeDataPanel = new superreferencer.EntryTypeDataPanel();
        separator1 = new javax.swing.JSeparator();
        retrieveButton = new javax.swing.JButton();
        pasteButton = new javax.swing.JButton();
        clearButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        progressBar = new javax.swing.JProgressBar();
        statusAnimationLabel = new javax.swing.JLabel();
        browseButton = new javax.swing.JButton();

        fileChooser.setName("fileChooser"); // NOI18N

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setResizable(false);

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(superreferencer.SuperReferencerApp.class).getContext().getResourceMap(DocumentPropertiesDialog.class);
        documentLabel.setFont(resourceMap.getFont("documentLabel.font")); // NOI18N
        documentLabel.setText(resourceMap.getString("documentLabel.text")); // NOI18N
        documentLabel.setName("documentLabel"); // NOI18N

        thumbnailLabel.setIcon(resourceMap.getIcon("thumbnailLabel.icon")); // NOI18N
        thumbnailLabel.setText(resourceMap.getString("thumbnailLabel.text")); // NOI18N
        thumbnailLabel.setName("thumbnailLabel"); // NOI18N
        thumbnailLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                thumbnailLabelMouseClicked(evt);
            }
        });

        keyLabel.setText(resourceMap.getString("keyLabel.text")); // NOI18N
        keyLabel.setName("keyLabel"); // NOI18N

        fileLabel.setText(resourceMap.getString("fileLabel.text")); // NOI18N
        fileLabel.setName("fileLabel"); // NOI18N

        metadataLabel.setFont(resourceMap.getFont("metadataLabel.font")); // NOI18N
        metadataLabel.setText(resourceMap.getString("metadataLabel.text")); // NOI18N
        metadataLabel.setName("metadataLabel"); // NOI18N

        typeLabel.setText(resourceMap.getString("typeLabel.text")); // NOI18N
        typeLabel.setName("typeLabel"); // NOI18N

        typeComboBox.setName("typeComboBox"); // NOI18N
        typeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeComboBoxActionPerformed(evt);
            }
        });

        cancelButton.setIcon(resourceMap.getIcon("cancelButton.icon")); // NOI18N
        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        saveButton.setIcon(resourceMap.getIcon("saveButton.icon")); // NOI18N
        saveButton.setText(resourceMap.getString("saveButton.text")); // NOI18N
        saveButton.setName("saveButton"); // NOI18N
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        keyTextField.setText(resourceMap.getString("keyTextField.text")); // NOI18N
        keyTextField.setName("keyTextField"); // NOI18N

        fileTextField.setEditable(false);
        fileTextField.setText(resourceMap.getString("fileTextField.text")); // NOI18N
        fileTextField.setName("fileTextField"); // NOI18N

        entryTypeDataScrollPane.setBorder(null);
        entryTypeDataScrollPane.setName("entryTypeDataScrollPane"); // NOI18N

        entryTypeDataPanel.setEntryType(superreferencer.EntryTypeDataPanel.BibtexEntryType.ARTICLE);
        entryTypeDataPanel.setName("entryTypeDataPanel"); // NOI18N
        entryTypeDataScrollPane.setViewportView(entryTypeDataPanel);

        separator1.setName("separator1"); // NOI18N

        retrieveButton.setIcon(resourceMap.getIcon("retrieveButton.icon")); // NOI18N
        retrieveButton.setText(resourceMap.getString("retrieveButton.text")); // NOI18N
        retrieveButton.setName("retrieveButton"); // NOI18N
        retrieveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                retrieveButtonActionPerformed(evt);
            }
        });

        pasteButton.setIcon(resourceMap.getIcon("pasteButton.icon")); // NOI18N
        pasteButton.setText(resourceMap.getString("pasteButton.text")); // NOI18N
        pasteButton.setName("pasteButton"); // NOI18N
        pasteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pasteButtonActionPerformed(evt);
            }
        });

        clearButton.setIcon(resourceMap.getIcon("clearButton.icon")); // NOI18N
        clearButton.setText(resourceMap.getString("clearButton.text")); // NOI18N
        clearButton.setName("clearButton"); // NOI18N
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });

        jSeparator1.setName("jSeparator1"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        statusAnimationLabel.setIcon(resourceMap.getIcon("statusAnimationLabel.icon")); // NOI18N
        statusAnimationLabel.setText(resourceMap.getString("statusAnimationLabel.text")); // NOI18N
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        browseButton.setIcon(resourceMap.getIcon("browseButton.icon")); // NOI18N
        browseButton.setText(resourceMap.getString("browseButton.text")); // NOI18N
        browseButton.setName("browseButton"); // NOI18N
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(documentLabel)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(thumbnailLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(fileLabel)
                            .addComponent(keyLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(fileTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 291, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(browseButton))
                            .addComponent(keyTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE)))
                    .addComponent(entryTypeDataScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 486, Short.MAX_VALUE)
                    .addComponent(metadataLabel)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(typeLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(typeComboBox, 0, 454, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(retrieveButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pasteButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                        .addComponent(cancelButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(saveButton)))
                .addContainerGap())
            .addComponent(separator1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 506, Short.MAX_VALUE)
            .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 506, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(330, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(documentLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(thumbnailLabel)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(keyLabel)
                            .addComponent(keyTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(fileLabel)
                            .addComponent(fileTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(browseButton))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(separator1, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(metadataLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(typeLabel)
                    .addComponent(typeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(entryTypeDataScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(saveButton)
                    .addComponent(cancelButton)
                    .addComponent(retrieveButton)
                    .addComponent(pasteButton)
                    .addComponent(clearButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(statusAnimationLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void typeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeComboBoxActionPerformed
        EntryTypeDataPanel.BibtexEntryType type = 
                (BibtexEntryType) typeComboBox.getSelectedItem();
        setEntryType(type);
    }//GEN-LAST:event_typeComboBoxActionPerformed

private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
    String key = keyTextField.getText();
    if (key != null) key = key.trim();
    if (key == null || key.isEmpty()) {
        JOptionPane.showMessageDialog(this, 
                "Entry key can't be empty", "Save Entry",
                JOptionPane.ERROR_MESSAGE);
        return;
    }
    /*if (view.getTheApplication().hasDocumentWithKey(key)) {
        JOptionPane.showMessageDialog(this,
                "There is an entry with the same key", "Save Entry",
                JOptionPane.ERROR_MESSAGE);
        return;
    }*/
    BibtexFile bibtexFile = view.getTheApplication().getBibtexFile();
    BibtexEntry entry = entryTypeDataPanel.getEntry(bibtexFile, key);
    /*if (file != null)
        entry.addFieldValue("file",
            entry.getOwnerFile().makeString(file.toURI().toString()));*/
    BibtexField[] validate = BibtexEntryType.validate(entry);
    if (validate.length > 0) {
        JOptionPane.showMessageDialog(this,
                "The following fields are missing:\n" +
            Arrays.toString(validate),
            "Missing Mandatory Fields", JOptionPane.WARNING_MESSAGE);
        return;
    }
    if (doc == null) {
        // new document
        view.addDocument(key, file, entry);        
    } else {
        /*try {
            String dec = URLDecoder.decode(file.toURI().toString(), "UTF-8");
            System.out.println(dec);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(DocumentPropertiesDialog.class.getName()).log(
                    Level.SEVERE, null, ex);
        }*/
        view.updateDocument(doc, file, entry);
    }
    firePropertyChange("documentChanged", null, doc);
    setVisible(false);
}//GEN-LAST:event_saveButtonActionPerformed

private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
    setVisible(false);
}//GEN-LAST:event_cancelButtonActionPerformed

private void retrieveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_retrieveButtonActionPerformed
    Task mT = myTask();
    ApplicationContext appC = view.getApplication().getContext();
    TaskMonitor tM = appC.getTaskMonitor();
    TaskService tS = appC.getTaskService();
    tS.execute(mT);
    tM.setForegroundTask(mT);
}//GEN-LAST:event_retrieveButtonActionPerformed

private void thumbnailLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_thumbnailLabelMouseClicked
    if (evt.getClickCount() == 2) {
        try {
            if (file != null)// open document
                java.awt.Desktop.getDesktop().open(file);
        } catch (IOException ex) {
            Logger.getLogger(DocumentPropertiesDialog.class.getName()).log(
                    Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }
}//GEN-LAST:event_thumbnailLabelMouseClicked

private void pasteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteButtonActionPerformed
    try {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clip.getContents(this);
        if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            String transferData =
                    (String) contents.getTransferData(DataFlavor.stringFlavor);
            if (transferData != null) {
                transferData = transferData.trim();
                if (!transferData.isEmpty()) {
                    System.out.println(transferData);
                    try {
                        BibtexFile tmp = new BibtexFile();
                        new BibtexParser(true).parse(tmp,
                                new StringReader(transferData));
                        Iterator<BibtexAbstractEntry> it =
                                tmp.getEntries().iterator();
                        BibtexEntry entry = null;
                        while (it.hasNext()) {
                            BibtexAbstractEntry ae = it.next();
                            if (ae instanceof BibtexEntry) {
                                entry = (BibtexEntry) ae;
                                break;
                            }
                        }
                        if (entry != null) loadEntry(entry);
                        else {
                            JOptionPane.showMessageDialog(this,
                                "No valid BibTeX entry on the Clipboard",
                                "Import BibTeX entry",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this,
                                "No valid BibTeX entry on the Clipboard",
                                "Import BibTeX entry",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }                    
            }
        }
    } catch (UnsupportedFlavorException ex) {
        Logger.getLogger(DocumentPropertiesDialog.class.getName()).log(
                Level.SEVERE, null, ex);
        ex.printStackTrace();
    } catch (IOException ex) {
        Logger.getLogger(DocumentPropertiesDialog.class.getName()).log(
                Level.SEVERE, null, ex);
        ex.printStackTrace();
    }
}//GEN-LAST:event_pasteButtonActionPerformed

private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
    entryTypeDataPanel.setEntry(null);
}//GEN-LAST:event_clearButtonActionPerformed

private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
    // TODO add your handling code here:
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false);
    fileChooser.setFileFilter(
            new FileNameExtensionFilter("PDF Document", "pdf"));
    int state = fileChooser.showOpenDialog(this);
    if (state == JFileChooser.APPROVE_OPTION) {
        file = fileChooser.getSelectedFile();
        setThumbnailIcon(
                SuperReferencerApp.getDefaultDocumentThumbnail());
        fileTextField.setText(file.getName());
        repaint();
        pack();
    }
}//GEN-LAST:event_browseButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseButton;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton clearButton;
    private javax.swing.JLabel documentLabel;
    private superreferencer.EntryTypeDataPanel entryTypeDataPanel;
    private javax.swing.JScrollPane entryTypeDataScrollPane;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JLabel fileLabel;
    private javax.swing.JTextField fileTextField;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel keyLabel;
    private javax.swing.JTextField keyTextField;
    private javax.swing.JLabel metadataLabel;
    private javax.swing.JButton pasteButton;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton retrieveButton;
    private javax.swing.JButton saveButton;
    private javax.swing.JSeparator separator1;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel thumbnailLabel;
    private javax.swing.JComboBox typeComboBox;
    private javax.swing.JLabel typeLabel;
    // End of variables declaration//GEN-END:variables

}
