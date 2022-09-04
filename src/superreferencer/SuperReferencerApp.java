/*
 * SuperReferencerApp.java
 */

package superreferencer;

import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexEntry;
import bibtex.dom.BibtexFile;
import bibtex.dom.BibtexString;
import com.mortennobel.imagescaling.AdvancedResizeOp;
import com.mortennobel.imagescaling.DimensionConstrain;
import com.mortennobel.imagescaling.ResampleOp;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.ImageIcon;
import net.sf.ghost4j.document.PDFDocument;
import net.sf.ghost4j.renderer.SimpleRenderer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.PDFTextStripper;
import org.jdesktop.application.Application;
import pt.unl.fct.di.tsantos.util.ImageUtilities;
import pt.unl.fct.di.tsantos.util.app.Data;
import pt.unl.fct.di.tsantos.util.app.DefaultSingleFrameApplication;
import superreferencer.SuperReferencerApp.BibDocuments.BibDocument;
import superreferencer.javacc.IASTExpression;
import superreferencer.javacc.ParseException;
import superreferencer.javacc.Parser;
import superreferencer.javacc.TokenMgrError;

/**
 * The main class of the application.
 */
public class SuperReferencerApp extends DefaultSingleFrameApplication {

    protected File imagesDir;

    @Data protected BibDocuments documents;

    public static interface BibDocumentComparator
            extends Comparator<BibDocument>, Serializable { }

    public static class KeyComparator
            extends AbstractBibDocumentComparator {
        
        protected int innerCompare(BibDocument o1, BibDocument o2) { return 0; }

        /*public int compare(BibDocument o1, BibDocument o2) {
            String k1 = o1.getKey();
            String k2 = o2.getKey();
            if (k1 == null && k2 == null) return 0;
            if (k1 == null) return -1;
            if (k2 == null) return 1;
            return k1.compareTo(k2);
        }*/
    }

    public static abstract class AbstractBibDocumentComparator
            implements BibDocumentComparator {
        public final int compare(BibDocument o1, BibDocument o2) {
            int cmp = innerCompare(o1, o2);
            if (cmp != 0) return cmp;
            String k1 = o1.getKey();
            String k2 = o2.getKey();
            if (k1 == null && k2 == null) return 0;
            if (k1 == null) return -1;
            if (k2 == null) return 1;
            return k1.compareTo(k2);
        }

        protected abstract int innerCompare(BibDocument o1, BibDocument o2);
    }

    public static class TypeComparator 
            extends AbstractBibDocumentComparator {

        protected int innerCompare(BibDocument o1, BibDocument o2) {
            BibtexEntry e1 = o1.getEntry();
            BibtexEntry e2 = o2.getEntry();
            if (e1 == null && e2 == null) return 0;
            if (e1 == null) return -1;
            if (e2 == null) return 1;
            String t1 = e1.getEntryType();
            String t2 = e2.getEntryType();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return -1;
            if (t2 == null) return 1;
            return t1.compareTo(t2);
        }

    }

    public static class AlphabeticFieldComparator
            extends AbstractBibDocumentComparator {
        protected String field;
        
        public AlphabeticFieldComparator(String field) {
            this.field = field;
        }
        
        protected int innerCompare(BibDocument o1, BibDocument o2) {
            BibtexEntry e1 = o1.getEntry();
            BibtexEntry e2 = o2.getEntry();
            if (e1 == null && e2 == null) return 0;
            if (e1 == null) return -1;
            if (e2 == null) return 1;
            BibtexAbstractValue f1 = e1.getFieldValue(field);
            BibtexAbstractValue f2 = e2.getFieldValue(field);
            if (f1 == null && f2 == null) return 0;
            if (f1 == null) return -1;
            if (f2 == null) return 1;
            String fs1 = f1.toString();
            String fs2 = f2.toString();
            if (f1 instanceof BibtexString) {
                BibtexString bs = (BibtexString) f1;
                fs1 = bs.getContent();
            }
            if (f2 instanceof BibtexString) {
                BibtexString bs = (BibtexString) f2;
                fs2 = bs.getContent();
            }
            return fs1.compareTo(fs2);
        }
    }

    public static class BibDocuments implements Serializable {

