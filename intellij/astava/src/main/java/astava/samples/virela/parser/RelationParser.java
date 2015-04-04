package astava.samples.virela.parser;

import astava.parse.*;
import astava.parse.charsequence.CharParse;

import java.util.function.Function;

public class RelationParser extends DelegateParser<Character, Relation> {
    private <TOut> Parser<Character, TOut> ws() {
        return new SkipParser<>(CharParse.<TOut>isWhitespace().then(Parse.consume()).multi());
    }

    private Parser<Character, Expression> ws =
        new SkipParser<>(CharParse.<Expression>isWhitespace().then(Parse.consume()).multi());

    private Parser<Character, Expression> intValue = Parse.reduceInt(
        CharParse.<Character>isDigit().then(Parse.copy()).then(Parse.consume()).onceOrMore(),
        i -> v -> v.visitIntLiteral(i)
    );
    private Parser<Character, Expression> intStream =
        CharParse.<Expression>isChars("int").then(Parse.put(v -> v.visitIntStream()));
    private Parser<Character, Expression> mulExpression =
        ref(() -> this.leafExpression).wrap((cursor, matcher) -> leProduction -> {
            Expression leafExpression = leProduction.cursor().peek();

            multOperation(leafExpression, cursor, matcher);
        });
    private Parser<Character, Integer> multOperator =
        (
            CharParse.<Integer>isChar('*').then(Parse.put(ExpressionVisitor.BINARY_OPERATOR_MUL))
            .or(CharParse.<Integer>isChar('/').then(Parse.put(ExpressionVisitor.BINARY_OPERATOR_DIV)))
        ).then(Parse.consume());
    private void multOperation(Expression lhs, Cursor<Character> cursor, Matcher<Character, Expression> matcher) {
        this.<Integer>ws().then(this.multOperator).pipe(Parse.<Integer, Expression>reify((c1, m1) -> {
            int operator = c1.peek();

            ws.then(this.leafExpression).pipe1To(rhs -> {
                Expression operation = v -> v.visitBinary(operator, lhs, rhs);
                multOperation(operation, cursor, matcher);
            }).parseFrom(cursor, m1);
        }))
        .or(Parse.<Character, Expression>success(() -> {
            new String();
        }).then(Parse.put(lhs)))
        .parse(cursor, matcher);
    }

    private Parser<Character, Expression> leafExpression = intStream.or(intValue).or(ref(() -> this.id));

    private Parser<Character, Expression> expression = mulExpression;

    private <TOut> Parser<Character, TOut> idPattern(Function<String, TOut> reducer) {
        return Parse.reduceString(
            CharParse.<Character>isLetter().then(Parse.copy()).then(Parse.consume()).onceOrMore(),
            str -> reducer.apply(str)
        );
    }

    private Parser<Character, Expression> id = idPattern(str -> v -> v.visitId(str));

    private Parser<Character, Relation> assign =
        idPattern(str -> str).wrap((cursor, matcher) -> idProduction -> {
            String id = idProduction.cursor().peek();

            ws.then(CharParse.isChar('=')).then(Parse.consume()).then(ws).then(expression)
            .reduce1(value ->
                new Relation(id, value))
            .parse(cursor, matcher);
        });

    private Parser<Character, Relation> statement = assign;
    private Parser<Character, Relation> statements = statement.then(this.<Relation>ws().then(statement).multi());
    private Parser<Character, Relation> body = this.<Relation>ws().then(statements).then(this.<Relation>ws());

    @Override
    public Parser<Character, Relation> createParser() {
        return ref(() -> this.body).then(Parse.atEnd());
    }
}
