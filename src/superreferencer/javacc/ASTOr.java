package superreferencer.javacc;

import superreferencer.SuperReferencerApp.BibDocuments.BibDocument;

/**
 * @author tvcsantos
 */
public class ASTOr extends AbstractASTExpression {

    /** left expression of the OR */
    protected IASTExpression left;
    /** right expression of the OR */
    protected IASTExpression right;

    /**
     * Constructs a new ASTOr expression with
     * the left and right expressions in argument
     * @param l the left expression of the OR
     * @param r the right expression of the OR
     */
    public ASTOr(IASTExpression l, IASTExpression r) {
        left = l;
        right = r;
    }

    public boolean evaluate(BibDocument doc, boolean sensitive) {
        return left.evaluate(doc, sensitive)
                || right.evaluate(doc, sensitive);
    }
}
