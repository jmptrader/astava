package astava.parse3.tree;

import astava.core.Atom;
import astava.core.Node;
import astava.core.Tuple;
import astava.parse3.Cursor;
import astava.parse3.Matcher;
import astava.parse3.Parser;

import java.util.Hashtable;
import java.util.List;

public class OpRouter implements Parser<Tuple, Node> {
    private Hashtable<Object, Parser<Tuple, Node>> map = new Hashtable<>();

    public OpRouter put(Object operatorValue, Parser<Tuple, Node> parser) {
        map.put(operatorValue, parser);
        return this;
    }

    @Override
    public void parse(Cursor<Tuple> cursor, Matcher<Tuple, Node> matcher) {
        Atom operator = getOperator(cursor.peek());

        if(operator != null) {
            Parser<Tuple, Node> parser = map.get(operator.getValue());

            if(parser != null) {
                parser.parse(cursor, matcher);
                return;
            }
        }

        matcher.visitFailure();
    }

    private static Atom getOperator(Tuple tuple) {
        if(tuple.size() > 0) {
            Node operatorCandidate = tuple.get(0);
            if(operatorCandidate instanceof Atom) {
                return (Atom)operatorCandidate;
            }
        }

        return null;
    }
}
