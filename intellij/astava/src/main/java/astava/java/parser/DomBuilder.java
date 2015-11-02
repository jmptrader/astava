package astava.java.parser;

import astava.tree.CodeDom;
import astava.tree.StatementDom;

import java.io.Serializable;
import java.util.List;

public interface DomBuilder extends Serializable {
    void accept(DomBuilderVisitor visitor);

    default boolean test(CodeDom code, List<Object> captures) {
        return false;
    }

    default CodeDom map(List<Object> captures) {
        throw new UnsupportedOperationException("Cannot do map (yet?).");
    }
}