        public static class BibDocument implements Serializable {
            static Map<File, ImageIcon> IMG_CACHE =
                    new HashMap<File, ImageIcon>();
            static Map<File, String> TEXT_CACHE =
                    new HashMap<File, String>();
            static ExecutorService TEXT_EXECUTOR =
                    Executors.newSingleThreadExecutor();
            static ExecutorService IMG_EXECUTOR =
                    Executors.newSingleThreadExecutor();

            protected String key;
            protected File file;
            protected ImageIcon thumbnail;
            protected String text;
            protected BibtexEntry entry;
            protected Set<String> tags;
            protected BibDocuments owner;
            protected ImageIcon defaultThumbnail;

            private boolean grabbedText = false;
            private boolean grabbedThumbnail = false;

            protected transient List<PropertyChangeListener> listeners;

            @Override
            public int hashCode() {
                int hash = 7;
                hash = 59 * hash + (this.key != null ? this.key.hashCode() : 0);
                return hash;
            }

            private BibDocument(BibDocuments owner, String key) {
                this(owner, key, null, null);
            }

            private BibDocument(BibDocuments owner, String key,
                    BibtexEntry entry) {
                this(owner, key, null, entry);
            }

            private BibDocument(BibDocuments owner, String key, File file) {
                this(owner, key, file, null);
            }

            private BibDocument(BibDocuments owner, String key, File file,
                    BibtexEntry entry) {
                if (key == null) throw new NullPointerException();
                this.owner = owner;
                this.key = key;
                this.file = file;
                this.entry = entry;
                this.tags = new HashSet<String>();
                this.listeners = new LinkedList<PropertyChangeListener>();
            }

            private void addPropertyChangeListener(
                    PropertyChangeListener listener) {
                if (listeners == null)
                    listeners = new LinkedList<PropertyChangeListener>();
                listeners.add(listener);
            }

            private void removePropertyChangeListener(
                    PropertyChangeListener listener) {
                if (listeners == null)
                    listeners = new LinkedList<PropertyChangeListener>();
                listeners.remove(listener);
            }

            private void notifyListeners(PropertyChangeEvent evt) {
                if (listeners == null)
                    listeners = new LinkedList<PropertyChangeListener>();
                for (PropertyChangeListener listener : listeners)
                    listener.propertyChange(evt);
            }

            public BibtexEntry getEntry() {
                return entry;
            }

            public void grabText() {
                if (file == null) return;
                if (grabbedText) return;
                grabbedText = true;
                if (TEXT_CACHE.get(file) == null) {
                    TEXT_EXECUTOR.submit(new Thread() {
                        @Override
                        public void run() {
                            try {
                                System.out.println("Grabbing Text");
                                PDDocument doc = PDDocument.load(
                                        BibDocument.this.file);
                                PDFTextStripper stripper = new PDFTextStripper();
                                String text = stripper.getText(doc);
                                doc.close();
                                String old = BibDocument.this.text;
                                BibDocument.this.text = text;
                                notifyListeners(new PropertyChangeEvent(
                                        BibDocument.this, "text", old,
                                        BibDocument.this.text));
                                TEXT_CACHE.put(file, text);
                            } catch (IOException ex) {
                                grabbedText = false;
                            }
                        }
                    });
                } else {
                    String old = BibDocument.this.text;
                    BibDocument.this.text = TEXT_CACHE.get(file);
                    notifyListeners(new PropertyChangeEvent(
                            BibDocument.this, "text", old,
                            BibDocument.this.text));
                }
            }

