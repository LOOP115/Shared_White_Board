/**
 * Class of client using the white board.
 * Icon images of buttons are stored in a directory => icons.
 * Since Jar files only recognise absolute paths, modify #Line56 before build or run the project,
 * otherwise buttons will have errors.
 */

package client;

import canvas.Canvas;
import canvas.ICanvasMsg;
import server.IBoardMgr;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static javax.swing.GroupLayout.Alignment.*;

public class Client extends UnicastRemoteObject implements IClient {

    private static final long serialVersionUID = 1L;
    private String username;
    private boolean isManager = false;
    private boolean hasAccess;
    private Canvas canvas;
    private final IBoardMgr server;

    // Use ConcurrentHaspMap to sync drawings
    private final ConcurrentHashMap<String, Point> points = new ConcurrentHashMap<>();

    // Save canvas
    private String canvasName;
    private String canvasPath;

    // UI window
    private JFrame window;
    private static final int windowWidth = 1000;
    private static final int windowHeight = 800;
    // Absolute path of icons directory
    private static final String iconDirPath = "C:\\Users\\cjhm0\\Desktop\\WhiteBoard\\src\\icons\\";

    // Emphasize selections with borders
    private final Color bgColor = new Color(238, 238, 238);
    private final LineBorder border = new LineBorder(Color.BLACK, 2);
    private final LineBorder antiBorder = new LineBorder(bgColor, 2);

    // Color buttons
    private JButton blackBt, whiteBt, grayBt, silverBt, maroonBt, redBt, purpleBt, fuchsiaBt;
    private JButton greenBt, limeBt, oliveBt, yellowBt, navyBt, blueBt, tealBt, aquaBt;
    private final ArrayList<JButton> colorBts = new ArrayList<>();
    private final JButton colorUse = new JButton();

    // Draw buttons
    private JButton freeBt, lineBt, circleBt, triangleBt, rectangleBt, textBt, eraserBt;
    private final ArrayList<JButton> drawBts = new ArrayList<>();
    private static final int drawBtWidth = 30;
    private static final int drawBtHeight = 30;

    // Function buttons
    private JButton newBt, openBt, saveBt, saveAsBt;
    private final ArrayList<JButton> funcBts = new ArrayList<>();
    private static final int funcBtWidth = 20;
    private static final int funcBtHeight = 20;

    // Client list
    private final DefaultListModel<String> clientList = new DefaultListModel<>();

    // Chat window
    private final DefaultListModel<String> chatHistory = new DefaultListModel<>();
    private JTextField chatMsg;
    private JScrollPane chatWindow;

    public Client(IBoardMgr server, String username) throws RemoteException {
        this.server = server;
        this.username = username;
        this.hasAccess = true;
    }

    @Override
    public String getName() throws RemoteException {
        return this.username;
    }

    @Override
    public void setName(String name) throws RemoteException {
        this.username = name;
    }

    @Override
    public void setAsManager() throws RemoteException {
        this.isManager = true;
    }

