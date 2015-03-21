package astava.parse3;

import java.util.function.BiFunction;

public interface Parser<TIn, TOut> {
    void parse(Cursor<TIn> cursor, Matcher<TIn, TOut> matcher);
    default Matcher<TIn, TOut> parseInit(Cursor<TIn> cursor, BiFunction<Parser<TIn, TOut>, Cursor<TIn>, Matcher<TIn, TOut>> matcherFunction) {
        Matcher<TIn, TOut> matcher = matcherFunction.apply(this, cursor);
        parse(cursor, matcher);
        return matcher;
    }

    default Parser<TIn, TOut> or(Parser<TIn, TOut> other) {
        return Parse.decision(this, other);
    }

    default Parser<TIn, TOut> then(Parser<TIn, TOut> next) {
        return Parse.sequence(this, next);
    }

    default <TOut2> Parser<TIn, TOut2> pipe(Parser<TOut, TOut2> next) {
        return Parse.pipe(this, next);
    }

    default <TOut2> Parser<TIn, TOut2> pipeOut(Parser<Cursor<TOut>, TOut2> next) {
        return Parse.pipeOut(this, next);
    }

    default Parser<TIn, TOut> multi() {
        return Parse.multi(this);
    }

    default Parser<TIn, TOut> onceOrMore() {
        return Parse.onceOrMore(this);
    }
}