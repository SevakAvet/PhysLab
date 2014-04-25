import java.awt.*;


public class SimLayout implements LayoutManager {
    private final int SPACER = 5;

    public SimLayout() {
    }

    public void addLayoutComponent(String name, Component c) {
    }

    public void removeLayoutComponent(Component c) {
    }

    public Dimension preferredLayoutSize(Container target) {
        return new Dimension(500, 500);
    }

    public Dimension minimumLayoutSize(Container target) {
        return new Dimension(100, 100);
    }

    private int countNumPanels(Container target) {
        int numPanels = 0;
        int n = target.getComponentCount();
        for (int i = 0; i < n; i++) {
            if (target.getComponent(i) instanceof SimPanel)
                numPanels++;
        }
        return numPanels;
    }

    private int grovelControls(Container target, int startComponent, boolean doMove, int startVertical, int[] lineHeights) {
        int n = target.getComponentCount();
        int canvasWidth = target.getSize().width;
        int verticalPosition = startVertical;
        int horizontalPosition = SPACER;
        int currentLineHeight = 0;
        int lineNum = 0;
        for (int i = startComponent; i < n; i++) {
            Component m = target.getComponent(i);

            if (m.isVisible()) {
                int w, h;
                Dimension d = m.getPreferredSize();
                h = d.height;
                w = d.width;

                if ((horizontalPosition + w > canvasWidth)) {
                    lineHeights[lineNum++] = currentLineHeight;
                    horizontalPosition = SPACER;
                    verticalPosition += currentLineHeight + SPACER;
                    currentLineHeight = 0;
                }

                if (doMove) {
                    m.setSize(w, h);

                    int x = horizontalPosition;
                    int y = verticalPosition + (lineHeights[lineNum] - m.getSize().height) / 2;
                    m.setLocation(x, y);
                }

                if (h > currentLineHeight) currentLineHeight = h;
                horizontalPosition += w + SPACER;
            }
        }

        lineHeights[lineNum] = currentLineHeight;
        return verticalPosition + currentLineHeight;
    }

    public void layoutContainer(Container target) {
        int canvasWidth = target.getSize().width;
        int numPanels = countNumPanels(target);
        int[] lineHeights = new int[100];

        int lastY = grovelControls(target, numPanels, false, 0, lineHeights);

        int canvasHeight = target.getSize().height - (lastY + SPACER);
        Component cnvs = target.getComponent(0);
        if (numPanels == 1) {
            cnvs.setLocation(0, 0);
            cnvs.setSize(canvasWidth, canvasHeight);
        } else if (numPanels >= 2) {
            cnvs.setLocation(canvasWidth / 2, 0);
            cnvs.setSize(canvasWidth / 2, canvasHeight);
            Component graph = target.getComponent(1);
            graph.setLocation(0, 0);
            graph.setSize(canvasWidth / 2, canvasHeight);
        }

        grovelControls(target, numPanels, true, canvasHeight + SPACER, lineHeights);
    }
}