            public void grabThumbnail() {
                if (file == null) return;
                if (grabbedThumbnail) return;
                grabbedThumbnail = true;
                if (IMG_CACHE.get(file) == null) {
                    IMG_EXECUTOR.submit(new Thread() {

                        @Override
                        public void run() {
                            System.out.println("Grabbing Thumbnail");
                            BufferedImage image = null;
                            String imageFormat = "png";
                            try {                                
                                PDFDocument document = new PDFDocument();
                                document.load(file);
                                SimpleRenderer renderer = new SimpleRenderer();
                                renderer.setResolution(200);
                                List<Image> images =
                                        renderer.render(document, 0, 0);
                                image = ImageUtilities.convert(images.get(0));
                                image = ImageUtilities.renew(image,
                                                            imageFormat);
                            } catch (Exception ex) {
                                try {
                                    PDDocument document =
                                            PDDocument.load(file);
                                    List pages = document.getDocumentCatalog().
                                            getAllPages();
                                    PDPage page = (PDPage)pages.get(0);
                                    image = page.convertToImage(
                                            BufferedImage.TYPE_INT_BGR, 200);
                                } catch (Exception ex2) {
                                    grabbedThumbnail = false;
                                }
                            }
                            if (grabbedThumbnail) {                            
                                int width = image.getWidth();
                                //System.out.println(width);
                                float factor = 64.0f / width;
                                ResampleOp resampleOp = new ResampleOp(
                                        DimensionConstrain.createRelativeDimension(
                                        factor));
                                resampleOp.setUnsharpenMask(
                                        AdvancedResizeOp.UnsharpenMask.Normal);
                                BufferedImage rescaled =
                                        resampleOp.filter(image, null);
                                ImageIcon old = BibDocument.this.thumbnail;
                                BibDocument.this.thumbnail =
                                        new ImageIcon(rescaled);
                                notifyListeners(new PropertyChangeEvent(
                                        BibDocument.this, "thumbnail", old,
                                        BibDocument.this.thumbnail));
                                IMG_CACHE.put(file, BibDocument.this.thumbnail);
                            }
                        }
                    });
                } else {
                    ImageIcon old = BibDocument.this.thumbnail;
                    BibDocument.this.thumbnail = IMG_CACHE.get(file);
                    notifyListeners(new PropertyChangeEvent(
                            BibDocument.this, "thumbnail", old,
                            BibDocument.this.thumbnail));
                }
            }

            public File getFile() {
                return file;
            }

            public String getKey() {
                return key;
            }
            
            public ImageIcon getThumbnail() {
                if (!grabbedThumbnail) grabThumbnail();
                if (thumbnail == null) return defaultThumbnail;
                return thumbnail;
            }

            private void setEntry(BibtexEntry entry) {
                this.entry = entry;
                if (this.entry != null)
                    this.entry.setEntryKey(key);
            }

            private boolean addTag(String tag) {
                return tags.add(tag);
            }

            private boolean removeTag(String tag) {
                return tags.remove(tag);
            }

            public boolean hasTag(String tag) {
                return tags.contains(tag);
            }

            public String getText() {
                if (!grabbedText) grabText();
                return text;
            }

            public BibDocuments getOwner() {
                return owner;
            }

            public Collection<String> getTags() {
                return tags;
            }

            public ImageIcon getDefaultThumbnail() {
                return defaultThumbnail;
            }

            public void setDefaultThumbnail(ImageIcon defaultThumbnail) {
                this.defaultThumbnail = defaultThumbnail;
            }

            public boolean hasDefaultThumbnail() {
                return defaultThumbnail != null;
            }

            public boolean hasNonDefaultThumbnail() {
                return thumbnail != null;
            }

            public boolean hasThumbnail() {
                return defaultThumbnail != null || thumbnail != null;
            }
            
            private void setFile(File file) {
                if (this.file != file) {
                    grabbedText = false;
                    grabbedThumbnail = false;
                    text = null;
                    thumbnail = null;
                }              
                this.file = file;
            }

            private void writeObject(java.io.ObjectOutputStream out)
                    throws IOException {
                out.defaultWriteObject();
            }

            private void readObject(java.io.ObjectInputStream in)
                    throws IOException,
                    ClassNotFoundException {
                in.defaultReadObject();
                if (thumbnail != null && grabbedThumbnail)
                    IMG_CACHE.put(file, thumbnail);
                if (text != null && grabbedText)
                    TEXT_CACHE.put(file, text);
                grabText();
                grabThumbnail();
            }
        }

        public static final Font DEFAULT_FONT = 
                new Font(Font.DIALOG, Font.PLAIN, 10);
        public static final Color DEFAULT_COLOR = Color.BLACK;
        
        protected Map<String, BibDocument> documents; // key, bibdocument
        protected BibtexFile bibtexFile;
        protected Set<String> tags;
        protected Font keyFont;
        protected Color keyColor;
        protected Font titleFont;
        protected Color titleColor;
        protected Font authorFont;
        protected Color authorColor;
        protected Font yearFont;
        protected Color yearColor;

