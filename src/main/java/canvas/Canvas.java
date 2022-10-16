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
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.rmi.RemoteException;

public class Canvas extends JComponent {

    private static final long serialVersionUID = 1L;
    private final String username;
    private final boolean isManager;
    private static final int canvasWidth = 700;
    private static final int canvasHeight = 450;
    private String drawType;
    private Color color;
    private Point start, end;
    private String text;
    private final IBoardMgr boardMgr;
    private Graphics2D g2;
    private BufferedImage frame;
    private BufferedImage prevFrame;


    public Canvas(IBoardMgr boardMgr, String username, boolean isManager) {
        this.boardMgr = boardMgr;
        this.username = username;
        this.isManager = isManager;
        this.color = Color.black;
        this.drawType = "free";
        this.text = "";

        setDoubleBuffered(false);
        // Mouse pressed => start position
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                start = event.getPoint();
                saveCanvas();
                try {
                    ICanvasMsg msg = new CanvasMsg("start", drawType, color, start, text, username);
                    boardMgr.broadcastMsg(msg);
                } catch (RemoteException e) {
                    JOptionPane.showMessageDialog(null, "Unable to draw, server is shut down!");
                }
            }
        });
        // Monitor motion of the mouse
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent event) {
                end = event.getPoint();
                Shape shape;
                if (g2 != null) {
                    // Generate different shapes according to types of drawings
                    switch (drawType) {
                        case "line":
                            renderFrame(prevFrame);
                            shape = drawLine(start, end);
                            break;
                        case "circle":
                            renderFrame(prevFrame);
                            shape = drawCircle(start, end);
                            break;
                        case "triangle":
                            renderFrame(prevFrame);
                            shape = drawTriangle(start, end);
                            break;
                        case "rectangle":
                            renderFrame(prevFrame);
                            shape = drawRectangle(start, end);
                            break;
                        case "free":
                            shape = drawLine(start, end);
                            start = end;
                            try {
                                ICanvasMsg msg = new CanvasMsg("drawing", drawType, color, end, text, username);
                                boardMgr.broadcastMsg(msg);
                            } catch (RemoteException e) {
                                JOptionPane.showMessageDialog(null, "Unable to connect to server!");
                            }
                            break;
                        case "text":
                            renderFrame(prevFrame);
                            g2.setFont(new Font("Calibri", Font.PLAIN, 20));
                            g2.drawString("Type your text here", end.x, end.y);
                            shape = drawText(start);
                            Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1, new float[]{3}, 0);
                            g2.setStroke(dashed);
                            break;
                        case "eraser":
                            shape = drawLine(start, end);
                            start = end;
                            g2.setPaint(Color.white);
                            g2.setStroke(new BasicStroke(10.0f));
                            try {
                                ICanvasMsg msg = new CanvasMsg("drawing", drawType, color, end, text, username);
                                boardMgr.broadcastMsg(msg);
                            } catch (RemoteException e) {
                                JOptionPane.showMessageDialog(null, "Unable to connect to server!");
                            }
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + drawType);
                    }
                    g2.draw(shape);
                    repaint();
                }
            }
        });
        // Mouse released => end position
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                end = event.getPoint();
                Shape shape = null;
                if (g2 != null) {
                    // Generate different shapes according to types of drawings
                    switch (drawType) {
                        case "line":
                        case "free":
                            shape = drawLine(start, end);
                            break;
                        case "circle":
                            shape = drawCircle(start, end);
                            break;
                        case "triangle":
                            shape = drawTriangle(start, end);
                            break;
                        case "rectangle":
                            shape = drawRectangle(start, end);
                            break;
                        case "text":
                            // Ask for text input
                            text = JOptionPane.showInputDialog("Type the text here");
                            if (text == null) text = "";
                            renderFrame(prevFrame);
                            g2.setFont(new Font("Calibri", Font.PLAIN, 20));
                            g2.drawString(text, end.x, end.y);
                            g2.setStroke(new BasicStroke(1.0f));
                            break;
                        case "eraser":
                            shape = drawLine(start, end);
                            g2.setPaint(color);
                            g2.setStroke(new BasicStroke(2.0f));
                            break;
                    }
                    // Draw on the canvas if it is not a text input
                    if (!drawType.equals("text")) {
                        try {
                            g2.draw(shape);
                        } catch (NullPointerException e) {
                            System.out.println("Drawing error!");
                        }
                    }
                    repaint();
                    // Broadcast changes to all clients
                    try {
                        ICanvasMsg msg = new CanvasMsg("end", drawType, color, end, text, username);
                        boardMgr.broadcastMsg(msg);
                    } catch (RemoteException e) {
                        JOptionPane.showMessageDialog(null, "Unable to connect to server!");
                    }
                }
            }
        });

    }


    public String getDrawType() {
        return drawType;
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

    // Render the canvas for a newly joined client
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (frame == null) {
            // Render a blank for the first joined client (manager)
            if (isManager) {
                frame = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
                g2 = (Graphics2D) frame.getGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(this.color);
                g2.setStroke(new BasicStroke(2.0f));
                cleanCanvas();
            } else {
                // Render the current canvas to the newly joined client
                try {
                    byte[] image = boardMgr.sendCurrentCanvas();
                    frame = ImageIO.read(new ByteArrayInputStream(image));
                    g2 = (Graphics2D) frame.getGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setPaint(this.color);
                    g2.setStroke(new BasicStroke(2.0f));
                } catch (Exception e) {
                    System.out.println("Render error");
                }
            }
        }
        g.drawImage(frame, 0, 0, null);
    }

    // Clean up the canvas
    public void cleanCanvas() {
        g2.setPaint(Color.white);
        g2.fillRect(0, 0, canvasWidth, canvasHeight);
        g2.setPaint(color);
        repaint();
    }

    // Save the canvas as an image
    public void saveCanvas() {
        ColorModel cm = frame.getColorModel();
        WritableRaster raster = frame.copyData(null);
        prevFrame = new BufferedImage(cm, raster, false, null);
    }

    // Get image of the current canvas
    public BufferedImage getCanvasImage() {
        saveCanvas();
        return prevFrame;
    }

