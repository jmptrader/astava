package astava.samples.drawnmap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Arrays;

public class MainView extends JFrame {
    private JComponent toolBoxView;
    private JComponent canvasView;
    private JComponent scriptView;

    public MainView() {
        setTitle("Draw'n'map");

        toolBoxView = createToolBoxView();
        canvasView = createCanvasView();
        scriptView = createScriptView();

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(toolBoxView, BorderLayout.NORTH);
        getContentPane().add(canvasView, BorderLayout.CENTER);
        getContentPane().add(scriptView, BorderLayout.SOUTH);
    }

    private ButtonGroup toolBoxButtonGroup;

    private JComponent createToolBoxView() {
        JToolBar toolBar = new JToolBar();

        toolBar.setFloatable(false);

        toolBoxButtonGroup = new ButtonGroup();

        java.util.List<Tool> tools = getTools();
        for(int i = 0; i < tools.size(); i++) {
            Tool tool = tools.get(i);

            JRadioButton b = new JRadioButton();
            b.setActionCommand("" + i);
            if(i == 0)
                b.setSelected(true);
            b.setText(tool.getText());
            toolBoxButtonGroup.add(b);
            toolBar.add(b);
        }

        return toolBar;
    }

    private Tool getSelectedTool() {
        int indexOfTool = Integer.parseInt(toolBoxButtonGroup.getSelection().getActionCommand());
        return getTools().get(indexOfTool);
    }

    private java.util.List<Tool> getTools() {
        return Arrays.asList(
            new LineTool(),
            new RectTool(),
            new NumberTool(),
            new MarkTool()
        );
    }

    private MouseAdapter canvasMouseAdapterProxy = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            canvasMouseAdapter.mouseClicked(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            canvasMouseAdapter.mousePressed(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            canvasMouseAdapter.mouseReleased(e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            canvasMouseAdapter.mouseEntered(e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            canvasMouseAdapter.mouseExited(e);
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            canvasMouseAdapter.mouseWheelMoved(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            canvasMouseAdapter.mouseDragged(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            canvasMouseAdapter.mouseMoved(e);
        }
    };
    private MouseAdapter canvasMouseAdapter;

    private JComponent createCanvasView() {
        JPanel view = new JPanel();

        view.setLayout(null);

        switchCanvasMousePending();

        view.addMouseListener(canvasMouseAdapterProxy);
        view.addMouseMotionListener(canvasMouseAdapterProxy);

        return view;
    }

    private void switchCanvasMousePending() {
        canvasMouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Tool tool = getSelectedTool();
                switchCanvasMouseAction(tool.startSession(canvasView, e.getX(), e.getY()));
            }
        };
    }

    private void switchCanvasMouseAction(ToolSession toolSession) {
        canvasMouseAdapter = new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
               toolSession.update(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                toolSession.end();
                switchCanvasMousePending();
            }
        };
    }


    private JComponent createScriptView() {
        JTextPane view = new JTextPane();

        view.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        return view;
    }
}