    @Override
    public boolean needAccess(String username) throws RemoteException {
        return JOptionPane.showConfirmDialog(window,
                username + " wants to share your white board.", "New share request",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    @Override
    public boolean getAccess() {
        return this.hasAccess;
    }

    @Override
    public void setAccess(boolean access) throws RemoteException {
        this.hasAccess = access;
    }

    @Override
    public void syncClientList(Set<IClient> clients) throws RemoteException {
        this.clientList.removeAllElements();
        this.clientList.addElement("Online users");
        for (IClient c: clients) {
            this.clientList.addElement(c.getName());
        }
    }

    @Override
    public void syncCanvas(ICanvasMsg draw) throws RemoteException {
        // No need to update drawer's canvas
        if (draw.getUsername().equals(this.username)) {
            return;
        }
        Shape shape = null;
        if (draw.getDrawState().equals(Canvas.paintStart)) {
            this.points.put(draw.getUsername(), draw.getPoint());
            return;
        }
        // Draw from the start point
        Color orgColor = this.canvas.getColor();
        Point start = this.points.get(draw.getUsername());
        this.canvas.getG2().setPaint(draw.getColor());

        switch (draw.getDrawState()) {
            // Sync mouse motion when free-hand drawing or using eraser
            case Canvas.painting:
                if (draw.getDrawType().equals("eraser")) {
                    canvas.getG2().setStroke(Canvas.thickStroke);
                }
                shape = canvas.drawLine(start, draw.getPoint());
                points.put(draw.getUsername(), draw.getPoint());
                canvas.getG2().draw(shape);
                canvas.repaint();
                break;
            // Sync mouse release
            case Canvas.paintEnd:
                if (draw.getDrawType().equals("free") || draw.getDrawType().equals("line") || draw.getDrawType().equals("eraser")) {
                    shape = canvas.drawLine(start, draw.getPoint());
                } else if (draw.getDrawType().equals("circle")) {
                    shape = canvas.drawCircle(start, draw.getPoint());
                } else if (draw.getDrawType().equals("triangle")) {
                    shape = canvas.drawTriangle(start, draw.getPoint());
                } else if (draw.getDrawType().equals("rectangle")) {
                    shape = canvas.drawRectangle(start, draw.getPoint());
                } else if (draw.getDrawType().equals("text")) {
                    canvas.getG2().setFont(Canvas.defaultFont);
                    canvas.getG2().drawString(draw.getText(), draw.getPoint().x, draw.getPoint().y);
                }
                // Draw on the canvas if it is not a text input
                if (!draw.getDrawType().equals("text")) {
                    try {
                        canvas.getG2().draw(shape);
                    } catch (NullPointerException e) {
                        System.out.println("Drawing error!");
                    }
                }
                canvas.repaint();
                points.remove(draw.getUsername());
                // Restore the original color and stroke
                canvas.getG2().setPaint(orgColor);
                canvas.getG2().setStroke(Canvas.defaultStroke);
                break;
        }
    }

    @Override
    public void cleanCanvas() throws RemoteException {
        this.canvas.cleanCanvas();
    }

    @Override
    public byte[] getCurrentCanvas() throws IOException {
        ByteArrayOutputStream image = new ByteArrayOutputStream();
        ImageIO.write(this.canvas.getCanvasImage(), "png", image);
        return image.toByteArray();
    }

    @Override
    public void overrideCanvas(byte[] canvas) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(canvas));
        this.canvas.renderFrame(image);
    }

