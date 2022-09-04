package superreferencer.javacc;

import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexEntry;
import bibtex.dom.BibtexString;
import java.util.regex.Pattern;
import superreferencer.SuperReferencerApp.BibDocuments.BibDocument;

/**
 * @author tvcsantos
 */
public class ASTAuthor extends AbstractASTExpression {

    /** the string literal */
    protected String s;

    /**
     * Constructs a new ASTString with
     * the string literal in argument
     * @param s the string literal of the expression
     */
    public ASTAuthor(String s) {
        this.s = s;
    }

    /*public boolean evaluate(String name, boolean sensitive) {
        int flags = Pattern.DOTALL;
        if (!sensitive) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        return Pattern.compile(s, flags).matcher(name).find();
        //return name.indexOf(s) != -1;
    }*/

    public boolean evaluate(BibDocument doc, boolean sensitive) {
        BibtexEntry entry = doc.getEntry();
        if (entry == null) return false;
        BibtexAbstractValue fieldValue = entry.getFieldValue("author");
        if (fieldValue == null) return false;
        String str = null;
        if (fieldValue instanceof BibtexString) {
            BibtexString bibtexString = (BibtexString) fieldValue;
            str = bibtexString.getContent();
        } else str = fieldValue.toString();
        if (str == null) return false;
        
        int flags = Pattern.DOTALL;
        if (!sensitive) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        return Pattern.compile(s, flags).matcher(str).find();
        //return name.indexOf(s) != -1;
    }
}
