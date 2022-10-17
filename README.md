# Distributed Shared White Board

### [Specification](resources/proj2-spec.pdf)



### Run the program

**[Artifacts](artifacts)**

```
# Start the server first
java -jar StartServer.jar <port>
java -jar StartServer.jar 3200

# The first user creates the white board and becomes the manager
java -jar CreateWhiteBoard.jar <serverIP> <serverPort> <username>
java -jar CreateWhiteBoard.jar 127.0.0.1 3200 Creator

# Other users join the white board
java -jar JoinWhiteBoard.jar <serverIP> <serverPort> <username>
java -jar JoinWhiteBoard.jar 127.0.0.1 3200 User
```

**Note: Need to specify the absolute path of directory storing icon images since jar files can only recognise absolute paths. Directory path can be modified in [Client.java#L56](src/main/java/client/Client.java#L56).**



### Introduction

Shared whiteboards allow multiple users to draw simultaneously on a canvas. There are multiple examples found on the Internet that support a range of features such as freehand drawing with the mouse, drawing lines and shapes such as circles and squares that can be moved and resized, and inserting text.

* Report (4 marks)
* Code and Demo of Network-based Distributed Users, Shared Whiteboard:
  * Basic system (12 marks)
  * Advanced features (4 marks)

* Deadline: October 21, 2022 (Friday) at 5:00pm



### Challenges

* #### Dealing with concurrency

  * Regardless of the technology you use, you will have to ensure that access to shared resources is properly handled and that simultaneous actions lead to a reasonable state. ✔
    * ConcurrentHashMap is used when syncing drawings and clients' list.

* #### Structuring your application and handling the system state

  * For example, you can have multiple servers that communicate with each other or a single central one that manages all the system state. ✔
    * A server is set up to manage all the operations. All users need to connect to the server to create or join a white board.

* #### Dealing with networked communication

  * You have to decide when/what messages are sent across the network. ✔
    * Drawings, clients' list and chat.
  * You may have to design an exchange protocol that establishes which messages are sent in which situation and the replies that they should generate. ✔
  * If you use RMI, then you have to design your remote interface(s) and servants. ✔
    * IBoardMgr

* #### Implementing the GUI ✔

  * The functionality can resemble tools like MS Paint. 
  * You can use any tool/API/library you want.
  * e.g.: [Java2D drawing package](http://docs.oracle.com/javase/tutorial/2d/index.html)



### Technology

* Develop a white board that can be shared between multiple users over the network. ✔
* The system must be implemented in Java but you can choose the technology (e.g., it can be even Sockets) you want to use to build your distributed application:
  * Sockets
    * TCP or UDP
    * Message format and exchange protocol
      * can be XML-based or your own format
      * Client can broadcast a canvas with updates to all other clients, other clients reply acknowledging the canvas.
  * Java RMI
    * Remote Objects/Remote Interface ✔
  * File or Database for Storage ✔
    * Store canvas as image files. ✔
  * Please Choose any technology of your choice
    * Make sure that your project can achieve the goal when you are choosing a “new” technology (otherwise, stick to what you already know).



### Basic features

* Multiple users can draw on a shared interactive canvas, created by a manager. ✔
* Your system will support a single whiteboard that is shared between all of the clients. ✔
* Consistent users pool view for all clients. ✔
* Management of all corresponding exceptions. ✔
* Key elements with GUI
  * Shapes: at least your white board should support for line, circle, triangle and rectangle ✔
  * Free-hand drawing ✔
  * Text inputting: allow users to type text anywhere inside the white board ✔
  * User should be able to choose their favourite colour to draw the above features. At least 16 colours  should be available. ✔



### Advanced features

* Chat window (text-based): allow users to communicate with each other by typing a text ✔
* A "File" menu with new, open, save, saveAs and close should be provided (only the manager can control this) ✔
* Allow the manager to kick out a certain peer/user. ✔
* Allow corresponding exceptions should be managed. ✔



### Usage/Operation

* Users must provide a username when joining the whiteboard. There should be a way of uniquely identifying users, either by enforcing unique usernames or automatically generating a unique identifier and associating it with each username. ✔
* All the users should see the same image of the whiteboard and should have the privilege of doing all the drawing operations. ✔
* When displaying a whiteboard, the client user interface should show the usernames of other users who are currently editing the same whiteboard. ✔
* Clients may connect and disconnect at any time. When a new client joins the system, the client should obtain the current state of the whiteboard so that the same objects are always displayed to every active client. ✔
* Only the manager of the whiteboard should be allowed to create a new whiteboard, open a previously saved one, save the current one, and close the application. ✔
* Users should be able to work on a drawing together in real time, without appreciable delays between making and observing edits. ✔



### Proposed Startup/Operational Model

* The first user creates a whiteboard and becomes the whiteboard’s manager. ✔
  * java CreateWhiteBoard <serverIPAddress> <serverPort> username
* Other users can ask to join the whiteboard application any time by inputting server’s IP address and port number. ✔
  * java JoinWhiteBoard <serverIPAddress> <serverPort> username
* A notification will be delivered to the manager if any peer wants to join. The peer can join in only after the manager approves. ✔
  * A dialog showing “someone wants to share your whiteboard”.
* An online peer list should be maintained and displayed. ✔
* All the peers will see the identical image of the whiteboard, as well as have the privilege of doing all the operations. ✔
* Online peers can choose to leave whenever they want. ✔
* The manager can kick someone out at any time. ✔
* When the manager quits, the application will be terminated. All the peers will get a canvas notifying them. ✔



### Progression

* #### Phase 1 (whiteboard)

  * Implement a client that allows a user to draw all the expected elements. ✔
  * Implement a server so that client and server are able to communicate entities created in Task A. ✔

* #### Phase 2 (user management skeleton)

  * Allow the manager to create a whiteboard ✔
  * Allow other peers to connect and join in by getting approval from the manager ✔
  * Allow the manager to choose whether a peer can join in ✔
    * join in means the peer name will appear in the user list ✔
  * Allow the joined peer to choose quit ✔
  * Allow the manager to close the application, and all peers get notified ✔
  * Allow the manager to kick out a certain peer/user ✔

* #### Phase 3

  * Integrate the whiteboard with the user management skeleton ✔
  * What communication mechanism will be used?
    * Socket, RMI, or any other frameworks of your choice.
  * How to propagate the modification from one peer to other peers?
    * You may need an event-based mechanism
  * How many threads do we need per peer?
    * At least one for drawing, one for messaging



### Submission

* #### Report

  * You should write a report that includes the system architecture, communication protocols and canvas formats, design diagrams (class and interaction), implementation details.
  * Don’t document anything you haven’t implemented in the report. This is misconduct and will result in severe penalties.

* #### You need to submit the following via LMS:

  * Your report in PDF format only.
  * The executable jar files used to run your system’s clients/server(s).
  * Your source files in a .ZIP or .TAR archive only.

* #### Demonstrations

  * You will showcase your system and discuss your design choices during the demos.
  * Date and venue will be announced closer to the submission date.