        //////////////////// VIEWS OF DOCUMENTS ////////////////////
        protected BibDocumentComparator currentOrder =
                new AlphabeticFieldComparator("title");
        protected SortedSet<BibDocument> unattachedDocuments;
        protected SortedSet<BibDocument> untaggedDocuments;
        protected Map<BibDocumentComparator, SortedSet<BibDocument>>
                orderedDocuments = new HashMap<BibDocumentComparator,
                                            SortedSet<BibDocument>>();

        public BibDocuments() {
            documents = new HashMap<String, BibDocument>();
            bibtexFile = new BibtexFile();
            tags = new HashSet<String>();
            keyFont = DEFAULT_FONT;
            keyColor = DEFAULT_COLOR;
            titleFont = DEFAULT_FONT.deriveFont(Font.BOLD);
            titleColor = DEFAULT_COLOR;
            yearFont = DEFAULT_FONT;
            yearColor = DEFAULT_COLOR;
            authorFont = DEFAULT_FONT.deriveFont(Font.ITALIC);
            authorColor = DEFAULT_COLOR;
        }

        public BibDocument addDocument(String key, File file,
                BibtexEntry entry) {
            BibDocument doc = new BibDocument(this, key, file);
            setBibtexEntry(doc, entry);
            //// INVALIDATE VIEWS
            if (doc.getFile() == null) unattachedDocuments = null;
            untaggedDocuments = null;
            orderedDocuments.clear();
            return documents.put(key, doc);
        }

        public BibDocument removeDocument(String key) {
            if (key == null) throw new NullPointerException();
            return innerRemoveDocument(key);
        }

        // should not allow documents with the same name?
        public List<BibDocument> removeDocumentsWithName(String name) {
            List<BibDocument> result = new LinkedList<BibDocument>();
            List<String> keys = new LinkedList<String>();
            for (BibDocument doc : documents.values()) {
                File f = doc.getFile();
                if (f != null && f.getName().compareTo(name) == 0)
                    keys.add(doc.getKey());
            }
            for (String key : keys) result.add(innerRemoveDocument(key));
            return result;
        }

        public boolean removeDocument(BibDocument doc) {
            String key = doc.getKey();
            return innerRemoveDocument(key) != null;
        }

        protected BibDocument innerRemoveDocument(String key) {
            BibDocument doc = documents.remove(key);
            if (doc != null && doc.getEntry() != null)
                bibtexFile.removeEntry(doc.getEntry());
            if (doc != null && doc.getFile() == null)
                unattachedDocuments = null;
            untaggedDocuments = null;
            orderedDocuments.clear();
            return doc;
        }

        public Collection<BibDocument> getDocuments() {
            SortedSet<BibDocument> docs =
                    orderedDocuments.get(currentOrder);
            if (docs == null) {
               docs = new TreeSet<BibDocument>(currentOrder);
               docs.addAll(documents.values());
               //Collections.sort(docs, currentOrder);
               orderedDocuments.put(currentOrder, docs);
            }
            return docs;
        }

        public List<BibDocument> getDocumentsWithName(String name) {
            List<BibDocument> result = new LinkedList<BibDocument>();
            for (BibDocument doc : documents.values())
                if (doc.getFile() != null && doc.getFile().getName().
                        compareToIgnoreCase(name) == 0)
                    result.add(doc);
            return result;
        }

        public BibDocument getDocumentWithKey(String key) {
            return documents.get(key);
        }

        public List<BibDocument> getDocuments(File file) {
            List<BibDocument> result = new LinkedList<BibDocument>();
            for (BibDocument doc : documents.values())
                if (doc.getFile() != null && doc.getFile().equals(file))
                    result.add(doc);
            return result;
        }

        public boolean hasDocuments(File file) {
            return !getDocuments(file).isEmpty();
        }

        public boolean hasDocumentsWithName(String name) {
            return !getDocumentsWithName(name).isEmpty();
        }

        public boolean hasDocumentWithKey(String key) {
            return getDocumentWithKey(key) != null;
        }

