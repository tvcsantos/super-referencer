package superreferencer.javacc;

import bibtex.dom.BibtexEntry;
import java.util.regex.Pattern;
import superreferencer.SuperReferencerApp.BibDocuments.BibDocument;

/**
 * @author tvcsantos
 */
public class ASTString extends AbstractASTExpression {

    /** the string literal */
    protected String s;

    /**
     * Constructs a new ASTString with
     * the string literal in argument
     * @param s the string literal of the expression
     */
    public ASTString(String s) {
        this.s = s;
    }

    public boolean evaluate(BibDocument doc, boolean sensitive) {
        String text = doc.getText();
        if (text == null) return false;
        int flags = Pattern.DOTALL;
        if (!sensitive) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        return Pattern.compile(s, flags).matcher(text).find();
        //return name.indexOf(s) != -1;
    }
}
