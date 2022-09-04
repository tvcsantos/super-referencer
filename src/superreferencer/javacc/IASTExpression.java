package superreferencer.javacc;

import superreferencer.SuperReferencerApp.BibDocuments.BibDocument;

/**
 * @author tvcsantos
 */
public interface IASTExpression {

    /**
     * Evaluate the abstract syntax tree.
     * @param name the string to match with.
     * @return true if all keywords in the string.
     */
    boolean evaluate(BibDocument doc);

    boolean evaluate(BibDocument doc, boolean sensitive);
}