        public boolean addTag(String tag) {
            return tags.add(tag);
        }

        public boolean addTag(String key, String tag) {
            BibDocument doc = documents.get(key);
            return addTag(doc, tag);
        }

        public boolean addTag(BibDocument doc, String tag) {
            if (doc == null) return false;
            boolean added = doc.addTag(tag);
            tags.add(tag);
            untaggedDocuments = null;
            return added;
        }

        public boolean removeTag(String tag) {
            boolean removed = tags.remove(tag);
            for (BibDocument doc : documents.values())
                removeTag(doc, tag);
            return removed;
        }

        public boolean removeTag(String key, String tag) {
            BibDocument doc = documents.get(key);
            return removeTag(doc, tag);
        }

        public boolean removeTag(BibDocument doc, String tag) {
            if (doc == null) return false;
            boolean removed = doc.removeTag(tag);
            untaggedDocuments = null;
            return removed;
        }
        
        public Collection<String> getTags() {
            return tags;
        }

        public Collection<BibDocument> getTaggedDocuments(String... tags) {
            return getTaggedDocuments(Arrays.asList(tags));
        }

        public Collection<BibDocument> getTaggedDocuments(List<String> tags) {
            List<BibDocument> result = new LinkedList<BibDocument>();
            for (BibDocument doc : documents.values())
                for (String tag : tags)
                    if (doc.hasTag(tag)) {
                        result.add(doc);
                        break;
                    }
            return result;
        }

        public Collection<BibDocument> getUntaggedDocuments() {
            if (untaggedDocuments == null) {
                untaggedDocuments = new TreeSet<BibDocument>(currentOrder);
                for (BibDocument doc : documents.values())
                    if (doc.getTags().isEmpty()) untaggedDocuments.add(doc);
                //Collections.sort(untaggedDocuments, currentOrder);
            }
            return untaggedDocuments;
        }

        public Collection<BibDocument> getUnattachedDocuments() {
            if (unattachedDocuments == null) {
                unattachedDocuments = new TreeSet<BibDocument>(currentOrder);
                for (BibDocument doc : documents.values())
                    if (doc.getFile() == null) unattachedDocuments.add(doc);
                //Collections.sort(unattachedDocuments, currentOrder);
            }
            return unattachedDocuments;
        }

        public void setCurrentOrder(BibDocumentComparator currentOrder) {
            this.currentOrder = currentOrder;
            //// INVALIDATE VIEWS ////
            unattachedDocuments = null;
            untaggedDocuments = null;
            //////////////////////////
        }

        public BibDocumentComparator getCurrentOrder() {
            return currentOrder;
        }

        public boolean hasTag(String tag) {
            return tags.contains(tag);
        }

        public boolean hasTaggedDocuments(String tag) {
            for (BibDocument doc : documents.values())
                if (doc.hasTag(tag)) return true;
            return false;
        }

        public void setFile(BibDocument doc, File file) {
            if (doc == null) return;
            doc.setFile(file);
            unattachedDocuments = null;
        }

        public BibtexFile getBibtexFile() {
            return bibtexFile;
        }

        public void setBibtexEntry(String key, BibtexEntry entry) {
            BibDocument doc = documents.get(key);
            setBibtexEntry(doc, entry);
        }

        public void setBibtexEntry(BibDocument doc, BibtexEntry entry) {
            if (doc == null) return;
            BibtexEntry old = doc.getEntry();
            if (old != null) bibtexFile.removeEntry(old);
            if (entry != null) bibtexFile.addEntry(entry);
            doc.setEntry(entry);
        }

        public void setKeyFont(Font keyFont) {
            this.keyFont = keyFont;
        }

        public void setTitleFont(Font titleFont) {
            this.titleFont = titleFont;
        }

        public void setYearFont(Font yearFont) {
            this.yearFont = yearFont;
        }

        public void setAuthorFont(Font authorFont) {
            this.authorFont = authorFont;
        }

        public Font getKeyFont() {
            return keyFont;
        }

        public Font getTitleFont() {
            return titleFont;
        }

        public Font getYearFont() {
            return yearFont;
        }

        public Font getAuthorFont() {
            return authorFont;
        }

        public Color getAuthorColor() {
            return authorColor;
        }