    @Override
    public void forceQuit() {
        // End the program when the client is not approved to join in
        if(!this.hasAccess) {
            Thread t = new Thread(() -> System.exit(0));
            t.start();
            return;
        }
        // Manager end the session or the client is kicked out
        Thread t = new Thread(() -> {
            JOptionPane.showMessageDialog(window, "Manager has end your session",
                    "Warning", JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        });
        t.start();
    }

    @Override
    public void syncChat(String msg) throws RemoteException {
        this.chatHistory.addElement(msg);
    }

    @Override
    public DefaultListModel<String> getChatHistory() {
        return this.chatHistory;
    }

    @Override
    public void syncChatHistory(DefaultListModel<String> history) throws RemoteException {
        if (isManager) {
            this.chatHistory.addElement("Chat history");
        }
        for(Object msg: history.toArray()) {
            this.chatHistory.addElement((String) msg);
        }
    }


    // Client manager has access to open, save and saveAs
    public void mgrOpen() throws IOException {
        FileDialog dialog = new FileDialog(this.window, "Open a canvas", FileDialog.LOAD);
        dialog.setVisible(true);
        if (dialog.getFile() != null) {
            this.canvasName = dialog.getFile();
            this.canvasPath = dialog.getDirectory();
            BufferedImage image = ImageIO.read(new File(canvasPath + canvasName));
            canvas.renderFrame(image);
            ByteArrayOutputStream imageArray = new ByteArrayOutputStream();
            ImageIO.write(image, "png", imageArray);
            server.sendExistCanvas(imageArray.toByteArray());
        }
    }

    private void mgrSave() throws IOException{
        if(this.canvasName != null) {
            ImageIO.write(canvas.getFrame(), "png", new File(canvasPath + canvasName));
        }
        else {
            JOptionPane.showMessageDialog(null, "Please save it as a file first.",
                    "Reminder", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void mgrSaveAs() throws IOException {
        FileDialog dialog = new FileDialog(window, "Save canvas", FileDialog.SAVE);
        dialog.setVisible(true);
        if (dialog.getFile() != null) {
            this.canvasName = dialog.getFile() + ".png";
            this.canvasPath = dialog.getDirectory();
            ImageIO.write(canvas.getFrame(), "png", new File(canvasPath + canvasName));
        }
    }


    // Fill borders on selected button and move borders on the rest
    public void selectButton(JButton buttonSelected, ArrayList<JButton> bts) {
        for (JButton bt: bts) {
            if (bt == buttonSelected) {
                bt.setBorder(this.border);
            } else {
                bt.setBorder(this.antiBorder);
            }
        }
    }

    // Resize the icon image
    public ImageIcon resizeIcon(String path, int width, int height) {
        // ImageIcon icon = new ImageIcon(String.valueOf(Paths.get(path).toAbsolutePath()));
        ImageIcon icon = new ImageIcon(iconDirPath + path);
        Image iconImg = icon.getImage();
        Image resizeImg = iconImg.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(resizeImg);
    }


    // Monitor client's mouse actions and make corresponding changes on UI
    ActionListener actionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent event) {
            // Select a color
            if (event.getSource() == blackBt) {
                canvas.black();
                colorUse.setBackground(canvas.getColor());
            } else if (event.getSource() == whiteBt) {
                canvas.white();
                colorUse.setBackground(canvas.getColor());
            } else if (event.getSource() == grayBt) {
                canvas.gray();
                colorUse.setBackground(canvas.getColor());
            } else if (event.getSource() == silverBt) {
                canvas.silver();
                colorUse.setBackground(canvas.getColor());
            } else if (event.getSource() == maroonBt) {
                canvas.maroon();
                colorUse.setBackground(canvas.getColor());
            } else if (event.getSource() == redBt) {
                canvas.red();
                colorUse.setBackground(canvas.getColor());
            } else if (event.getSource() == purpleBt) {
                canvas.purple();
                colorUse.setBackground(canvas.getColor());
            } else if (event.getSource() == fuchsiaBt) {
                canvas.fuchsia();
                colorUse.setBackground(canvas.getColor());
            } else if (event.getSource() == greenBt) {
                canvas.green();
                colorUse.setBackground(canvas.getColor());
            } else if (event.getSource() == limeBt) {
                canvas.lime();
                colorUse.setBackground(canvas.getColor());
            } else if (event.getSource() == oliveBt) {
                canvas.olive();
                colorUse.setBackground(canvas.getColor());
            } else if (event.getSource() == yellowBt) {
                canvas.yellow();
                colorUse.setBackground(canvas.getColor());
            } else if (event.getSource() == navyBt) {
                canvas.navy();
                colorUse.setBackground(canvas.getColor());
            } else if (event.getSource() == blueBt) {
                canvas.blue();
                colorUse.setBackground(canvas.getColor());
            } else if (event.getSource() == tealBt) {
                canvas.teal();
                colorUse.setBackground(canvas.getColor());
            } else if (event.getSource() == aquaBt) {
                canvas.aqua();
                colorUse.setBackground(canvas.getColor());
            }
            // Select a draw type
            else if (event.getSource() == freeBt) {
                canvas.free();
                selectButton(freeBt, drawBts);
            } else if (event.getSource() == lineBt) {
                canvas.line();
                selectButton(lineBt, drawBts);
            } else if (event.getSource() == circleBt) {
                canvas.circle();
                selectButton(circleBt, drawBts);
            } else if (event.getSource() == triangleBt) {
                canvas.triangle();
                selectButton(triangleBt, drawBts);
            } else if (event.getSource() == rectangleBt) {
                canvas.rectangle();
                selectButton(rectangleBt, drawBts);
            } else if (event.getSource() == textBt) {
                canvas.text();
                selectButton(textBt, drawBts);
            } else if (event.getSource() == eraserBt) {
                canvas.eraser();
                selectButton(eraserBt, drawBts);
            }
            // Select a function button
            else if (event.getSource() == newBt) {
                if (isManager) {
                    try {
                        if (JOptionPane.showConfirmDialog(window,
                                "Are you sure you want to create a new canvas?\nUnsaved changes will be discarded!",
                                "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_NO_OPTION) {
                            server.cleanCanvas();
                        }
                    } catch (RemoteException e) {
                        System.out.println("Error with creating a new canvas!");
                    }
                }
            } else if (event.getSource() == openBt) {
                if (isManager) {
                    try {
                        if (JOptionPane.showConfirmDialog(window,
                                "Are you sure you want to open another canvas?\nUnsaved changes will be discarded!",
                                "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_NO_OPTION) {
                            mgrOpen();
                        }
                    } catch (IOException e) {
                        System.out.println("Error with opening a canvas!");
                    }
                }
            } else if (event.getSource() == saveBt) {
                if (isManager) {
                    try {
                        mgrSave();
                    } catch (IOException e) {
                        System.out.println("Error with saving the canvas!");
                    }
                }
            } else if (event.getSource() == saveAsBt) {
                if (isManager) {
                    try {
                        mgrSaveAs();
                    } catch (IOException e) {
                        System.out.println("Error with saving the canvas as a file!");
                    }
                }
            }
        }
    };


    // Initialise and render the UI
    @Override
    public void renderUI(IBoardMgr server) throws RemoteException {
        this.window = new JFrame("White Board");
        Container container = this.window.getContentPane();
        canvas = new Canvas(server, username, isManager);

        // Configure color buttons
        blackBt = new JButton();
        blackBt.setBackground(Color.black);
        this.colorBts.add(blackBt);

        whiteBt = new JButton();
        whiteBt.setBackground(Color.white);
        this.colorBts.add(whiteBt);

        grayBt = new JButton();
        grayBt.setBackground(Color.gray);
        this.colorBts.add(grayBt);

        silverBt = new JButton();
        silverBt.setBackground(new Color(75, 75, 75));
        this.colorBts.add(silverBt);

        maroonBt = new JButton();
        maroonBt.setBackground(new Color(50, 0, 0));
        this.colorBts.add(maroonBt);

        redBt = new JButton();
        redBt.setBackground(Color.red);
        this.colorBts.add(redBt);

        purpleBt = new JButton();
        purpleBt.setBackground(new Color(128, 0, 128));
        this.colorBts.add(purpleBt);

        fuchsiaBt = new JButton();
        fuchsiaBt.setBackground(new Color(255, 0, 255));
        this.colorBts.add(fuchsiaBt);

        greenBt = new JButton();
        greenBt.setBackground(new Color(0, 128, 0));
        this.colorBts.add(greenBt);

        limeBt = new JButton();
        limeBt.setBackground(new Color(0, 255, 0));
        this.colorBts.add(limeBt);

        oliveBt = new JButton();
        oliveBt.setBackground(new Color(128, 128, 0));
        this.colorBts.add(oliveBt);

        yellowBt = new JButton();
        yellowBt.setBackground(Color.yellow);
        this.colorBts.add(yellowBt);

        navyBt = new JButton();
        navyBt.setBackground(new Color(0, 0, 50));
        this.colorBts.add(navyBt);

        blueBt = new JButton();
        blueBt.setBackground(Color.blue);
        this.colorBts.add(blueBt);

        tealBt = new JButton();
        tealBt.setBackground(new Color(0, 50, 50));
        this.colorBts.add(tealBt);

        aquaBt = new JButton();
        aquaBt.setBackground(new Color(0, 100, 100));
        this.colorBts.add(aquaBt);

        for (JButton bt: colorBts) {
            bt.setBorderPainted(false);
            bt.setOpaque(true);
            bt.addActionListener(actionListener);
        }
        colorUse.setBackground(Color.black);


        // Configure drawing buttons
        ImageIcon icon;
        icon = resizeIcon("free.png", drawBtWidth, drawBtHeight);
        freeBt = new JButton(icon);
        freeBt.setToolTipText("Free-hand");
        this.drawBts.add(freeBt);

        icon = resizeIcon("line.png", drawBtWidth, drawBtHeight);
        lineBt = new JButton(icon);
        lineBt.setToolTipText("Line");
        this.drawBts.add(lineBt);

        icon = resizeIcon("circle.png", drawBtWidth, drawBtHeight);
        circleBt = new JButton(icon);
        circleBt.setToolTipText("Circle");
        this.drawBts.add(circleBt);

        icon = resizeIcon("triangle.png", drawBtWidth, drawBtHeight);
        triangleBt = new JButton(icon);
        triangleBt.setToolTipText("Triangle");
        this.drawBts.add(triangleBt);

        icon = resizeIcon("rectangle.png", drawBtWidth, drawBtHeight);
        rectangleBt = new JButton(icon);
        rectangleBt.setToolTipText("Rectangle");
        this.drawBts.add(rectangleBt);

        icon = resizeIcon("text.png", drawBtWidth, drawBtHeight);
        textBt = new JButton(icon);
        textBt.setToolTipText("Text");
        this.drawBts.add(textBt);

        icon = resizeIcon("eraser.png", drawBtWidth, drawBtHeight);
        eraserBt = new JButton(icon);
        eraserBt.setToolTipText("Eraser");
        this.drawBts.add(eraserBt);

        for (JButton bt: drawBts) {
            bt.setBorder(antiBorder);
            bt.addActionListener(actionListener);
        }


        // Configure function buttons for client manager
        icon = resizeIcon("new.png", funcBtWidth, funcBtHeight);
        newBt = new JButton(icon);
        newBt.setToolTipText("New canvas");
        this.funcBts.add(newBt);

        icon = resizeIcon("open.png", funcBtWidth, funcBtHeight);
        openBt = new JButton(icon);
        openBt.setToolTipText("Open a canvas");
        this.funcBts.add(openBt);

        icon = resizeIcon("save.png", funcBtWidth, funcBtHeight);
        saveBt = new JButton(icon);
        saveBt.setToolTipText("Save the canvas");
        this.funcBts.add(saveBt);

        icon = resizeIcon("saveAs.png", funcBtWidth, funcBtHeight);
        saveAsBt = new JButton(icon);
        saveAsBt.setToolTipText("Save as a file");
        this.funcBts.add(saveAsBt);

        for (JButton bt: funcBts) {
            bt.addActionListener(actionListener);
        }


        // Show all online users
        JList<String> clientJList = new JList<>(this.clientList);
        JScrollPane clientWindow = new JScrollPane(clientJList);
        clientWindow.setMinimumSize(new Dimension(50, 300));
        clientWindow.setBorder(border);

        // Manager can double-click on usernames to kick out users
        if (isManager) {
            clientJList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    @SuppressWarnings("unchecked")
                    JList<String> list = (JList<String>)event.getSource();
                    if (event.getClickCount() == 2) {
                        int index = list.locationToIndex(event.getPoint());
                        String kickName = list.getModel().getElementAt(index);
                        try {
                            if(!getName().equals(kickName)) {
                                if(JOptionPane.showConfirmDialog(window,
                                        "Are you sure you want to kick " + kickName + " out?",
                                        "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                                    try {
                                        server.kickClient(kickName);
                                        server.syncClientList();
                                    } catch (IOException e) {
                                        System.err.println("Unable to kick out " + kickName + "!");
                                    }
                                }
                            }
                        } catch (HeadlessException e) {
                            System.err.println("Headless error.");
                        } catch (RemoteException e) {
                            System.err.println("Remote error");
                        }
                    }
                }
            });
        }

        // All clients are forced to quit when the manager leaves
        window.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (isManager) {
                    try {
                        if (JOptionPane.showConfirmDialog(window,
                                "Are you sure you want to end the session?\nAll participants will be removed.",
                                "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                            server.removeAllClients();
                            System.exit(0);
                        }
                    } catch (IOException e) {
                        System.err.println("IO error");
                        System.exit(0);
                    }
                } else {
                    try {
                        if (JOptionPane.showConfirmDialog(window,
                                "Are you sure you want to leave the session?", "Warning",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                            server.quitClient(username);
                            server.syncClientList();
                            System.exit(0);
                        }
                    } catch (RemoteException e) {
                        JOptionPane.showMessageDialog(null, "Unable to connect to the server!");
                        System.exit(0);
                    }
                }
            }
        });


        // Configure chat window
        JList<String> chat = new JList<>(chatHistory);
        // Display chat history
        chatWindow = new JScrollPane(chat);
        chatWindow.setMinimumSize(new Dimension(50, 300));
        chatWindow.setBorder(border);
        // Type chat message here
        chatMsg = new JTextField();
        chatMsg.setMinimumSize(new Dimension(50, 10));
        chatMsg.setBorder(border);
        // Button to send message
        icon = resizeIcon("send.png", drawBtWidth, drawBtHeight);
        JButton sendBt = new JButton(icon);
        sendBt.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if(chatMsg.getText().equals("")) {
                    JOptionPane.showMessageDialog(null, "Message cannot be empty.");
                } else {
                    try {
                        server.broadcastChat(username + ": "+ chatMsg.getText());
                        // Show the latest message
                        SwingUtilities.invokeLater(() -> {
                            JScrollBar vertical = chatWindow.getVerticalScrollBar();
                            vertical.setValue(vertical.getMaximum());
                        });
                    } catch (RemoteException e) {
                        JOptionPane.showMessageDialog(null, "Server is down, failed to send the message!");
                    }
                    chatMsg.setText("");
                }
            }
        });


        // UI design
        GroupLayout layout = new GroupLayout(container);
        container.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        canvas.setBorder(border);

        // Horizontal layout
        layout.setHorizontalGroup(layout.createSequentialGroup()
            // Left
            .addGroup(layout.createParallelGroup(LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(newBt)
                    .addComponent(openBt)
                    .addComponent(saveBt)
                    .addComponent(saveAsBt))
                .addComponent(clientWindow)
                .addComponent(chatWindow)
                .addGroup(layout.createParallelGroup(CENTER)
                    .addComponent(chatMsg)
                    .addComponent(sendBt)))
            // Right
            .addGroup(layout.createParallelGroup(CENTER)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(blackBt)
                    .addComponent(grayBt)
                    .addComponent(maroonBt)
                    .addComponent(purpleBt)
                    .addComponent(greenBt)
                    .addComponent(oliveBt)
                    .addComponent(navyBt)
                    .addComponent(tealBt))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(whiteBt)
                    .addComponent(silverBt)
                    .addComponent(redBt)
                    .addComponent(fuchsiaBt)
                    .addComponent(limeBt)
                    .addComponent(yellowBt)
                    .addComponent(blueBt)
                    .addComponent(aquaBt))
                .addComponent(canvas)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(freeBt)
                    .addComponent(lineBt)
                    .addComponent(circleBt)
                    .addComponent(triangleBt)
                    .addComponent(rectangleBt)
                    .addComponent(textBt)
                    .addComponent(eraserBt)
                    .addComponent(colorUse))));


        // Vertical layout
        layout.setVerticalGroup(layout.createSequentialGroup()
            // Top
            .addGroup(layout.createParallelGroup(BASELINE)
                .addComponent(newBt)
                .addComponent(openBt)
                .addComponent(saveBt)
                .addComponent(saveAsBt)
                .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(BASELINE)
                        .addComponent(blackBt)
                        .addComponent(grayBt)
                        .addComponent(maroonBt)
                        .addComponent(purpleBt)
                        .addComponent(greenBt)
                        .addComponent(oliveBt)
                        .addComponent(navyBt)
                        .addComponent(tealBt))
                    .addGroup(layout.createParallelGroup(BASELINE)
                        .addComponent(whiteBt)
                        .addComponent(silverBt)
                        .addComponent(redBt)
                        .addComponent(fuchsiaBt)
                        .addComponent(limeBt)
                        .addComponent(yellowBt)
                        .addComponent(blueBt)
                        .addComponent(aquaBt))))
                .addGroup(layout.createParallelGroup(TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(clientWindow)
                        .addComponent(chatWindow)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(chatMsg)
                            .addComponent(sendBt)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(canvas)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(BASELINE)
                                .addComponent(freeBt)
                                .addComponent(lineBt)
                                .addComponent(circleBt)
                                .addComponent(triangleBt)
                                .addComponent(rectangleBt)
                                .addComponent(textBt)
                                .addComponent(eraserBt)
                                .addComponent(colorUse))))));

        // Only manager has access to functional buttons
        if (!isManager) {
            newBt.setVisible(false);
            openBt.setVisible(false);
            saveBt.setVisible(false);
            saveAsBt.setVisible(false);
        }

        // Configure buttons' size
        layout.linkSize(SwingConstants.HORIZONTAL, newBt, openBt, saveBt, saveAsBt);
        layout.linkSize(SwingConstants.HORIZONTAL, freeBt, lineBt, circleBt, triangleBt, rectangleBt, textBt, eraserBt, colorUse, sendBt);
        layout.linkSize(SwingConstants.VERTICAL, freeBt, lineBt, circleBt, triangleBt, rectangleBt, textBt, eraserBt, colorUse, sendBt);

        // Configure the UI window
        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.setMinimumSize(new Dimension(windowWidth, windowHeight));
    }

}
