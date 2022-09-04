/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * EntryTypeDataPanel.java
 *
 * Created on 27/Out/2011, 12:43:09
 */

package superreferencer;

import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexEntry;
import bibtex.dom.BibtexFile;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import pt.unl.fct.di.tsantos.util.ImageUtilities;
import pt.unl.fct.di.tsantos.util.Tuple;
import pt.unl.fct.di.tsantos.util.collection.ArraysExtended;
import pt.unl.fct.di.tsantos.util.string.StringUtils;
import pt.unl.fct.di.tsantos.util.swing.SpringUtilities;

/**
 *
 * @author tvcsantos
 */
public class EntryTypeDataPanel extends javax.swing.JPanel {

    public enum BibtexEntryType {
        ARTICLE {
            public BibtexField[] getRequiredFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE, BibtexField.JOURNAL,
                    BibtexField.YEAR
                };
            }

            public BibtexField[] getOptionalFields() {
                return new BibtexField[]{
                    BibtexField.VOLUME, BibtexField.NUMBER,
                    BibtexField.PAGES, BibtexField.MONTH, BibtexField.NOTE,
                    BibtexField.KEY
                };
            }

            public BibtexField[] getFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE, BibtexField.JOURNAL,
                    BibtexField.YEAR, BibtexField.VOLUME, BibtexField.NUMBER,
                    BibtexField.PAGES, BibtexField.MONTH, BibtexField.NOTE,
                    BibtexField.KEY
                };
            }
        },
        BOOK {
            @Override
            public BibtexField[] getAlternatives(BibtexField field) {
                switch (field) {
                    case AUTHOR: return getArray(BibtexField.EDITOR);
                    case EDITOR: return getArray(BibtexField.AUTHOR);
                    case VOLUME: return getArray(BibtexField.NUMBER);
                    case NUMBER: return getArray(BibtexField.VOLUME);
                    default: return new BibtexField[]{};
                }
            }

            public BibtexField[] getRequiredFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.EDITOR, BibtexField.TITLE,
                    BibtexField.PUBLISHER, BibtexField.YEAR
                };
            }

            public BibtexField[] getOptionalFields() {
                return new BibtexField[]{
                    BibtexField.VOLUME,
                    BibtexField.NUMBER, BibtexField.SERIES, BibtexField.ADDRESS,
                    BibtexField.EDITION, BibtexField.MONTH, BibtexField.NOTE,
                    BibtexField.KEY
                };
            }

            public BibtexField[] getFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.EDITOR, BibtexField.TITLE,
                    BibtexField.PUBLISHER, BibtexField.YEAR, BibtexField.VOLUME,
                    BibtexField.NUMBER, BibtexField.SERIES, BibtexField.ADDRESS,
                    BibtexField.EDITION, BibtexField.MONTH, BibtexField.NOTE,
                    BibtexField.KEY
                };
            }
        },
        BOOKLET {
            public BibtexField[] getRequiredFields() {
                return new BibtexField[]{
                    BibtexField.TITLE
                };
            }

            public BibtexField[] getOptionalFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR,
                    BibtexField.HOWPUBLISHED, BibtexField.ADDRESS,
                    BibtexField.MONTH, BibtexField.YEAR, BibtexField.NOTE,
                    BibtexField.KEY
                };
            }

            public BibtexField[] getFields() {
                return new BibtexField[]{
                    BibtexField.TITLE, BibtexField.AUTHOR,
                    BibtexField.HOWPUBLISHED, BibtexField.ADDRESS,
                    BibtexField.MONTH, BibtexField.YEAR, BibtexField.NOTE,
                    BibtexField.KEY
                };
            }
        },
        CONFERENCE {
            @Override
            public BibtexField[] getAlternatives(BibtexField field) {
                switch (field) {
                    case VOLUME: return getArray(BibtexField.NUMBER);
                    case NUMBER: return getArray(BibtexField.VOLUME);
                    default: return new BibtexField[]{};
                }
            }

            public BibtexField[] getRequiredFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE,
                    BibtexField.BOOKTITLE, BibtexField.YEAR,
                };
            }

            public BibtexField[] getOptionalFields() {
                return new BibtexField[]{
                    BibtexField.EDITOR, BibtexField.VOLUME, BibtexField.NUMBER,
                    BibtexField.SERIES, BibtexField.PAGES, BibtexField.ADDRESS,
                    BibtexField.MONTH, BibtexField.ORGANIZATION,
                    BibtexField.PUBLISHER, BibtexField.NOTE, BibtexField.KEY
                };
            }

            public BibtexField[] getFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE,
                    BibtexField.BOOKTITLE, BibtexField.YEAR,
                    BibtexField.EDITOR, BibtexField.VOLUME, BibtexField.NUMBER,
                    BibtexField.SERIES, BibtexField.PAGES, BibtexField.ADDRESS,
                    BibtexField.MONTH, BibtexField.ORGANIZATION,
                    BibtexField.PUBLISHER, BibtexField.NOTE, BibtexField.KEY
                };
            }
        },
        INBOOK {
            @Override
            public BibtexField[] getAlternatives(BibtexField field) {
                switch (field) {
                    case AUTHOR: return getArray(BibtexField.EDITOR);
                    case EDITOR: return getArray(BibtexField.AUTHOR);
                    case CHAPTER: return getArray(BibtexField.PAGES);
                    case PAGES: return getArray(BibtexField.CHAPTER);
                    case VOLUME: return getArray(BibtexField.NUMBER);
                    case NUMBER: return getArray(BibtexField.VOLUME);
                    default: return new BibtexField[]{};
                }
            }

            public BibtexField[] getRequiredFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.EDITOR, BibtexField.TITLE,
                    BibtexField.CHAPTER, BibtexField.PAGES,
                    BibtexField.PUBLISHER, BibtexField.YEAR
                };
            }

            public BibtexField[] getOptionalFields() {
                return new BibtexField[]{
                    BibtexField.VOLUME,
                    BibtexField.NUMBER, BibtexField.SERIES, BibtexField.TYPE,
                    BibtexField.ADDRESS, BibtexField.EDITION, BibtexField.MONTH,
                    BibtexField.NOTE, BibtexField.KEY
                };
            }

            public BibtexField[] getFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.EDITOR, BibtexField.TITLE,
                    BibtexField.CHAPTER, BibtexField.PAGES,
                    BibtexField.PUBLISHER, BibtexField.YEAR, BibtexField.VOLUME,
                    BibtexField.NUMBER, BibtexField.SERIES, BibtexField.TYPE,
                    BibtexField.ADDRESS, BibtexField.EDITION, BibtexField.MONTH,
                    BibtexField.NOTE, BibtexField.KEY
                };
            }
        },
        INCOLLECTION {
            @Override
            public BibtexField[] getAlternatives(BibtexField field) {
                switch (field) {
                    case VOLUME: return getArray(BibtexField.NUMBER);
                    case NUMBER: return getArray(BibtexField.VOLUME);
                    default: return new BibtexField[]{};
                }
            }

            public BibtexField[] getRequiredFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE,
                    BibtexField.BOOKTITLE, BibtexField.PUBLISHER,
                    BibtexField.YEAR
                };
            }

            public BibtexField[] getOptionalFields() {
                return new BibtexField[]{
                    BibtexField.EDITOR, BibtexField.VOLUME,
                    BibtexField.NUMBER, BibtexField.SERIES, BibtexField.TYPE,
                    BibtexField.CHAPTER, BibtexField.PAGES, BibtexField.ADDRESS,
                    BibtexField.EDITION, BibtexField.MONTH, BibtexField.NOTE,
                    BibtexField.KEY
                };
            }

            public BibtexField[] getFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE, 
                    BibtexField.BOOKTITLE, BibtexField.PUBLISHER,
                    BibtexField.YEAR, BibtexField.EDITOR, BibtexField.VOLUME,
                    BibtexField.NUMBER, BibtexField.SERIES, BibtexField.TYPE,
                    BibtexField.CHAPTER, BibtexField.PAGES, BibtexField.ADDRESS,
                    BibtexField.EDITION, BibtexField.MONTH, BibtexField.NOTE,
                    BibtexField.KEY
                };
            }
        },
        INPROCEEDINGS {
            @Override
            public BibtexField[] getAlternatives(BibtexField field) {
                switch (field) {
                    case VOLUME: return getArray(BibtexField.NUMBER);
                    case NUMBER: return getArray(BibtexField.VOLUME);
                    default: return new BibtexField[]{};
                }
            }

            public BibtexField[] getRequiredFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE,
                    BibtexField.BOOKTITLE, BibtexField.YEAR,
                };
            }

            public BibtexField[] getOptionalFields() {
                return new BibtexField[]{
                    BibtexField.EDITOR, BibtexField.VOLUME, BibtexField.NUMBER,
                    BibtexField.SERIES, BibtexField.PAGES, BibtexField.ADDRESS,
                    BibtexField.MONTH, BibtexField.ORGANIZATION,
                    BibtexField.PUBLISHER, BibtexField.NOTE, BibtexField.KEY
                };
            }

            public BibtexField[] getFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE,
                    BibtexField.BOOKTITLE, BibtexField.YEAR,
                    BibtexField.EDITOR, BibtexField.VOLUME, BibtexField.NUMBER,
                    BibtexField.SERIES, BibtexField.PAGES, BibtexField.ADDRESS,
                    BibtexField.MONTH, BibtexField.ORGANIZATION,
                    BibtexField.PUBLISHER, BibtexField.NOTE, BibtexField.KEY
                };
            }
        },
        MANUAL {
            public BibtexField[] getRequiredFields() {
                return new BibtexField[]{
                    BibtexField.TITLE
                };
            }

            public BibtexField[] getOptionalFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR,
                    BibtexField.ORGANIZATION, BibtexField.ADDRESS,
                    BibtexField.EDITION, BibtexField.MONTH, BibtexField.YEAR,
                    BibtexField.NOTE, BibtexField.KEY
                };
            }

            public BibtexField[] getFields() {
                return new BibtexField[]{
                    BibtexField.TITLE, BibtexField.AUTHOR,
                    BibtexField.ORGANIZATION, BibtexField.ADDRESS,
                    BibtexField.EDITION, BibtexField.MONTH, BibtexField.YEAR,
                    BibtexField.NOTE, BibtexField.KEY
                };
            }
        },
        MASTERSTHESIS {
            public BibtexField[] getRequiredFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE, BibtexField.SCHOOL,
                    BibtexField.YEAR
                };
            }

            public BibtexField[] getOptionalFields() {
                return new BibtexField[]{
                    BibtexField.TYPE, BibtexField.ADDRESS,
                    BibtexField.MONTH, BibtexField.NOTE, BibtexField.KEY
                };
            }

            public BibtexField[] getFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE, BibtexField.SCHOOL, 
                    BibtexField.YEAR, BibtexField.TYPE, BibtexField.ADDRESS,
                    BibtexField.MONTH, BibtexField.NOTE, BibtexField.KEY
                };
            }
        },
        MISC {
            public BibtexField[] getRequiredFields() {
                return new BibtexField[]{};
            }

            public BibtexField[] getOptionalFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE,
                    BibtexField.HOWPUBLISHED, BibtexField.MONTH,
                    BibtexField.YEAR, BibtexField.NOTE, BibtexField.KEY
                };
            }

            public BibtexField[] getFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE, 
                    BibtexField.HOWPUBLISHED, BibtexField.MONTH,
                    BibtexField.YEAR, BibtexField.NOTE, BibtexField.KEY
                };
            }
        },
        PHDTHESIS {
            public BibtexField[] getRequiredFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE, BibtexField.SCHOOL,
                    BibtexField.YEAR
                };
            }

            public BibtexField[] getOptionalFields() {
                return new BibtexField[]{
                    BibtexField.TYPE, BibtexField.ADDRESS,
                    BibtexField.MONTH, BibtexField.NOTE, BibtexField.KEY
                };
            }

            public BibtexField[] getFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE, BibtexField.SCHOOL,
                    BibtexField.YEAR, BibtexField.TYPE, BibtexField.ADDRESS,
                    BibtexField.MONTH, BibtexField.NOTE, BibtexField.KEY
                };
            }
        },
        PROCEEDINGS {
            @Override
            public BibtexField[] getAlternatives(BibtexField field) {
                switch (field) {
                    case VOLUME: return getArray(BibtexField.NUMBER);
                    case NUMBER: return getArray(BibtexField.VOLUME);
                    default: return new BibtexField[]{};
                }
            }

            public BibtexField[] getRequiredFields() {
                return new BibtexField[]{
                    BibtexField.TITLE, BibtexField.YEAR
                };
            }

            public BibtexField[] getOptionalFields() {
                return new BibtexField[]{
                    BibtexField.EDITOR,
                    BibtexField.VOLUME, BibtexField.NUMBER, BibtexField.SERIES,
                    BibtexField.ADDRESS, BibtexField.MONTH,
                    BibtexField.PUBLISHER, BibtexField.ORGANIZATION,
                    BibtexField.NOTE, BibtexField.KEY
                };
            }

            public BibtexField[] getFields() {
                return new BibtexField[]{
                    BibtexField.TITLE, BibtexField.YEAR, BibtexField.EDITOR,
                    BibtexField.VOLUME, BibtexField.NUMBER, BibtexField.SERIES,
                    BibtexField.ADDRESS, BibtexField.MONTH,
                    BibtexField.PUBLISHER, BibtexField.ORGANIZATION,
                    BibtexField.NOTE, BibtexField.KEY
                };
            }
        },
        TECHREPORT {
            public BibtexField[] getRequiredFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE,
                    BibtexField.INSTITUTION, BibtexField.YEAR
                };
            }

            public BibtexField[] getOptionalFields() {
                return new BibtexField[]{
                    BibtexField.TYPE,
                    BibtexField.NUMBER, BibtexField.ADDRESS, BibtexField.MONTH,
                    BibtexField.NOTE, BibtexField.KEY
                };
            }

            public BibtexField[] getFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE,
                    BibtexField.INSTITUTION, BibtexField.YEAR, BibtexField.TYPE,
                    BibtexField.NUMBER, BibtexField.ADDRESS, BibtexField.MONTH,
                    BibtexField.NOTE, BibtexField.KEY
                };
            }
        },
        UNPUBLISHED {
            public BibtexField[] getRequiredFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE, BibtexField.NOTE
                };
            }

            public BibtexField[] getOptionalFields() {
                return new BibtexField[]{
                    BibtexField.MONTH, BibtexField.YEAR, BibtexField.KEY
                };
            }

            public BibtexField[] getFields() {
                return new BibtexField[]{
                    BibtexField.AUTHOR, BibtexField.TITLE, BibtexField.NOTE,
                    BibtexField.MONTH, BibtexField.YEAR, BibtexField.KEY
                };
            }
        };

        public boolean isRequired(String name) {
            name = name.toUpperCase();
            if (!isField(name)) return false;
            return isRequired(BibtexField.valueOf(name));
        }

        public boolean isRequired(BibtexField field) {
            return ArraysExtended.contains(getRequiredFields(), field);
        }

        public boolean isOptional(String name) {
            name = name.toUpperCase();
            if (!isField(name)) return false;
            return isOptional(BibtexField.valueOf(name));
        }

        public boolean isOptional(BibtexField field) {
            return ArraysExtended.contains(getOptionalFields(), field);
        }

        public boolean hasAlternative(String name) {
            name = name.toUpperCase();
            if (!isField(name)) return false;
            return hasAlternative(BibtexField.valueOf(name));
        }
        
        public boolean hasAlternative(BibtexField field) {
            return getAlternatives(field).length > 0;
        }

        public BibtexField[] getAlternatives(BibtexField field) {
            return new BibtexField[]{};
        }
        
        public abstract BibtexField[] getFields();
        public abstract BibtexField[] getRequiredFields();
        public abstract BibtexField[] getOptionalFields();

        public boolean isField(String name) {
            try {
                BibtexField field = BibtexField.valueOf(name.toUpperCase());
                return isField(field);
            } catch(IllegalArgumentException e) {
                return false;
            }
        }

        public boolean isField(BibtexField field) {
            return ArraysExtended.contains(getFields(), field);
        }

        public static BibtexField[] validate(BibtexEntry entry) {
            String type = entry.getEntryType();
            type = type.toUpperCase();
            BibtexEntryType theType = valueOf(type);
            List<BibtexField> result = new LinkedList<BibtexField>();
            for (BibtexField field : theType.getRequiredFields()) {
                BibtexAbstractValue fieldValue = 
                        entry.getFieldValue(field.toString().toLowerCase());
                if (fieldValue == null) {
                    BibtexField[] alternatives = theType.getAlternatives(field);
                    boolean hasAlternative = false;
                    for (BibtexField alt : alternatives) {
                        BibtexAbstractValue fieldValueAlt = 
                             entry.getFieldValue(alt.toString().toLowerCase());
                        if (fieldValueAlt != null) {
                            hasAlternative = true;
                            break;
                        }
                    }
                    if (!hasAlternative) result.add(field);
                }
            }
            return result.toArray(new BibtexField[]{});
        }

        public boolean isExtraField(String name) {
            try {
                BibtexField field = BibtexField.valueOf(name.toUpperCase());
                return !isField(field);
            } catch(IllegalArgumentException e) {
                return true;
            }
        }

        public static <T> T[] getArray(T ... elements) {
            return elements;
        }
    }

    public enum BibtexField {
        ADDRESS, AUTHOR, BOOKTITLE, CHAPTER, EDITION,
        EDITOR, HOWPUBLISHED, INSTITUTION, JOURNAL, KEY,
        MONTH, NOTE, NUMBER, ORGANIZATION, PAGES,
        PUBLISHER, SCHOOL, SERIES, TITLE, TYPE, VOLUME, YEAR
    }

    private class BoundJLabel extends JLabel {
        private Object bind;

        public BoundJLabel(Object bind) {
            super();
            this.bind = bind;
        }

        public Object getBind() {
            return bind;
        }

        public <T> T getBind(Class<T> aClass) {
            return aClass.cast(bind);
        }
    }

    //public static final String ADD_FIELD_ICON_PATH = "resources/plus.png";
    public static final String REMOVE_FIELD_ICON_PATH = "resources/cross.png";
    public static final String REQUIRED_ICON_PATH = 
            "resources/asterisk-small.png";
    public static final String ALTERNATIVE_ICON_PATH =
            "resources/asterisk-small-yellow.png";

    /*public static final ImageIcon addFieldIcon =
            ImageUtilities.getImageIconSafe(EntryTypeDataPanel.class,
            ADD_FIELD_ICON_PATH);*/

    public static final ImageIcon removeFieldIcon =
            ImageUtilities.getImageIconSafe(EntryTypeDataPanel.class,
            REMOVE_FIELD_ICON_PATH);
    public static final ImageIcon requiredFieldIcon =
            ImageUtilities.getImageIconSafe(EntryTypeDataPanel.class,
            REQUIRED_ICON_PATH);
    public static final ImageIcon alternativeFieldIcon =
            ImageUtilities.getImageIconSafe(EntryTypeDataPanel.class,
            ALTERNATIVE_ICON_PATH);

    protected Map<String, Tuple> map;
    protected BibtexEntryType type;
    protected BibtexEntry entry;

    private void boundJLabelMouseClicked(MouseEvent e) {
        BoundJLabel label = (BoundJLabel) e.getSource();
        String field = label.getBind(String.class);
        map.remove(field);
        if (entry != null) entry.undefineField(field);
        loadEntry(entry, this.type);
    }

    private MouseAdapter boundMouseAdapter;

    /** Creates new form EntryTypeDataPanel */
    public EntryTypeDataPanel() {
        initComponents();

        map = new HashMap<String, Tuple>();

        boundMouseAdapter = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    boundJLabelMouseClicked(e);
                }
            };

        for (BibtexField field : BibtexField.values()) {
            String fieldName = field.toString().toLowerCase();
            BoundJLabel label = new BoundJLabel(fieldName);
            label.setIcon(null);
            map.put(fieldName, new Tuple(
                    new JLabel(capitalize(fieldName) + ":"),
                    new JTextField(""), label));
        }

        SpringLayout layout = new SpringLayout();
        setLayout(layout);

        setEntryType(BibtexEntryType.ARTICLE);
    }

    public void setEntry(BibtexEntry entry) {
        this.entry = entry;
        if (entry == null) {
            //clear();
            if (this.type != null) 
                setEntryType(this.type);
            else setEntryType(BibtexEntryType.ARTICLE);
        } else loadEntry(entry);
    }

    private void loadEntry(BibtexEntry entry) {
        String strType = entry.getEntryType();
        BibtexEntryType entryType =
            BibtexEntryType.valueOf(strType.toUpperCase());
        loadEntry(entry, entryType);
    }

    private void loadEntry(BibtexEntry entry, BibtexEntryType entryType) {
        clear();
        this.type = entryType;
        BibtexField[] fields = type.getFields();
        removeAll();

        List<String> theFields = new LinkedList<String>();
        for (BibtexField field : fields)
            theFields.add(field.toString().toLowerCase());
        for (String key : entry.getFields().keySet()) {
            key = key.toLowerCase();
            if (key.equals("file")) continue;
            if (this.type.isExtraField(key)) {
                theFields.add(key);
            }
        }
        addFields(theFields);

        SpringUtilities.makeCompactGrid(this,
                                theFields.size() /*+ 1*/, 3, //rows, cols
                                6, 6,        //initX, initY
                                6, 6);

        setPreferredSize(null);
        Dimension size = getPreferredSize();
        setPreferredSize(size);

        fillFields(theFields, entry);

        SpringUtilities.makeCompactGrid(this,
                                theFields.size() /*+ 1*/, 3, //rows, cols
                                6, 6,        //initX, initY
                                6, 6);
        revalidate();
        repaint();
    }
    
    public BibtexEntry getEntry(BibtexFile owner, String key) {
        BibtexEntry newEntry =
                owner.makeEntry(type.toString(), key);
        for (Tuple tuple : map.values()) {
            JTextField tf = tuple.get(1, JTextField.class);
            if (tf.getText() == null || tf.getText().trim().isEmpty())
                continue;
            String field = tuple.get(0, JLabel.class).getText();
            field = field.substring(0, field.length() - 1); //remove :
            field = field.toLowerCase();
            newEntry.setField(field,
                    owner.makeString(StringUtils.escapeLatex(
                                            tf.getText().trim())));
        }
        return newEntry;
    }

    public String getCurrentTitle() {
        Tuple tuple = map.get("title");
        if (tuple == null) return null;
        JTextField tf = tuple.get(1, JTextField.class);
        if (tf.getText() == null || tf.getText().trim().isEmpty())
            return null;
        return tf.getText().trim();
    }
    
    public final void setEntryType(BibtexEntryType type) {
        this.type = type;
        loadType(this.type);
    }

    private void loadType(BibtexEntryType type) {
        clear();
        BibtexField[] fields = type.getFields();
        removeAll();
        
        addFields(fields);

        SpringUtilities.makeCompactGrid(this,
                                fields.length /*+ 1*/, 3, //rows, cols
                                6, 6,        //initX, initY
                                6, 6);

        setPreferredSize(null);
        Dimension size = getPreferredSize();
        setPreferredSize(size);

        SpringUtilities.makeCompactGrid(this,
                                fields.length /*+ 1*/, 3, //rows, cols
                                6, 6,        //initX, initY
                                6, 6);

        if (entry != null) loadEntry(entry, type);
        repaint();
    }

    private void clear() {
        for (Tuple tuple : map.values()) {
            JLabel label = tuple.get(0, JLabel.class);
            label.setIcon(null);
            tuple.get(1, JTextField.class).setText("");
            /*JLabel*/ label = tuple.get(2, JLabel.class);
            label.removeMouseListener(boundMouseAdapter);
            label.setIcon(null);
        }
    }

    private void addFields(BibtexField[] fields) {
        for (BibtexField field : fields) {
            Tuple tuple = map.get(
                    field.toString().toLowerCase());
            if (tuple == null) continue;
            JLabel label = tuple.get(0, JLabel.class);
            if (this.type.isRequired(field)) {
                if (this.type.hasAlternative(field))
                    label.setIcon(alternativeFieldIcon);
                else label.setIcon(requiredFieldIcon);
            }
            add(label);
            tuple.get(1, JTextField.class).setText("");
            add(tuple.get(1, JTextField.class));
            /*JLabel*/ label = tuple.get(2, JLabel.class);
            if (this.type.isExtraField(field.toString().toLowerCase())) {
                label.setIcon(removeFieldIcon);
                label.addMouseListener(boundMouseAdapter);
            }
            add(label);
        }
    }

    private void addFields(List<String> fields) {
        for (String field : fields) {
            Tuple tuple = map.get(field);
            if (tuple == null) {
                BoundJLabel label = new BoundJLabel(field);
                label.setIcon(null);
                tuple = new Tuple(
                        new JLabel(capitalize(field) + ":"),
                            new JTextField(""), label);
                map.put(field, tuple);
            }
            JLabel label = tuple.get(0, JLabel.class);
            if (this.type.isRequired(field)) {
                if (this.type.hasAlternative(field))
                    label.setIcon(alternativeFieldIcon);
                else label.setIcon(requiredFieldIcon);
            }
            add(label);
            //add(tuple.get(0, JLabel.class));
            tuple.get(1, JTextField.class).setText("");
            add(tuple.get(1, JTextField.class));
            /*JLabel*/ label = tuple.get(2, JLabel.class);
            if (this.type.isExtraField(field)) {
                label.setIcon(removeFieldIcon);
                label.addMouseListener(boundMouseAdapter);
            }
            add(label);
        }
    }

    private void fillFields(List<String> fields,
            BibtexEntry entry) {
        for (String field : fields) {
            Tuple tuple = map.get(field);
            if (tuple == null) continue;
            JTextField tf = tuple.get(1, JTextField.class);
            BibtexAbstractValue fieldValue = entry.getFieldValue(field);
            if (fieldValue != null) {
                String s = fieldValue.toString();
                Matcher m = Pattern.compile("\\{(.*)\\}").matcher(s);
                if (m.matches()) s = m.group(1);
                tf.setText(StringUtils.unescapeLatex(s));
                tf.setCaretPosition(0);
            }
        }
    }
    
    static String capitalize(String s) {
        if (s.isEmpty()) return s;
        String fl = s.substring(0,1);
        return fl.toUpperCase() + s.substring(1);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setName("Form"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

}
