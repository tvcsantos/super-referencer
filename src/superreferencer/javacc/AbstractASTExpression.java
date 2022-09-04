package superreferencer.javacc;

import superreferencer.SuperReferencerApp.BibDocuments.BibDocument;

/**
 * @author tvcsantos
 */
public abstract class AbstractASTExpression implements IASTExpression {

    public final boolean evaluate(BibDocument doc) {
        return evaluate(doc, true);
    }
}
