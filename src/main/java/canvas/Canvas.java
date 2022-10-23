/**
 * Class for user interface of the canvas.
 */

package canvas;

import server.IBoardMgr;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.rmi.RemoteException;

public class Canvas extends JComponent {

    private static final long serialVersionUID = 1L;
    private final boolean isManager;
    private String paintType = Utils.free;
    private Color color = Color.black;
    private Point start, end;
    private String text = "";
    private final IBoardMgr boardMgr;
    private Graphics2D g2;
    private BufferedImage frame;
    private BufferedImage savedFrame;

    public Canvas(IBoardMgr boardMgr, String username, boolean isManager) {
        this.boardMgr = boardMgr;
        this.isManager = isManager;

        // Mouse pressed => start position
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1) {
                    start = event.getPoint();
                    saveCanvas();
                    try {
                        ICanvasMsg msg = new CanvasMsg(Utils.paintStart, paintType, color, start, text, username);
                        boardMgr.broadcastMsg(msg);
                    } catch (RemoteException e) {
                        JOptionPane.showMessageDialog(null, "Unable to draw, server is shut down!");
                    }
                }
            }
        });

        // Monitor motion of the mouse
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    end = event.getPoint();
                    Shape shape = null;
                    if (g2 != null) {
                        // Generate different shapes according to types of drawings
                        switch (paintType) {
                            case Utils.line:
                                renderFrame(savedFrame);
                                shape = drawLine(start, end);
                                break;
                            case Utils.circle:
                                renderFrame(savedFrame);
                                shape = drawCircle(start, end);
                                break;
                            case Utils.triangle:
                                renderFrame(savedFrame);
                                shape = drawTriangle(start, end);
                                break;
                            case Utils.rectangle:
                                renderFrame(savedFrame);
                                shape = drawRectangle(start, end);
                                break;
                            case Utils.free:
                                shape = drawLine(start, end);
                                start = end;
                                try {
                                    ICanvasMsg msg = new CanvasMsg(Utils.painting, paintType, color, end, text, username);
                                    boardMgr.broadcastMsg(msg);
                                } catch (RemoteException e) {
                                    JOptionPane.showMessageDialog(null, "Unable to connect to server!");
                                }
                                break;
                            case Utils.text:
                                renderFrame(savedFrame);
                                g2.setFont(Utils.defaultFont);
                                g2.drawString("Text", end.x, end.y);
                                // shape = drawText(start);
                                break;
                            case Utils.eraser:
                                shape = drawLine(start, end);
                                start = end;
                                g2.setPaint(Color.white);
                                g2.setStroke(Utils.thickStroke);
                                try {
                                    ICanvasMsg msg = new CanvasMsg(Utils.painting, paintType, Color.white, end, text, username);
                                    boardMgr.broadcastMsg(msg);
                                } catch (RemoteException e) {
                                    JOptionPane.showMessageDialog(null, "Unable to connect to server!");
                                }
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + paintType);
                        }
                        if (!paintType.equals(Utils.text)) {
                            g2.draw(shape);
                        }
                        repaint();
                    }
                }
            }
        });

        // Mouse released => end position
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1) {
                    end = event.getPoint();
                    Shape shape = null;
                    if (g2 != null) {
                        // Generate different shapes according to types of drawings
                        switch (paintType) {
                            case Utils.line:
                            case Utils.free:
                            case Utils.eraser:
                                shape = drawLine(start, end);
                                break;
                            case Utils.circle:
                                shape = drawCircle(start, end);
                                break;
                            case Utils.triangle:
                                shape = drawTriangle(start, end);
                                break;
                            case Utils.rectangle:
                                shape = drawRectangle(start, end);
                                break;
                            case Utils.text:
                                // Ask for text input
                                text = JOptionPane.showInputDialog("Type your text here");
                                if (text == null) text = "";
                                renderFrame(savedFrame);
                                g2.setFont(Utils.defaultFont);
                                g2.drawString(text, end.x, end.y);
                                break;
                        }
                        // Broadcast changes to all clients
                        try {
                            ICanvasMsg msg;
                            if (paintType.equals(Utils.eraser)) {
                                msg = new CanvasMsg(Utils.paintEnd, paintType, Color.white, end, text, username);
                            } else {
                                msg = new CanvasMsg(Utils.paintEnd, paintType, color, end, text, username);
                            }
                            boardMgr.broadcastMsg(msg);
                        } catch (RemoteException e) {
                            JOptionPane.showMessageDialog(null, "Unable to connect to server!");
                        }
                        // Draw on the canvas if it is not a text input
                        if (!paintType.equals(Utils.text)) {
                            try {
                                g2.draw(shape);
                            } catch (NullPointerException e) {
                                System.out.println("Drawing error!");
                            }
                        }
                        repaint();
                        // Restore the original color and stroke
                        g2.setPaint(color);
                        g2.setStroke(Utils.defaultStroke);
                    }
                }
            }
        });

    }


    // Render the canvas for a newly joined client
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (frame == null) {
            // Render a blank for the first joined client (manager)
            if (isManager) {
                frame = new BufferedImage(Utils.canvasWidth, Utils.canvasHeight, BufferedImage.TYPE_INT_RGB);
                g2 = (Graphics2D) frame.getGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(this.color);
                g2.setStroke(Utils.defaultStroke);
                cleanCanvas();
            } else {
                // Render the current canvas to the newly joined client
                try {
                    byte[] image = boardMgr.sendCurrentCanvas();
                    frame = ImageIO.read(new ByteArrayInputStream(image));
                    g2 = (Graphics2D) frame.getGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setPaint(this.color);
                    g2.setStroke(Utils.defaultStroke);
                } catch (Exception e) {
                    System.out.println("Render error");
                }
            }
        }
        g.drawImage(frame, 0, 0, null);
    }


    public Color getColor() {
        return color;
    }

    public Graphics2D getG2() {
        return g2;
    }

    public BufferedImage getFrame() {
        return frame;
    }

    public void renderFrame(BufferedImage f) {
        g2.drawImage(f, 0, 0, null);
        repaint();
    }

    // Clean up the canvas
    public void cleanCanvas() {
        g2.setPaint(Color.white);
        g2.fillRect(0, 0, Utils.canvasWidth, Utils.canvasHeight);
        g2.setPaint(color);
        repaint();
    }

    // Save the canvas as an image
    public void saveCanvas() {
        ColorModel cm = frame.getColorModel();
        WritableRaster raster = frame.copyData(null);
        savedFrame = new BufferedImage(cm, raster, false, null);
    }

    // Get image of the current canvas
    public BufferedImage getCanvasImage() {
        saveCanvas();
        return savedFrame;
    }

