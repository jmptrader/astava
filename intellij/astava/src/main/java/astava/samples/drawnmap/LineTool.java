package astava.samples.drawnmap;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;

public class LineTool implements Tool {
    @Override
    public String getText() {
        return "Line";
    }

    private static class Line extends JComponent {
        private Line2D line;

        private Line(int x1, int y1) {
            line = new Line2D.Float(x1, y1, x1, y1);
        }

        public void setLine(int x1, int y1, int x2, int y2) {
            line.setLine(x1, y1, x2, y2);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2D = (Graphics2D)g;
            g2D.draw(line);
        }
    }

    @Override
    public ToolSession startSession(JComponent target, int x1, int y1) {
        Line line = new Line(x1, y1);

        line.setLocation(x1, y1);

        target.add(line);
        target.revalidate();
        target.repaint();

        return new ToolSession() {
            @Override
            public void update(int x2, int y2) {
                int left = Math.min(x1, x2);
                int right = Math.max(x1, x2);
                int top = Math.min(y1, y2);
                int bottom = Math.max(y1, y2);
                int xDelta = right - left;
                int yDelta = bottom - top;

                int xDir = x1 < x2
                    ? 0 // LeftRight
                    : 1 ; // RightLeft
                int yDir = y1 < y2
                    ? 0 // TopDown
                    : 1 ; // BottomUp

                line.setSize(xDelta + 1, yDelta + 1);
                line.setLocation(left, top);
                int lineX1 = xDir == 0 ? 0 : xDelta;
                int lineY1 = yDir == 0 ? 0 : yDelta;
                int lineX2 = xDir == 1 ? 0 : xDelta;
                int lineY2 = yDir == 1 ? 0 : yDelta;
                line.setLine(lineX1, lineY1, lineX2, lineY2);
                line.revalidate();
                line.repaint();
            }

            @Override
            public void end() {

            }
        };
    }
}