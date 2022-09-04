/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package superreferencer;

import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexEntry;
import bibtex.dom.BibtexPerson;
import bibtex.dom.BibtexPersonList;
import bibtex.dom.BibtexString;
import bibtex.expansions.ExpansionException;
import bibtex.expansions.MacroReferenceExpander;
import bibtex.expansions.PersonListExpander;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.io.File;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import net.sourceforge.tuned.FileUtilities;
import pt.unl.fct.di.tsantos.util.string.StringUtils;
import superreferencer.SuperReferencerApp.BibDocuments.BibDocument;

/**
 *
 * @author tvcsantos
 */
public class BibDocumentListCellRenderer extends JLabel
        implements ListCellRenderer,
        Serializable {

    public static final Font SMALL_FONT = new Font("Dialog", 0, 10);

    //private Color gridColor = new Color(205, 205, 205);

    private Color evenRowColor = Color.WHITE;
    private Color oddRowColor = new Color(236, 243, 254);
    private Color selectedRowColor = new Color(61, 128, 223);


    private JPanel currentPanel;
    private JPanel leftPanel;
    private JPanel rightPanel;

    private JLabel iconLabel;
    private JLabel typeLabel;
    private JLabel keyLabel;
    private JLabel titleLabel;
    private JLabel yearLabel;
    private JLabel authorLabel;

    private Border lineBorder;

    private Font typeFont;
    private Color typeColor;
    private Font keyFont;
    private Color keyColor;
    private Font titleFont;
    private Color titleColor;
    private Font yearFont;
    private Color yearColor;
    private Font authorFont;
    private Color authorColor;

    public void setTypeFont(Font typeFont) {
        this.typeFont = typeFont;
        typeLabel.setFont(this.typeFont);
    }

    public void setKeyFont(Font keyFont) {
        this.keyFont = keyFont;
        keyLabel.setFont(this.keyFont);
    }

    public void setTitleFont(Font titleFont) {
        this.titleFont = titleFont;
        titleLabel.setFont(this.titleFont);
    }

    public void setYearFont(Font yearFont) {
        this.yearFont = yearFont;
        yearLabel.setFont(this.yearFont);
    }

    public void setAuthorFont(Font authorFont) {
        this.authorFont = authorFont;
        authorLabel.setFont(this.authorFont);
    }

    public void setTypeColor(Color typeColor) {
        this.typeColor = typeColor;
        typeLabel.setForeground(this.typeColor);
    }

    public void setKeyColor(Color keyColor) {
        this.keyColor = keyColor;
        keyLabel.setForeground(this.keyColor);
    }

    public void setTitleColor(Color titleColor) {
        this.titleColor = titleColor;
        titleLabel.setForeground(this.titleColor);
    }

    public void setYearColor(Color yearColor) {
        this.yearColor = yearColor;
        this.yearLabel.setForeground(this.yearColor);
    }

    public void setAuthorColor(Color authorColor) {
        this.authorColor = authorColor;
        this.authorLabel.setForeground(this.authorColor);
    }

    public BibDocumentListCellRenderer() {
	super();
        currentPanel = new JPanel();
        leftPanel = new JPanel();
        rightPanel = new JPanel();
        lineBorder = javax.swing.BorderFactory.
            createLineBorder(Color.BLACK);
        currentPanel.setLayout(null);
        currentPanel.removeAll();
        leftPanel.setLayout(null);
        leftPanel.removeAll();
        rightPanel.setLayout(null);
        rightPanel.removeAll();
        typeLabel = new JLabel();
        keyLabel = new JLabel();
        titleLabel = new JLabel();
        yearLabel = new JLabel();
        authorLabel = new JLabel();
        iconLabel = new JLabel();
        iconLabel.setIcon(null);

        typeFont = SMALL_FONT;
        keyFont = SMALL_FONT;
        titleFont = SMALL_FONT;
        yearFont = SMALL_FONT;
        authorFont = SMALL_FONT;
        typeColor = Color.BLACK;
        keyColor = Color.BLACK;
        titleColor = Color.BLACK;
        yearColor = Color.BLACK;
        authorColor = Color.BLACK;

        BoxLayout layout = new BoxLayout(currentPanel, BoxLayout.X_AXIS);
        currentPanel.setLayout(layout);

        leftPanel.setBackground(null);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(iconLabel);
        leftPanel.add(Box.createVerticalStrut(5));

        //currentPanel.add(iconLabel);
        currentPanel.add(leftPanel);

        currentPanel.add(Box.createHorizontalStrut(5));

        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        rightPanel.add(Box.createVerticalStrut(5));

        typeLabel.setFont(typeFont);
        typeLabel.setForeground(typeColor);
        rightPanel.add(typeLabel);

        keyLabel.setFont(keyFont);
        keyLabel.setForeground(keyColor);
        rightPanel.add(keyLabel);

        titleLabel.setFont(titleFont);
        titleLabel.setForeground(titleColor);
        rightPanel.add(titleLabel);

        yearLabel.setFont(yearFont);
        yearLabel.setForeground(yearColor);
        rightPanel.add(yearLabel);

        authorLabel.setFont(authorFont);
        authorLabel.setForeground(authorColor);
        rightPanel.add(authorLabel);

        rightPanel.add(Box.createVerticalStrut(5));

        currentPanel.add(rightPanel);

        currentPanel.add(Box.createHorizontalStrut(5));
    }

    private void revalidatePanel() {
        typeLabel.revalidate();
        keyLabel.revalidate();
        titleLabel.revalidate();
        yearLabel.revalidate();
        authorLabel.revalidate();
        iconLabel.revalidate();
        leftPanel.revalidate();
        rightPanel.revalidate();
        currentPanel.revalidate();
        //revalidate();
    }
    
    /**
     * Returns the appropriate background color for the given row.
     */
    protected Color colorForRow(int row, boolean isSelected) {
        if (isSelected) {
            return selectedRowColor;
        }
        if ((row % 2) == 0) {
            return evenRowColor;
        } else {
            return oddRowColor;
        }
    }

    /**
     * @param color Current font color
     * @param isSelected If font is selected
     * @return Font color for text
     */
    private Color getFontColor(Color color, boolean isSelected) {
        if (isSelected) {
            return Color.WHITE;
        } else {
            return color;
        }
    }

    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {

        BibDocument doc = (BibDocument)value;
        File file = doc.getFile();
        //if (doc.getThumbnail() != null) {
            iconLabel.setIcon(doc.getThumbnail());
            if (doc.hasNonDefaultThumbnail()) iconLabel.setBorder(lineBorder);
            else iconLabel.setBorder(null);
        //}

        String type = "";
        String key = file != null ? 
            FileUtilities.getNameWithoutExtension(file.getName()) : null;
        String title = "";
        String year = "";
        String author = "";

        BibtexEntry entry = doc.getEntry();
        if (entry != null) {
            try {
                MacroReferenceExpander macroExpander =
                        new MacroReferenceExpander(true, false, false, true);
                macroExpander.expand(entry.getOwnerFile());
            } catch(ExpansionException e) {}
            try {
                PersonListExpander personExpander =
                        new PersonListExpander(true, true, false);
                personExpander.expand(entry.getOwnerFile());
            } catch(ExpansionException e) {

            }
            if (!entry.getEntryType().isEmpty())
                type = entry.getEntryType();
            if (!entry.getEntryKey().isEmpty())
                key = entry.getEntryKey();
            BibtexAbstractValue fieldValue = entry.getFieldValue("title");
            if (fieldValue != null) {
                if (fieldValue instanceof BibtexString)
                    title = ((BibtexString)fieldValue).getContent();
                else title = fieldValue.toString();
            }
            fieldValue = entry.getFieldValue("year");
            if (fieldValue != null) year = fieldValue.toString();
            fieldValue = entry.getFieldValue("author");
            if (fieldValue != null) {
                if (fieldValue instanceof BibtexPersonList) {
                    BibtexPersonList bplist = (BibtexPersonList) fieldValue;
                    List<BibtexPerson> plist = bplist.getList();
                    Iterator<BibtexPerson> it = plist.iterator();
                    if (it.hasNext()) {
                        BibtexPerson person = it.next();
                        author = person.toString();
                    }
                    if (it.hasNext()) author += " et al.";
                }
            }
        }

        currentPanel.setBackground(colorForRow(index, isSelected));

        // icon
        iconLabel.setForeground(this.getFontColor(Color.BLACK, isSelected));

        rightPanel.setBackground(colorForRow(index, isSelected));

        // type
        typeLabel.setText(type);
        typeLabel.setForeground(
                this.getFontColor(this.typeColor, isSelected));

        // key
        keyLabel.setText(key);
        keyLabel.setForeground(
                this.getFontColor(this.keyColor, isSelected));
        // title
        titleLabel.setText(StringUtils.latexStringToHTML(
                StringUtils.unescapeLatex(title), 25)/*, true, false)*/);
        titleLabel.setForeground(
                this.getFontColor(this.titleColor, isSelected));
        // year
        yearLabel.setText(year);
        yearLabel.setForeground(this.getFontColor(this.yearColor, isSelected));
        // author
        authorLabel.setText(StringUtils.latexStringToHTML(
                StringUtils.unescapeLatex(author))/*, false, true)*/);
        authorLabel.setForeground(
                this.getFontColor(this.authorColor, isSelected));

        revalidatePanel();
        //System.out.println("renderer");

        return currentPanel;
    }
}
