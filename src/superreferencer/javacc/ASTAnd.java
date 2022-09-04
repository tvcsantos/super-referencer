package superreferencer.javacc;

import superreferencer.SuperReferencerApp.BibDocuments.BibDocument;

/**
 * @author tvcsantos
 */
public class ASTAnd extends AbstractASTExpression {

    /** left expression of the AND */
    protected IASTExpression left;
    /** right expression of the AND */
    protected IASTExpression right;

    /**
     * Constructs a new ASTAnd expression with
     * the left and right expressions in argument
     * @param l the left expression of the AND
     * @param r the right expression of the AND
     */
    public ASTAnd(IASTExpression l, IASTExpression r) {
        left = l;
        right = r;
    }

    public boolean evaluate(BibDocument doc, boolean sensitive) {
        return left.evaluate(doc, sensitive)
                && right.evaluate(doc, sensitive);
    }
}
