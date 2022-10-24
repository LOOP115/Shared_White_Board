/**
 * Define available colors
 */

package canvas;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Utils {

    // Paint modes
    public static final String line = "line";
    public static final String circle = "circle";
    public static final String triangle = "triangle";
    public static final String rectangle = "rectangle";
    public static final String text = "text";
    public static final String free = "free";
    public static final String eraser = "eraser";

    // Extra colors available for painting
    public static final Color silver = new Color(75, 75, 75);
    public static final Color maroon = new Color(50, 0, 0);
    public static final Color purple = new Color(128, 0, 128);
    public static final Color fuchsia = new Color(255, 0, 255);
    public static final Color green = new Color(0, 128, 0);
    public static final Color lime = new Color(0, 255, 0);
    public static final Color olive = new Color(128, 128, 0);
    public static final Color navy = new Color(0, 0, 50);
    public static final Color teal = new Color(0, 50, 50);
    public static final Color aqua = new Color(0, 100, 100);

    // Constants for canvas
    public static final BasicStroke defaultStroke = new BasicStroke(2f);
    public static final BasicStroke thickStroke = new BasicStroke(50f);
    public static final Font defaultFont = new Font("Calibri",Font.PLAIN, 30);
    public static final String paintStart = "paintStart";
    public static final String painting = "painting";
    public static final String paintEnd = "paintEnd";

    // Emphasize selections with borders
    public static final Color bgColor = new Color(238, 238, 238);
    public static final LineBorder border = new LineBorder(Color.BLACK, 2);
    public static final LineBorder antiBorder = new LineBorder(bgColor, 2);

    // UI settings
    public static final int windowWidth = 1000;
    public static final int windowHeight = 800;
    public static final int canvasWidth = 660;
    public static final int canvasHeight = 660;
    public static final int drawBtWidth = 30;
    public static final int drawBtHeight = 30;
    public static final int funcBtWidth = 20;
    public static final int funcBtHeight = 20;
    public static final int clientWindowWidth = 300;
    public static final int clientWindowHeight = 300;
    public static final int chatWindowWidth = 300;
    public static final int chatWindowHeight = 300;
    public static final int msgWindowWidth = 50;
    public static final int msgWindowHeight = 10;


    // Fill borders on selected button and move borders on the rest
    public static void selectButton(JButton buttonSelected, ArrayList<JButton> bts) {
        for (JButton bt: bts) {
            if (bt == buttonSelected) {
                bt.setBorder(Utils.border);
            } else {
                bt.setBorder(Utils.antiBorder);
            }
        }
    }

    // Resize the icon image
    public static ImageIcon resizeIcon(String path, int width, int height) {
        Path currentRelativePath = Paths.get("");
        String dir = currentRelativePath.toAbsolutePath().toString();
        ImageIcon icon = new ImageIcon(dir + "/icons/" + path);
        Image iconImg = icon.getImage();
        Image resizeImg = iconImg.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(resizeImg);
    }

}
