/**
 * Class of client using the white board
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
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.CENTER;

public class Client extends UnicastRemoteObject implements IClient {

    private static final long serialVersionUID = 1L;
    private String username;
    private boolean isManager = false;
    private boolean hasAccess;
    private Canvas canvas;
    private IBoardMgr server;
    private Hashtable<String, Point> points = new Hashtable<>();

    // Save canvas
    private String canvasName;
    private String canvasPath;

    // UI window
    private JFrame window;
    private final int windowWidth = 1000;
    private final int windowHeight = 800;

    // Emphasize selections with borders
    private final LineBorder border = new LineBorder(Color.BLACK, 2);
    private final LineBorder antiBorder = new LineBorder(new Color(238, 238, 238), 2);

    // Color buttons
    private JButton blackBt, whiteBt, grayBt, silverBt, maroonBt, redBt, purpleBt, fuchsiaBt;
    private JButton greenBt, limeBt, oliveBt, yellowBt, navyBt, blueBt, tealBt, aquaBt;
    private ArrayList<JButton> colorBts = new ArrayList<>();
    private JTextArea colorBox = new JTextArea("Color in use");
    private JTextArea colorUse = new JTextArea("");

    // Draw buttons
    private JButton freeBt, lineBt, circleBt, triangleBt, rectangleBt, textBt, eraserBt;
    private ArrayList<JButton> drawBts = new ArrayList<>();

    // Function buttons
    private JButton newBt, openBt, saveBt, saveAsBt, closeBt, sendBt;
    private ArrayList<JButton> funcBts = new ArrayList<>();

    // Client list
    private DefaultListModel<String> clientList = new DefaultListModel<>();
    private JList<String> clientJList = new JList<>(clientList);
    private JScrollPane clientWindow = new JScrollPane(clientJList);

    // Chat window
    private DefaultListModel<String> chatHistory = new DefaultListModel<>();
    private JList<String> chat;
    private JTextField chatMsg;
    private JScrollPane chatWindow;

    public Client(IBoardMgr server, String username) throws RemoteException {
        this.server = server;
        this.canvas = new Canvas(server, username);
        this.hasAccess = true;
    }


    @Override
    public String getName() throws RemoteException {
        return this.username;
    }

    @Override
    public void setName(String s) throws RemoteException {
        this.username = s;
    }

    @Override
    public void setAsManager() throws RemoteException {
        this.isManager = true;
    }

    @Override
    public boolean isManager() throws RemoteException {
        return isManager;
    }

    @Override
    public boolean needAccess(String name) throws RemoteException {
        return JOptionPane.showConfirmDialog(window,
                "Approve " + name + " to join as an editor.", "New editor request",
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
    public void updateClientList(Set<IClient> clients) throws RemoteException {
        this.clientList = new DefaultListModel<>();
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
        if (draw.getDrawState().equals("start")) {
            points.put(draw.getUsername(), draw.getPoint());
            return;
        }
        // Draw from the start point
        Point start = points.get(draw.getUsername());
        canvas.getG2().setPaint(draw.getColor());

        switch (draw.getDrawState()) {
            // Sync mouse motion
            case "drawing":
                if (draw.getDrawType().equals("eraser")) {
                    canvas.getG2().setStroke(new BasicStroke(15.0f));
                }
                shape = canvas.drawLine(start, draw.getPoint());
                points.put(draw.getUsername(), draw.getPoint());
                canvas.getG2().draw(shape);
                canvas.repaint();
                break;
            case "end":
                if (draw.getDrawType().equals("free") || draw.getDrawType().equals("line")) {
                    shape = canvas.drawLine(start, draw.getPoint());
                } else if (draw.getDrawType().equals("circle")) {
                    shape = canvas.drawCircle(start, draw.getPoint());
                } else if (draw.getDrawType().equals("triangle")) {
                    shape = canvas.drawTriangle(start, draw.getPoint());
                } else if (draw.getDrawType().equals("rectangle")) {
                    shape = canvas.drawRectangle(start, draw.getPoint());
                } else if (draw.getDrawType().equals("text")) {
                    canvas.getG2().setFont(new Font("Calibri", Font.PLAIN, 20));
                    canvas.getG2().drawString(draw.getText(), draw.getPoint().x, draw.getPoint().y);
                } else if (draw.getDrawType().equals("eraser")) {
                    canvas.getG2().setStroke(new BasicStroke(1.0f));
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
    public void forceQuit() throws IOException {
        // Create a separate thread to end the program when the client is forced to quit
        if(!this.hasAccess) {
            Thread t = new Thread(() -> {
                JOptionPane.showMessageDialog(null, "Access denied.",
                        "Warning", JOptionPane.WARNING_MESSAGE);
                System.exit(0);
            });
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
    public byte[] getChatHistory() throws IOException {
        return new byte[0];
    }


    // Client manager has access to open, save and saveAs
    public void mgrOpen() throws IOException {
        FileDialog dialog = new FileDialog(this.window, "Open a canvas", FileDialog.LOAD);
        dialog.setVisible(true);
        if (dialog.getFile() != null) {
            this.canvasPath = dialog.getDirectory();
            this.canvasName = dialog.getFile();
            BufferedImage image = ImageIO.read(new File(canvasName + canvasPath));
            canvas.renderFrame(image);
            ByteArrayOutputStream imageArray = new ByteArrayOutputStream();
            ImageIO.write(image, "png", imageArray);
            server.sendExistCanvas(imageArray.toByteArray());
        }
    }

    private void mgrSave() throws IOException{
        if(this.canvasName == null) {
            JOptionPane.showMessageDialog(null, "Save it as a file first!");
        }
        else {
            ImageIO.write(canvas.getFrame(), "png", new File(canvasPath + canvasName));
        }
    }

    private void mgrSaveAs() throws IOException {
        FileDialog dialog = new FileDialog(window, "Save canvas", FileDialog.SAVE);
        dialog.setVisible(true);
        if (dialog.getFile() != null) {
            this.canvasPath = dialog.getDirectory();
            this.canvasName = dialog.getFile();
            ImageIO.write(canvas.getFrame(), "png", new File(canvasPath + canvasName));
        }
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
                        server.cleanCanvas();
                    } catch (RemoteException e) {
                        JOptionPane.showMessageDialog(null, "Server error, unable to create a new canvas!");
                    }
                }
            } else if (event.getSource() == openBt) {
                if (isManager) {
                    try {
                        mgrOpen();
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
            } else if (event.getSource() == openBt) {
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


    // Initialise and render the UI
    @Override
    public void renderUI(IBoardMgr server) {
        this.window = new JFrame("White Board");
        Container container = this.window.getContentPane();

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

        colorBox.setBackground(new Color(238,238,238));
        colorUse.setBackground(Color.black);

        // Configure drawing buttons
        Icon icon;
        icon = new ImageIcon(String.valueOf(Paths.get("src/icons/free.png").toAbsolutePath()));
        freeBt = new JButton(icon);
        freeBt.setToolTipText("Free-hand");
        this.drawBts.add(freeBt);

        icon = new ImageIcon(String.valueOf(Paths.get("src/icons/line.png").toAbsolutePath()));
        lineBt = new JButton(icon);
        lineBt.setToolTipText("Line");
        this.drawBts.add(lineBt);

        icon = new ImageIcon(String.valueOf(Paths.get("src/icons/circle.png").toAbsolutePath()));
        circleBt = new JButton(icon);
        circleBt.setToolTipText("Circle");
        this.drawBts.add(circleBt);

        icon = new ImageIcon(String.valueOf(Paths.get("src/icons/triangle.png").toAbsolutePath()));
        triangleBt = new JButton(icon);
        triangleBt.setToolTipText("Triangle");
        this.drawBts.add(triangleBt);

        icon = new ImageIcon(String.valueOf(Paths.get("src/icons/rectangle.png").toAbsolutePath()));
        rectangleBt = new JButton(icon);
        rectangleBt.setToolTipText("Rectangle");
        this.drawBts.add(rectangleBt);

        icon = new ImageIcon(String.valueOf(Paths.get("src/icons/text.png").toAbsolutePath()));
        textBt = new JButton(icon);
        textBt.setToolTipText("Text");
        this.drawBts.add(textBt);

        icon = new ImageIcon(String.valueOf(Paths.get("src/icons/eraser.png").toAbsolutePath()));
        eraserBt = new JButton(icon);
        eraserBt.setToolTipText("Eraser");
        this.drawBts.add(eraserBt);

        for (JButton bt: drawBts) {
            bt.setBorder(antiBorder);
            bt.addActionListener(actionListener);
        }

        // Configure function buttons for client manager
        newBt = new JButton("New");
        newBt.setToolTipText("New canvas");
        this.funcBts.add(newBt);

        openBt = new JButton("Open");
        openBt.setToolTipText("Open a canvas");
        this.funcBts.add(openBt);

        saveBt = new JButton("Save");
        saveBt.setToolTipText("Save the canvas");
        this.funcBts.add(saveBt);

        saveAsBt = new JButton("Save As");
        saveAsBt.setToolTipText("Save as a file");
        this.funcBts.add(saveAsBt);

        for (JButton bt: funcBts) {
            bt.addActionListener(actionListener);
        }

        if (!isManager) {
            newBt.setVisible(false);
            openBt.setVisible(false);
            saveBt.setVisible(false);
            saveAsBt.setVisible(false);
        }

        // Show all editors of the canvas.
        clientWindow.setMinimumSize(new Dimension(100, 290));
        if(isManager) {
            clientWindow.setMinimumSize(new Dimension(100, 150));
        }

        // Manager can kick out users
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
                                int dialog = JOptionPane.showConfirmDialog(window,
                                        "Are you sure you want to kick " + kickName + " out?",
                                        "Warning", JOptionPane.YES_NO_OPTION);
                                if(dialog == JOptionPane.YES_OPTION) {
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

        // Configure chat window
        chat = new JList<>(chatHistory);
        chatWindow = new JScrollPane(chat);
        chatWindow.setMinimumSize(new Dimension(100, 100));
        chatMsg = new JTextField();
        sendBt = new JButton("Send");
        sendBt.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if(!chatMsg.getText().equals("")) {
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
                } else {
                    JOptionPane.showMessageDialog(null, "Message cannot be empty.");
                }
            }
        });


        // All clients are forced to quit when the manager leaves
        window.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (isManager) {
                    if (JOptionPane.showConfirmDialog(window,
                            "Are you sure you want to end the session?", "Warning",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                        try {
                            server.removeAllClients();
                        } catch (IOException e) {
                            System.err.println("IO error");
                        } finally {
                            System.exit(0);
                        }
                    }
                } else {
                    if (JOptionPane.showConfirmDialog(window,
                            "Are you sure you want to leave the session?", "Warning",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                        try {
                            server.quitClient(username);
                            server.syncClientList();
                        } catch (RemoteException e) {
                            JOptionPane.showMessageDialog(null, "Unable to connect to the server!");
                        } finally {
                            System.exit(0);
                        }
                    }
                }
            }
        });

        // Configure the UI window
        window.setMinimumSize(new Dimension(windowWidth, windowHeight));
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.setVisible(true);


        // UI design
        GroupLayout layout = new GroupLayout(container);
        container.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        // Horizontal layout
        layout.setHorizontalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(CENTER)
                .addComponent(freeBt)
                .addComponent(lineBt)
                .addComponent(circleBt)
                .addComponent(triangleBt)
                .addComponent(rectangleBt)
                .addComponent(textBt)
                .addComponent(eraserBt)
            ).addGroup(layout.createParallelGroup(CENTER)
                .addComponent(canvas)
                .addComponent(chatWindow).addGroup(layout.createSequentialGroup()
                    .addComponent(chatMsg)
                    .addComponent(sendBt)
                ).addGroup(layout.createSequentialGroup()
                    .addComponent(blackBt)
                    .addComponent(grayBt)
                    .addComponent(maroonBt)
                    .addComponent(purpleBt)
                    .addComponent(greenBt)
                    .addComponent(oliveBt)
                    .addComponent(navyBt)
                    .addComponent(tealBt)
                ).addGroup(layout.createSequentialGroup()
                    .addComponent(whiteBt)
                    .addComponent(silverBt)
                    .addComponent(redBt)
                    .addComponent(fuchsiaBt)
                    .addComponent(limeBt)
                    .addComponent(yellowBt)
                    .addComponent(blueBt)
                    .addComponent(aquaBt)
                )
            ).addGroup(layout.createParallelGroup(CENTER)
                .addComponent(newBt)
                .addComponent(openBt)
                .addComponent(saveBt)
                .addComponent(saveAsBt)
                .addComponent(clientWindow)
                .addComponent(colorBox)
                .addComponent(colorUse)
            )
        );
        // Vertical layout
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(BASELINE)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(freeBt)
                    .addComponent(lineBt)
                    .addComponent(circleBt)
                    .addComponent(triangleBt)
                    .addComponent(rectangleBt)
                    .addComponent(textBt)
                    .addComponent(eraserBt)
                )
                .addComponent(canvas).addGroup(layout.createSequentialGroup()
                    .addComponent(newBt)
                    .addComponent(openBt)
                    .addComponent(saveBt)
                    .addComponent(saveAsBt)
                    .addComponent(clientWindow)
                    .addComponent(colorBox)
                    .addComponent(colorUse)
                )
            )
            .addGroup(layout.createSequentialGroup()
                .addComponent(chatWindow)
                .addGroup(layout.createParallelGroup()
                    .addComponent(chatMsg)
                    .addComponent(sendBt)
                )
            )
            .addGroup(layout.createSequentialGroup().
                addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(blackBt)
                    .addComponent(grayBt)
                    .addComponent(maroonBt)
                    .addComponent(purpleBt)
                    .addComponent(greenBt)
                    .addComponent(oliveBt)
                    .addComponent(navyBt)
                    .addComponent(tealBt)
                )
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(whiteBt)
                    .addComponent(silverBt)
                    .addComponent(redBt)
                    .addComponent(fuchsiaBt)
                    .addComponent(limeBt)
                    .addComponent(yellowBt)
                    .addComponent(blueBt)
                    .addComponent(aquaBt)
                )
            )
        );
        // Same button size
        layout.linkSize(SwingConstants.HORIZONTAL, newBt, openBt, saveBt, saveAsBt);
    }

}
