Step 1 -> Start the server

	java -jar WhiteBoard.jar StartServer <port>
	
	java -jar WhiteBoard.jar StartServer 3200



Step 2 -> Create a white board:

	java -jar WhiteBoard.jar CreateWhiteBoard <serverIP> <serverPort> <username>
	
	java -jar WhiteBoard.jar CreateWhiteBoard 127.0.0.1 3200 Creator



Step 3 -> Join a white board:

	java -jar WhiteBoard.jar JoinWhiteBoard <serverIP> <serverPort> <username>
	
	java -jar WhiteBoard.jar JoinWhiteBoard 127.0.0.1 3200 User




Note:

If no command line argument is entered, default settings will be loaded.

The icons directory must be located at the same directory
with WhiteBoard.jar to load the buttons successfully.

It may have some UI bugs if running on Mac:
	1. There may have warnings of fonts since macOS Monterey have removed "Times".
	2. The area which shows current using color may not work properly.

Running on Windows should generally be error free,
except that javax.swing may sometimes have bugs.
