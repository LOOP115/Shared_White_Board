/**
 * Class of client using the white board.
 * Icon images of buttons are stored in a directory => icons.
 * Since Jar files only recognise absolute paths, modify #Line56 before build or run the project,
 * otherwise buttons will have errors.
 */

package client;

import canvas.Canvas;
import canvas.ICanvasMsg;
import canvas.Utils;
import server.IBoardMgr;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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
    private final JFrame window = new JFrame("White Board");

    // Color buttons
    private JButton blackBt, whiteBt, grayBt, silverBt, maroonBt, redBt, purpleBt, fuchsiaBt;
    private JButton greenBt, limeBt, oliveBt, yellowBt, navyBt, blueBt, tealBt, aquaBt;
    private final ArrayList<JButton> colorBts = new ArrayList<>();
    private final JButton colorUse = new JButton();

    // Draw buttons
    private JButton freeBt, lineBt, circleBt, triangleBt, rectangleBt, textBt, eraserBt;
    private final ArrayList<JButton> drawBts = new ArrayList<>();

    // Function buttons
    private JButton newBt, openBt, saveBt, saveAsBt;
    private final ArrayList<JButton> funcBts = new ArrayList<>();

    // Client list
    private final DefaultListModel<String> clientList = new DefaultListModel<>();
    private final JList<String> clientJList = new JList<>(this.clientList);
    private final JScrollPane clientWindow = new JScrollPane(clientJList);

    // Chat window
    private final DefaultListModel<String> chatHistory = new DefaultListModel<>();
    private JTextField chatMsg;
    private JScrollPane chatWindow;
    private JButton sendBt;

    public Client(IBoardMgr server, String username) throws RemoteException {
        this.server = server;
        this.username = username;
        this.hasAccess = true;
    }

    @Override
    public String getUsername() throws RemoteException {
        return this.username;
    }

    @Override
    public void setUsername(String name) throws RemoteException {
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
            this.clientList.addElement(c.getUsername());
        }
    }

    @Override
    public void syncCanvas(ICanvasMsg draw) throws RemoteException {
        // No need to update drawer's canvas
        if (draw.getUsername().equals(this.username)) {
            return;
        }
        Shape shape = null;
        if (draw.getPaintState().equals(Utils.paintStart)) {
            this.points.put(draw.getUsername(), draw.getPoint());
            return;
        }
        // Draw from the start point
        Color orgColor = this.canvas.getColor();
        Point start = this.points.get(draw.getUsername());
        this.canvas.getG2().setPaint(draw.getColor());

        switch (draw.getPaintState()) {
            // Sync mouse motion when free-hand drawing or using eraser
            case Utils.painting:
                if (draw.getPaintType().equals(Utils.eraser)) {
                    canvas.getG2().setStroke(Utils.thickStroke);
                }
                shape = canvas.drawLine(start, draw.getPoint());
                points.put(draw.getUsername(), draw.getPoint());
                canvas.getG2().draw(shape);
                canvas.repaint();
                break;
            // Sync mouse release
            case Utils.paintEnd:
                if (draw.getPaintType().equals(Utils.free) || draw.getPaintType().equals(Utils.line) || draw.getPaintType().equals(Utils.eraser)) {
                    shape = canvas.drawLine(start, draw.getPoint());
                } else if (draw.getPaintType().equals(Utils.circle)) {
                    shape = canvas.drawCircle(start, draw.getPoint());
                } else if (draw.getPaintType().equals(Utils.triangle)) {
                    shape = canvas.drawTriangle(start, draw.getPoint());
                } else if (draw.getPaintType().equals(Utils.rectangle)) {
                    shape = canvas.drawRectangle(start, draw.getPoint());
                } else if (draw.getPaintType().equals(Utils.text)) {
                    canvas.getG2().setFont(Utils.defaultFont);
                    canvas.getG2().drawString(draw.getText(), draw.getPoint().x, draw.getPoint().y);
                }
                // Draw on the canvas if it is not a text input
                if (!draw.getPaintType().equals(Utils.text)) {
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
                canvas.getG2().setStroke(Utils.defaultStroke);
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

    @Override
    public void configUI() throws RemoteException {
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
        silverBt.setBackground(Utils.silver);
        this.colorBts.add(silverBt);

        maroonBt = new JButton();
        maroonBt.setBackground(Utils.maroon);
        this.colorBts.add(maroonBt);

        redBt = new JButton();
        redBt.setBackground(Color.red);
        this.colorBts.add(redBt);

        purpleBt = new JButton();
        purpleBt.setBackground(Utils.purple);
        this.colorBts.add(purpleBt);

        fuchsiaBt = new JButton();
        fuchsiaBt.setBackground(Utils.fuchsia);
        this.colorBts.add(fuchsiaBt);

        greenBt = new JButton();
        greenBt.setBackground(Utils.green);
        this.colorBts.add(greenBt);

        limeBt = new JButton();
        limeBt.setBackground(Utils.lime);
        this.colorBts.add(limeBt);

        oliveBt = new JButton();
        oliveBt.setBackground(Utils.olive);
        this.colorBts.add(oliveBt);

        yellowBt = new JButton();
        yellowBt.setBackground(Color.yellow);
        this.colorBts.add(yellowBt);

        navyBt = new JButton();
        navyBt.setBackground(Utils.navy);
        this.colorBts.add(navyBt);

        blueBt = new JButton();
        blueBt.setBackground(Color.blue);
        this.colorBts.add(blueBt);

        tealBt = new JButton();
        tealBt.setBackground(Utils.teal);
        this.colorBts.add(tealBt);

        aquaBt = new JButton();
        aquaBt.setBackground(Utils.aqua);
        this.colorBts.add(aquaBt);

        for (JButton bt: colorBts) {
            bt.setBorderPainted(false);
            bt.setOpaque(true);
            bt.addActionListener(colorListener);
        }
        colorUse.setBackground(Color.black);

        // Configure drawing buttons
        ImageIcon icon;
        icon = Utils.resizeIcon("line.png", Utils.drawBtWidth, Utils.drawBtHeight);
        lineBt = new JButton(icon);
        lineBt.setToolTipText("Line");
        this.drawBts.add(lineBt);

        icon = Utils.resizeIcon("circle.png", Utils.drawBtWidth, Utils.drawBtHeight);
        circleBt = new JButton(icon);
        circleBt.setToolTipText("Circle");
        this.drawBts.add(circleBt);

        icon = Utils.resizeIcon("triangle.png", Utils.drawBtWidth, Utils.drawBtHeight);
        triangleBt = new JButton(icon);
        triangleBt.setToolTipText("Triangle");
        this.drawBts.add(triangleBt);

        icon = Utils.resizeIcon("rectangle.png", Utils.drawBtWidth, Utils.drawBtHeight);
        rectangleBt = new JButton(icon);
        rectangleBt.setToolTipText("Rectangle");
        this.drawBts.add(rectangleBt);

        icon = Utils.resizeIcon("text.png", Utils.drawBtWidth, Utils.drawBtHeight);
        textBt = new JButton(icon);
        textBt.setToolTipText("Text");
        this.drawBts.add(textBt);

        icon = Utils.resizeIcon("free.png", Utils.drawBtWidth, Utils.drawBtHeight);
        freeBt = new JButton(icon);
        freeBt.setToolTipText("Free-hand");
        this.drawBts.add(freeBt);

        icon = Utils.resizeIcon("eraser.png", Utils.drawBtWidth, Utils.drawBtHeight);
        eraserBt = new JButton(icon);
        eraserBt.setToolTipText("Eraser");
        this.drawBts.add(eraserBt);

        for (JButton bt: drawBts) {
            bt.addActionListener(paintListener);
        }
        Utils.selectButton(freeBt, drawBts);

        // Configure function buttons for client manager
        icon = Utils.resizeIcon("new.png", Utils.funcBtWidth, Utils.funcBtHeight);
        newBt = new JButton(icon);
        newBt.setToolTipText("New canvas");
        this.funcBts.add(newBt);

        icon = Utils.resizeIcon("open.png", Utils.funcBtWidth, Utils.funcBtHeight);
        openBt = new JButton(icon);
        openBt.setToolTipText("Open a canvas");
        this.funcBts.add(openBt);

        icon = Utils.resizeIcon("save.png", Utils.funcBtWidth, Utils.funcBtHeight);
        saveBt = new JButton(icon);
        saveBt.setToolTipText("Save the canvas");
        this.funcBts.add(saveBt);

        icon = Utils.resizeIcon("saveAs.png", Utils.funcBtWidth, Utils.funcBtHeight);
        saveAsBt = new JButton(icon);
        saveAsBt.setToolTipText("Save as a file");
        this.funcBts.add(saveAsBt);

        for (JButton bt: funcBts) {
            bt.addActionListener(funcListener);
        }

        // Show all online users
        clientWindow.setMinimumSize(new Dimension(Utils.clientWindowWidth, Utils.clientWindowHeight));
        clientWindow.setBorder(Utils.border);
        // Manager can double-click on usernames to kick out users
        if (isManager) clientJList.addMouseListener(kickListener);
        // All clients are forced to quit when the manager leaves
        window.addWindowListener(quitListener);

        // Configure chat window
        JList<String> chat = new JList<>(chatHistory);
        // Display chat history
        chatWindow = new JScrollPane(chat);
        chatWindow.setMinimumSize(new Dimension(Utils.chatWindowWidth, Utils.chatWindowHeight));
        chatWindow.setBorder(Utils.border);
        // Type chat message here
        chatMsg = new JTextField();
        chatMsg.setMinimumSize(new Dimension(Utils.msgWindowWidth, Utils.msgWindowHeight));
        chatMsg.setBorder(Utils.border);
        // Button to send message
        icon = Utils.resizeIcon("send.png", Utils.drawBtWidth, Utils.drawBtHeight);
        sendBt = new JButton(icon);
        sendBt.addMouseListener(sendChatListener);
    }

    @Override
    public void renderUI() throws RemoteException {
        // Configure buttons and windows
        configUI();
        // UI settings
        Container container = this.window.getContentPane();
        GroupLayout layout = new GroupLayout(container);
        container.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        canvas.setBorder(Utils.border);

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
                    .addComponent(lineBt)
                    .addComponent(circleBt)
                    .addComponent(triangleBt)
                    .addComponent(rectangleBt)
                    .addComponent(textBt)
                    .addComponent(freeBt)
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
                                .addComponent(lineBt)
                                .addComponent(circleBt)
                                .addComponent(triangleBt)
                                .addComponent(rectangleBt)
                                .addComponent(textBt)
                                .addComponent(freeBt)
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
        window.setMinimumSize(new Dimension(Utils.windowWidth, Utils.windowHeight));
    }

/*********************************Client manager has access to open, save and saveAs***********************************/
    private void mgrOpen() throws IOException {
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

/*************************************************Action listeners*****************************************************/
    // Monitor current using color
    private final ActionListener colorListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent event) {
            JButton src = (JButton) event.getSource();
            // Select a color
            if (src == blackBt) {
                canvas.black();
            } else if (src == whiteBt) {
                canvas.white();
            } else if (src == grayBt) {
                canvas.gray();
            } else if (src == silverBt) {
                canvas.silver();
            } else if (src == maroonBt) {
                canvas.maroon();
            } else if (src == redBt) {
                canvas.red();
            } else if (src == purpleBt) {
                canvas.purple();
            } else if (src == fuchsiaBt) {
                canvas.fuchsia();
            } else if (src == greenBt) {
                canvas.green();
            } else if (src == limeBt) {
                canvas.lime();
            } else if (src == oliveBt) {
                canvas.olive();
            } else if (src == yellowBt) {
                canvas.yellow();
            } else if (src == navyBt) {
                canvas.navy();
            } else if (src == blueBt) {
                canvas.blue();
            } else if (src == tealBt) {
                canvas.teal();
            } else if (src == aquaBt) {
                canvas.aqua();
            }
            if (src != null) {
                colorUse.setBackground(canvas.getColor());
            }
        }
    };

    // Monitor current painting type
    private final ActionListener paintListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent event) {
            JButton src = (JButton) event.getSource();
            // Select a draw type
            if (src == lineBt) {
                canvas.line();
            } else if (src == circleBt) {
                canvas.circle();
            } else if (src == triangleBt) {
                canvas.triangle();
            } else if (src == rectangleBt) {
                canvas.rectangle();
            } else if (src == textBt) {
                canvas.text();
            } else if (src == freeBt) {
                canvas.free();
            } else if (src == eraserBt) {
                canvas.eraser();
            }
            if (src != null) {
                Utils.selectButton(src, drawBts);
            }
        }
    };

    // Monitor functional buttons
    private final ActionListener funcListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent event) {
            JButton src = (JButton) event.getSource();
            // Select a function button
            if (src == newBt) {
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
            } else if (src == openBt) {
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
            } else if (src == saveBt) {
                if (isManager) {
                    try {
                        mgrSave();
                    } catch (IOException e) {
                        System.out.println("Error with saving the canvas!");
                    }
                }
            } else if (src == saveAsBt) {
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

    // Monitor kicking - Double click to kick out clients
    private final MouseListener kickListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent event) {
            @SuppressWarnings("unchecked")
            JList<String> src = (JList<String>) event.getSource();
            if (event.getClickCount() == 2) {
                int index = src.locationToIndex(event.getPoint());
                String kickName = src.getModel().getElementAt(index);
                try {
                    if(!getUsername().equals(kickName)) {
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
    };

    // Monitor quit action
    private final WindowListener quitListener = new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent event) {
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
    };

    // Monitor sending chat
    private final MouseListener sendChatListener = new MouseAdapter() {
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
    };

}