        public void setAuthorColor(Color authorColor) {
            this.authorColor = authorColor;
        }

        public Color getKeyColor() {
            return keyColor;
        }

        public void setKeyColor(Color keyColor) {
            this.keyColor = keyColor;
        }

        public Color getTitleColor() {
            return titleColor;
        }

        public void setTitleColor(Color titleColor) {
            this.titleColor = titleColor;
        }

        public Color getYearColor() {
            return yearColor;
        }

        public void setYearColor(Color yearColor) {
            this.yearColor = yearColor;
        }

        public void addPropertyChangeListener(String key,
                PropertyChangeListener listener) {
            BibDocument doc = getDocumentWithKey(key);
            if (doc != null) doc.addPropertyChangeListener(listener);
        }

        public void removePropertyChangeListener(String key,
                PropertyChangeListener listener) {
            BibDocument doc = getDocumentWithKey(key);
            if (doc != null) doc.removePropertyChangeListener(listener);
        }
    }

    protected static ImageIcon defaultThumbnail =
            ImageUtilities.getImageIconSafe(SuperReferencerApp.class,
                "resources/document_64.png");

    protected static ImageIcon defaultTrayIcon =
            ImageUtilities.getImageIconSafe(SuperReferencerApp.class,
                "resources/document-text.gif");
  
    public static ImageIcon getDefaultDocumentThumbnail() {
        return defaultThumbnail;
    }

