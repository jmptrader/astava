package astava.parse3;

public interface Cursor<T>  {
    public interface State {
        void restore();
    }

    //Position<T> position();
    //Cursor<T> interval(Position<T> start, Position<T> end);
    T peek();
    void consume();
    boolean atEnd();
    State state();

    //void setPosition(Position<T> position);
    /*default Stream<T> stream() {
        return new Stream<T>() {
        }
    }*/
}