/**************************************************Types of Drawings***************************************************/
    public void free() {
        this.drawType = "free";
    }

    public void line() {
        this.drawType = "line";
    }

    public void circle() {
        this.drawType = "circle";
    }

    public void triangle() {
        this.drawType = "triangle";
    }

    public void rectangle() {
        this.drawType = "rectangle";
    }

    public void text() {
        this.drawType = "text";
    }

    public void eraser() {
        this.drawType = "eraser";
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
        int minY = Math.min(start.y, end.y);
        int maxX = Math.max(start.x, end.x);
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

    public Shape drawText(Point start) {
        int x = start.x;
        int y = start.y;
        int width = 120;
        int height = 40;
        return new RoundRectangle2D.Double(x, y, width, height, 15, 15);
    }

/*********************************************The Sixteen Named Colors*************************************************/
    public void black() {
        color = Color.black;
        g2.setPaint(color);
    }

    public void white() {
        color = Color.white;
        g2.setPaint(color);
    }

    public void gray() {
        color = Color.gray;
        g2.setPaint(color);
    }

    public void silver() {
        color = new Color(75, 75, 75);
        g2.setPaint(color);
    }

    public void maroon() {
        color = new Color(50, 0, 0);
        g2.setPaint(color);
    }

    public void red() {
        color = Color.red;
        g2.setPaint(color);
    }

    public void purple() {
        color = new Color(128, 0, 128);
        g2.setPaint(color);
    }

    public void fuchsia() {
        color = new Color(255, 0, 255);
        g2.setPaint(color);
    }

    public void green() {
        color = new Color(0, 128, 0);
        g2.setPaint(color);
    }

    public void lime() {
        color = new Color(0, 255, 0);
        g2.setPaint(color);
    }

    public void olive() {
        color = new Color(128, 128, 0);
        g2.setPaint(color);
    }

    public void yellow() {
        color = Color.yellow;
        g2.setPaint(color);
    }

    public void navy() {
        color = new Color(0, 0, 50);
        g2.setPaint(color);
    }

    public void blue() {
        color = Color.blue;
        g2.setPaint(color);
    }

    public void teal() {
        color = new Color(0, 50, 50);
        g2.setPaint(color);
    }

    public void aqua() {
        color = new Color(0, 100, 100);
        g2.setPaint(color);
    }

}
