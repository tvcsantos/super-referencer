package superreferencer.javacc;

import superreferencer.SuperReferencerApp.BibDocuments.BibDocument;

/**
 * @author tvcsantos
 */
public class ASTNot extends AbstractASTExpression {

    /** the expression to negate */
    protected IASTExpression expression;

    /**
     * Constructs a new ASTNot expression with
     * the expression in argument
     * @param e the expression to negate
     */
    public ASTNot(IASTExpression e) {
        expression = e;
    }

    public boolean evaluate(BibDocument doc, boolean sensitive) {
        return !expression.evaluate(doc, sensitive);
    }
}