    public Collection<BibDocument> getDocuments() {
        return documents.getDocuments();
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of SuperReferencerApp
     */
    public static SuperReferencerApp getApplication() {
        return Application.getInstance(SuperReferencerApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(SuperReferencerApp.class, args);
    }

    @Override
    protected void initApplication() {
        super.initApplication();
        imagesDir = new File(getSettingsDirectory(), "images");
        if (!imagesDir.exists()) imagesDir.mkdirs();
        documents = new BibDocuments();
        setTrayImageIcon(defaultTrayIcon);
        setFrameIcon(defaultTrayIcon);
    }

    @Override
    protected boolean checkChanges() {
        return false;
    }

    @Override
    protected void createSettingsDirectory() {
        if (!imagesDir.exists()) imagesDir.mkdirs();
    }
    
    @Override
    protected void populateSettingsDirectory() {}

    @Override
    protected void update() throws Exception {}

    @Override
    protected String initSettingsDirectory() {
        return ".superreferencer";
    }

    @Override
    protected URL initWebLocation() {
        return null;
    }

    @Override
    protected String initName() {
        return "Super Referencer";
    }

    @Override
    protected Long initUpdateInterval() {
        return null;
    }

    public BibDocument addDocument(String key, File file, BibtexEntry entry) {
        documents.addDocument(key, file, entry);
        BibDocument doc = documents.getDocumentWithKey(key);
        doc.setDefaultThumbnail(defaultThumbnail);
        return doc;
    }

    public void addDocumentPropertyChangeListener(String key,
            PropertyChangeListener listener) {
        documents.addPropertyChangeListener(key, listener);
    }

    public BibDocument removeDocument(String key) {
        BibDocument doc = documents.removeDocument(key);
        return doc;
    }

    public List<BibDocument> removeDocumentsWithName(String name) {
        List<BibDocument> result = documents.removeDocumentsWithName(name);
        return result;
    }

    public boolean removeDocument(BibDocument doc) {
        boolean result = documents.removeDocument(doc);
        return result;
    }
    
    public Collection<BibDocument> getDocuments(String search)
            throws ParseException, TokenMgrError {
        Parser parser = new Parser(new StringReader(search));
        IASTExpression ast = parser.Start();

        List<BibDocument> res = new LinkedList<BibDocument>();
        for (BibDocument doc : getDocuments()) {
            if (ast.evaluate(doc, false)) {
                res.add(doc);
            }
        }
        return res;
    }

    public boolean hasDocuments(File file) {
        return documents.hasDocuments(file);
    }

    public boolean hasDocumentsWithName(String name) {
        return documents.hasDocumentsWithName(name);
    }

    public boolean hasDocumentWithKey(String key) {
        return documents.hasDocumentWithKey(key);
    }

    public void updateDocument(BibDocument doc, File file, BibtexEntry entry) {
        documents.setBibtexEntry(doc, entry);
        documents.setFile(doc, file);
    }

    public boolean addTag(BibDocument doc, String tag) {
        boolean res = documents.addTag(doc, tag);
        return res;
    }

    public boolean addTag(String tag) {
        boolean res = documents.addTag(tag);
        return res;
    }

    public boolean removeTag(String tag) {
        boolean res = documents.removeTag(tag);
        return res;
    }

    public boolean removeTag(BibDocument doc, String tag) {
        boolean res = documents.removeTag(doc, tag);
        return res;
    }

    public Collection<String> getTags() {
        return documents.getTags();
    }

    public boolean hasTag(String tag) {
        return documents.hasTag(tag);
    }

    public Collection<BibDocument> getTaggedDocuments(String... tags) {
        return documents.getTaggedDocuments(tags);
    }

    public Collection<BibDocument> getUntaggedDocuments() {
        return documents.getUntaggedDocuments();
    }

    public Collection<BibDocument> getUnattachedDocuments() {
        return documents.getUnattachedDocuments();
    }

    public BibtexFile getBibtexFile() {
        return documents.getBibtexFile();
    }

    public void setDocumentsKeyFont(Font keyFont) {
        documents.setKeyFont(keyFont);
    }

    public void setDocumentsTitleFont(Font titleFont) {
        documents.setTitleFont(titleFont);
    }

    public void setDocumentsYearFont(Font yearFont) {
        documents.setYearFont(yearFont);
    }

    public void setDocumentsAuthorFont(Font authorFont) {
        documents.setAuthorFont(authorFont);
    }

    public Font getDocumentsKeyFont() {
        return documents.getKeyFont();
    }

    public Font getDocumentsTitleFont() {
        return documents.getTitleFont();
    }

    public Font getDocumentsYearFont() {
        return documents.getYearFont();
    }

    public Font getDocumentsAuthorFont() {
        return documents.getAuthorFont();
    }

    public void setDocumentsKeyColor(Color keyColor) {
        documents.setKeyColor(keyColor);
    }

    public void setDocumentsTitleColor(Color titleColor) {
        documents.setTitleColor(titleColor);
    }

    public void setDocumentsYearColor(Color yearColor) {
        documents.setYearColor(yearColor);
    }

    public void setDocumentsAuthorColor(Color authorColor) {
        documents.setAuthorColor(authorColor);
    }

    public Color getDocumentsKeyColor() {
        return documents.getKeyColor();
    }

    public Color getDocumentsTitleColor() {
        return documents.getTitleColor();
    }

    public Color getDocumentsYearColor() {
        return documents.getYearColor();
    }

    public Color getDocumentsAuthorColor() {
        return documents.getAuthorColor();
    }

    public void printBibtexFile(PrintWriter writer) {
        documents.getBibtexFile().printBibtex(writer);
    }

    public void printBibtexFile(PrintWriter writer, String tag) {
        Collection<BibDocument> taggedDocuments =
                documents.getTaggedDocuments(tag);
        printBibtexFile(writer, taggedDocuments);
    }

    public void printUntaggedBibtexFile(PrintWriter writer) {
        Collection<BibDocument> untaggedDocuments = 
                documents.getUntaggedDocuments();
        printBibtexFile(writer, untaggedDocuments);
    }

    public void printUnattachedBibtexFile(PrintWriter writer) {
        Collection<BibDocument> unattachedDocuments =
                documents.getUnattachedDocuments();
        printBibtexFile(writer, unattachedDocuments);
    }

    public void printBibtexFile(PrintWriter writer, BibDocument... docs) {
        List<BibDocument> asList = Arrays.asList(docs);
        printBibtexFile(writer, asList);
    }

    public void printBibtexFile(PrintWriter writer,
            Collection<BibDocument> docs) {
        for (BibDocument doc : docs) {
            doc.getEntry().printBibtex(writer);
            writer.println();
        }
    }

    public BibDocumentComparator getComparator() {
        return documents.getCurrentOrder();
    }

    public void setComparator(BibDocumentComparator comparator) {
        documents.setCurrentOrder(comparator);
    }
}