/**************************************************Types of Drawings***************************************************/
    public void line() {
        this.paintType = Utils.line;
    }

    public void circle() {
        this.paintType = Utils.circle;
    }

    public void triangle() {
        this.paintType = Utils.triangle;
    }

    public void rectangle() {
        this.paintType = Utils.rectangle;
    }

    public void text() {
        this.paintType = Utils.text;
    }

    public void free() {
        this.paintType = Utils.free;
    }

    public void eraser() {
        this.paintType = Utils.eraser;
    }


    public Shape drawLine(Point start, Point end) {
        return new Line2D.Double(start.x, start.y, end.x, end.y);
    }

    public Shape drawCircle(Point start, Point end) {
        int width = Math.abs(start.x - end.x);
        int height = Math.abs(start.y - end.y);
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        return new Ellipse2D.Double(x, y, Math.max(width, height), Math.max(width, height));
    }

    public Shape drawTriangle(Point start, Point end) {
        int minX = Math.min(start.x, end.x);
        int maxX = Math.max(start.x, end.x);
        int minY = Math.min(start.y, end.y);
        int maxY = Math.max(start.y, end.y);
        int[] x = {minX, (minX + maxX)/2, maxX};
        int[] y = {maxY, minY, maxY};
        if (end.y < start.y) {
            y = new int[]{minY, maxY, minY};
        }
        return new Polygon(x, y, 3);
    }

    public Shape drawRectangle(Point start, Point end) {
        int width = Math.abs(start.x - end.x);
        int height = Math.abs(start.y - end.y);
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        return new Rectangle2D.Double(x, y, width, height);
    }

/*********************************************The Sixteen Named Colors*************************************************/
    public void black() {
        this.color = Color.black;
        this.g2.setPaint(this.color);
    }

    public void white() {
        this.color = Color.white;
        this.g2.setPaint(this.color);
    }

    public void gray() {
        this.color = Color.gray;
        this.g2.setPaint(this.color);
    }

    public void silver() {
        this.color = Utils.silver;
        this.g2.setPaint(this.color);
    }

    public void maroon() {
        this.color = Utils.maroon;
        this.g2.setPaint(this.color);
    }

    public void red() {
        this.color = Color.red;
        this.g2.setPaint(this.color);
    }

    public void purple() {
        this.color = Utils.purple;
        this.g2.setPaint(this.color);
    }

    public void fuchsia() {
        this.color = Utils.fuchsia;
        this.g2.setPaint(this.color);
    }

    public void green() {
        this.color = Utils.green;
        this.g2.setPaint(this.color);
    }

    public void lime() {
        this.color = Utils.lime;
        this.g2.setPaint(this.color);
    }

    public void olive() {
        this.color = Utils.olive;
        this.g2.setPaint(this.color);
    }

    public void yellow() {
        this.color = Color.yellow;
        this.g2.setPaint(this.color);
    }

    public void navy() {
        this.color = Utils.navy;
        this.g2.setPaint(this.color);
    }

    public void blue() {
        this.color = Color.blue;
        this.g2.setPaint(this.color);
    }

    public void teal() {
        this.color = Utils.teal;
        this.g2.setPaint(this.color);
    }

    public void aqua() {
        this.color = Utils.aqua;
        this.g2.setPaint(this.color);
    }

}
