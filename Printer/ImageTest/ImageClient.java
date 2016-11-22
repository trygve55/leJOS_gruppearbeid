import java.io.*;
import java.net.*;
import java.util.Scanner;

//Imports for image
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.io.*;
import javax.imageio.*;
import java.awt.Color;

//Imports for GUI and graphics
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JButton;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
Socket client that connects to the server running on the EV3.
*/
class Client {
	String[] bufferQueue = new String[5];;
	Socket serverSocket = null;
	DataInputStream inStream;
	PrintStream outStream;
	
	/**
	Adds new line to server recieved buffer.
	*/
	public void queueAdd(String command) {
		if (bufferQueue[bufferQueue.length - 1] != null) {
			String[] newArray = new String[bufferQueue.length + 5];
			for (int i = 0;i < bufferQueue.length;i++) {
				newArray[i] = bufferQueue[i];
			}
			bufferQueue = newArray;
		}
		
		for (int i = 0; true;i++) {
			if (bufferQueue[i] == null) {
				bufferQueue[i] = command;
				break;
			}
		}	
	}
	
	/**
	Returns the oldest line in the recived buffer.
	*/
	public String queueGet() {
		String returnString = bufferQueue[0];
		String[] newArray = new String[bufferQueue.length];
		for (int i = 1;i < bufferQueue.length;i++) {
			newArray[i-1] = bufferQueue[i];
		}
		bufferQueue = newArray;
		return returnString;
	}
	
	/**
	Returns entire queue.
	*/
	public String getQueue() {
		String output = "";
		if (bufferQueue[0] == null) output = null;
		for (int i = 0;bufferQueue[i] != null;i++) {
			output += bufferQueue[i] + ", ";
		}
		return output;
	}
	
	/**
	Sends string argument to the server running on the EV3.
	*/
	public void send(String message) {
		outStream.println(message);						
	}

	class ThreadNetworkListen implements Runnable {
		DataInputStream inStream;
		PrintStream outStream;
		
		public ThreadNetworkListen(DataInputStream inStream, PrintStream outStream) {
			this.inStream = inStream;
			this.outStream = outStream;
		}
		
		public void run() {
			String incomingLine = "";
			try {
				while (true) {
					if ((incomingLine = inStream.readLine()) != null) {
						queueAdd(incomingLine);
					}
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}
	
	/**
	Sets EV3 server host and port.
	*/
	public Client(String host,int port) {
		
		Input input = new Input();
		
		boolean noExit = true;
		
		while (noExit) {
			noExit = false;
			try {
				this.serverSocket = new Socket(host, port);
				this.outStream = new PrintStream(serverSocket.getOutputStream());
				this.inStream = new DataInputStream(serverSocket.getInputStream());
			} catch (UnknownHostException e) {
				System.err.println("Unknown host");
			} catch (IOException e) {
				System.err.println("No connection (no I/O stream)");
				noExit = true;
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
					
				}
			}
			
		}
		
		if (serverSocket != null && outStream != null && inStream != null) {
						
			ThreadNetworkListen tNetworkListen = new ThreadNetworkListen(inStream, outStream);
			new Thread(tNetworkListen).start();
			
		}
	}
}

class ImageArray {
	
	BufferedImage img = null;
	int[][][] bilde;
	
	/**
	Creates a new pixel array and BufferedImage. With size width times height pixels.
	*/
	public ImageArray(int width, int height) {
		this.bilde = new int[width][height][3];
		this.img = new BufferedImage(width, height, 1);
	}
	
	public ImageArray(String file) {
		
		try {
			img = ImageIO.read(new File(file));
			System.out.println("Virker");
		} catch (IOException e) {
			System.out.println("Virker ikke");
		}
		
		System.out.println(img.getWidth()+" " +img.getHeight());
		
		bilde = new int[img.getWidth()][img.getHeight()][3];
		
		for (int y = 0;y < img.getHeight();y++) {
			for (int x = 0;x < img.getWidth();x++) {
				//System.out.println(x + " " + y);
				int argb = img.getRGB(x, y);
				bilde[x][y][0] = (argb >> 16) & 0xFF;
				bilde[x][y][1] = (argb >> 8) & 0xFF;
				bilde[x][y][2] = (argb) & 0xFF ;
				
				//System.out.println(bilde[x][y][0] + " " + bilde[x][y][1] + " " + bilde[x][y][2]);
			}
			System.out.println("Line: " + y);
		}
	}
	
	public void fixImage(int i) {
		int[][][] newArray = new int[img.getWidth()][img.getHeight()][3];
		
		for (int y = 0; y < bilde[0].length;y++) {
			for (int x = 0; x < bilde.length;x++) {
				for (int c = 0; c < 3;c++) {
					if (y % 2== 0) {
						newArray[x][y][c] = bilde[x][y][c];
					} else {
						try {
							newArray[x][y][c] = bilde[x+i][y][c];
						} catch (Exception e) {
							//System.out.println(e);
							newArray[x][y][c] = ((i > 0) ? bilde[img.getWidth() - 1][y][c] : bilde[0][y][c]);
						}
					}
				}
			}
		}
		
		this.bilde = newArray;
	}
	
	/**
	Sets the color values for pixel at pos (x, y) to (r, g, b).
	*/
	public void setPixel(int x, int y, int r, int g, int b) {
		bilde[x][y][0] = r;
		bilde[x][y][1] = g;
		bilde[x][y][2] = b;
		
	}
	
	/**
	Returns image width in pixels.
	*/
	public int getWidth() {
		if (img != null) return img.getWidth();
		return bilde.length;
	}
	
	/**
	Returns image height in pixels.
	*/
	public int getHeight() {
		if (img != null) return img.getHeight();
		return bilde[0].length;
	}
	
	/**
	Returns array with RGB values of pixel.
	*/
	public int[] getPixel(int x, int y) {
		return bilde[x][y];
	}
	
	/**
	Returns position of missing pixel(not recived pixel). Returns (-1, -1) if none missing pixels.
	*/ 
	public int[] getMissingPixel() {
		for (int y = 0;y < bilde[0].length;y++) {
			for(int x = 0; x < bilde.length; x++) {
				if (bilde[x][y][0] == 0 && bilde[x][y][1] == 0 && bilde[x][y][2] == 0) {
					int[] array = {x, y};
					return array; 
				}
			}
		}
		int[] array = {-1, -1};
		return array; 
	}
	
	/**
	Converts pixel array to BufferedImage and returns referance to BufferedImage.
	*/
	public BufferedImage getImage() {
		Color color;
		for (int y = 0;y < bilde[0].length;y++) {
			for(int x = 0; x < bilde.length; x++) {
				// for (int c = 0; c < 3; c++) {
					// int rgb 
				// }
				color = new Color(bilde[x][y][0], bilde[x][y][1], bilde[x][y][2]);
				img.setRGB(x, y, color.getRGB());
			}
		}
		return img;
	}
	
	/**
	Saves BufferedImage to file.
	*/
	public void toFile(String name) {
		img = toBufferedImage();
		
		try {
			File outputfile = new File(name + ".png");
			ImageIO.write(img, "png", outputfile);
		} catch (IOException e) {
			
		}
	}
	
	/**
	Converts pixel array to BufferedImage.
	*/
	public BufferedImage toBufferedImage() {
		Color color;
		for (int y = 0;y < bilde[0].length;y++) {
			for(int x = 0; x < bilde.length; x++) {
				// for (int c = 0; c < 3; c++) {
					// int rgb 
				// }
				color = new Color(bilde[x][y][0], bilde[x][y][1], bilde[x][y][2]);
				img.setRGB(x, y, color.getRGB());
			}
		}
		
		return img;
	}
	
	/**
	Adjusts the contrast of the pixel array.
	*/
	public void autoContrast() {
		int lowest = 255, highest = 0;
		for (int y = 0;y < bilde[0].length;y++) {
			for(int x = 0; x < bilde.length; x++) {
				for (int c = 0; c < 3; c++) {
					if (lowest > bilde[x][y][c]) lowest = bilde[x][y][c];
					if (highest < bilde[x][y][c]) highest = bilde[x][y][c];
				}
			}
		}
		
		float correctionRatio = (float) 255.0 / (highest - lowest);
		
		for (int y = 0;y < bilde[0].length;y++) {
			for(int x = 0; x < bilde.length; x++) {
				for (int c = 0; c < 3; c++) {
					bilde[x][y][c]  = (int) ((bilde[x][y][c] - lowest) * correctionRatio);
				}
			}
		}
	}
}

/**
Input class
*/
class Input {
	
	/**
	Gets input from user and return string of current line when the return key is pressed.
	*/
	public String getString(String dialog) {
		
		String inputString = "";
		Scanner sc = new Scanner(System.in);
		inputString = sc.nextLine();
		
		return inputString;
	}
	
	public int getInt(String dialog, int minimum, int maksimum) {
		
		int inputTall = 0;
		boolean noExit = true;
		
		while (noExit) {
			System.out.println(dialog);
			Scanner sc = new Scanner(System.in);
			
			try {
    			inputTall = sc.nextInt();
			} catch (Exception e) {
				System.out.println("ikke int");
				continue;
			}
			
			if (inputTall < minimum) {
				System.out.println("Tallet kan ikke vaere lavere en " + minimum + "!");
				continue;
			} else if (inputTall > maksimum) {
				System.out.println("Tallet kan ikke vaere hoyere en " + maksimum + "!");
				continue;
			} else {
				noExit = false;
			}
		}
		return inputTall;
	}
}

/**
Window showing the buffered image.
*/
class Window extends JFrame {
	ImageArray img;
	int width, height;
	ImagePanel tegningen;
	private static final long serialVersionUID = 1L;
	
	/**
	Creates the new window.
	*/
	public Window(String tittel, ImageArray img, int width, int height) {
		setTitle(tittel);
		setSize(width, height);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.img = img;
		this.width = width;
		this.height = height;
		reDraw(img);
	}
	
	/**
	Redraws the buffered image.
	*/
	public void reDraw(ImageArray img) {
		tegningen = new ImagePanel(img, width, height);
		add(tegningen);
		validate();
		repaint();
	}
	

}

/**
Panel containing the BufferedImage.
*/
class ImagePanel extends JPanel {
	int width, height;
	double scale;
	private static final long serialVersionUID = 2L;
	
	private BufferedImage image;

    public ImagePanel(ImageArray image, int width, int height) {
		
		if (width/image.getWidth() > height/image.getHeight()) {
			this.scale = height/image.getHeight();
		} else {
			this.scale = width/image.getWidth();
		}
		
		this.width = (int) ((double) width*scale);
		this.height = (int) ((double) height*scale);
		
		this.width = width;
		this.height = height;
		
		try {                
			this.image = image.toBufferedImage(); //ImageIO.read(new File("saved.png"));
		} catch (Exception ex) {
				// handle exception...
		}
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, width, height, null); // see javadoc for more info on the parameters            
    }
}

class Commands {
	Client client;
	ScannerSettings scannerSettings;
	
	public Commands(Client client, ScannerSettings scannerSettings) {
		this.client = client;
		this.scannerSettings = scannerSettings;
	}
	
	public void setScanWidth(int width) {
		scannerSettings.setWidth(width);
		printAndSend("scan width " + width);
	}
	
	public void setScanHeight(int height) {
		printAndSend("scan height " + height);
	}
	
	public void setScanDPI(int dpi) {
		printAndSend("scan dpi " + dpi);
	}
	
	public void moveY(int mm) {
		printAndSend("move y " + mm);
	}
	
	public void scanStart() {
		printAndSend("scan stop");
	}
	
	public void scanStop() {
		printAndSend("scan stop");
	}
	
	public int getScanWidth() {
		return scannerSettings.getWidth();
	}
	
	public int getScanHeight() {
		return scannerSettings.getHeight();
	}
	
	public int getScanDPI() {
		return scannerSettings.getDPI();
	}
	
	public int getScanOffset() {
		return scannerSettings.getBackOffset();
	}
	
	private void printAndSend(String command) {
		System.out.println(command);
		client.send(command);
	}
}

/**
Command window, shows all info and control buttons for the scanner.
*/
class CommandWindow extends JFrame implements ActionListener{
	Commands commands;
	ScannerSettings scannerSettings;
	Client client;
	
	JButton buttonWidth, buttonHeight, buttonDPI, buttonStartScan, buttonStopScan, buttonOffset;
	TextField textWidth = new TextField(10);
	JLabel labelScannerSettings, labelImageInfo;
	
	private static final long serialVersionUID = 3L;
	
	/**
	Creates the command window.
	*/
	CommandWindow(String title, Client client, Commands commands, ScannerSettings scannerSettings) {
		super(title);
		setLayout(new GridLayout(3, 3, 10, 10));
		setSize(500, 400);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);   
		
		this.commands = commands;
		this.scannerSettings = scannerSettings;
		this.client = client;
		
		addButtons();
		addLabels();
		setVisible(true);
	}
	
	
	public void setClient(Client client) {
		this.client = client;
	}
	
	public void addButtons() {
		
		buttonWidth = addButton("Set Width", "scan width ", "Set width(mm): ");
		buttonHeight = addButton("Set height", "scan height ", "Set height(mm): ");
		buttonDPI = addButton("Set DPI", "scan dpi ", "Set DPI: ");
		buttonOffset = addButton("Set backOffset", "scan offset ", "Set Backoffset(deg): ");
		buttonStartScan = addButton("Start scan", "scan start");
		buttonStartScan = addButton("Stop scan", "scan stop");
		buttonOffset = addButton("Move Y", "move y ", "Move Y(mm): ");
		
	}
	
	public void addLabels() {
		labelScannerSettings = addLabel("scanner settings");
		labelImageInfo = addLabel("image info");
		
		ThreadLabelUpdate tLabelUpdate = new ThreadLabelUpdate(this);
		new Thread(tLabelUpdate).start();
		updateLabelScannerSettings();
	}
	
	public JLabel addLabel(String title) {
		JLabel label = new JLabel(title);
		add(label);
		return label;
	}
	
	public void updateLabelScannerSettings() {
		String text = "<html>Width: " + commands.getScanWidth() +  " mm<br>Height: " + commands.getScanHeight() +
		" mm<br>Resolution: " + commands.getScanDPI() + " DPI<br>Offset: " + commands.getScanOffset() + " deg<br>Estimated time: " + 
		estimateTime() + "</html>";
		labelScannerSettings.setText(text);
	}
	
	private String estimateTime() {
		String text = "";
		int totalSeconds = (int) ((2.0*commands.getScanWidth()/120)*commands.getScanHeight())*(40/commands.getScanDPI()); 
		int seconds = totalSeconds % 60;
		int minutes = (totalSeconds % 3600 - totalSeconds % 60)/60;
		int hours = (totalSeconds - totalSeconds % 3600)/3600;
		if (hours > 0) text += hours + " timer, ";
		if (minutes > 0) text += minutes + " min, ";
		if (seconds > 0) text += seconds + " sec";
		return text;
	}
	
	public void updateLabelImageInfo() {
		String text = "<html>Connected: " + scannerSettings.getConnected() + "<br>Resolution: " + scannerSettings.getPxWidth() +  "x" + scannerSettings.getPxHeight() + 
		"px<br>Progress: line " + scannerSettings.getLineAt() + "/" + scannerSettings.getPxHeight() + "</html>";
		labelImageInfo.setText(text);
	}
	
	private void updateLabels() {
		updateLabelScannerSettings();
		updateLabelImageInfo();
		revalidate();
		repaint();
	}
	
		
	private JButton addButton(String buttonText, String command, String dialogText) {
		JButton button = new JButton(buttonText);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String input = JOptionPane.showInputDialog(null, dialogText);
				System.out.println(command + input);
				client.send(command + input);
				updateLabels();
			}
		});
		add(button);
		return button; 
	}
	
	private JButton addButton(String buttonText, String command) {
		JButton button = new JButton(buttonText);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println(command);
				client.send(command);
				updateLabels();
				
			}
		});
		add(button);
		return button; 
	}

	
	@Override
	public void actionPerformed(ActionEvent e) {
		JOptionPane.showMessageDialog(this, "" + e);
	}
	
	class ThreadLabelUpdate implements Runnable {
		CommandWindow commandWindow;
		
		public ThreadLabelUpdate(CommandWindow commandWindow) {
			this.commandWindow = commandWindow;
		}
		
		public void run() {
			while (true) {
				commandWindow.updateLabels();
				try {
					Thread.sleep(100);
				} catch (Exception e){
					System.out.println(e);
				}
			}
		}
	}
}

class ScannerSettings {
	int width, height, dpi, backOffset, pxWidth, pxHeight, lineAt;
	boolean connected, stopScan;
	
	public ScannerSettings(int width, int height, int dpi, int backOffset) {
		this.width = width;
		this.height = height;
		this.dpi = dpi;
		this.backOffset = backOffset;
		this.pxWidth = 0;
		this.pxHeight = 0;
		this.lineAt = 0;
		this.stopScan = false;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getDPI() {
		return dpi;
	}
	
	public int getBackOffset() {
		return backOffset;
	}
	
	public int getPxWidth() {
		return pxWidth;
	}
	
	public int getPxHeight() {
		return pxHeight;
	}
	
	public int getLineAt() {
		return lineAt;
	}
	
	public boolean getConnected() {
		return connected;
	}
	
	public boolean getStopScan() {
		return stopScan;
	}
	
	public void setWidth(int width) {
		this.width = width;
		System.out.println("setting width");
	}
	
	public void setHeight(int height) {
		this.height = height;
	}
	
	public void setDPI(int dpi) {
		this.dpi = dpi;
	}
	
	public void setBackOffset(int backOffset) {
		this.backOffset = backOffset;
	}
	
	public void setPxWidth(int pxWidth) {
		this.pxWidth = pxWidth;
	}
	
	public void setPxHeight(int pxHeight) {
		this.pxHeight = pxHeight;
	}
	
	public void setLineAt(int lineAt) {
		this.lineAt = lineAt;
	}
	
	public void setStopScan(boolean stopScan) {
		this.stopScan = stopScan;
	}
	
	
	public String toString() {
		return "Width: " + width + "mm\nHeight: " + height + "mm\nDPI: " + dpi + "\nOffset: " + backOffset;
	}
	
	public void setConnected(boolean connected) {
		this.connected = connected;
	}
}

class ImageClient {
	ImageArray img;
	Window window;
	ScannerSettings scannerSettings;
	
	public ImageClient(ScannerSettings scannerSettings) {
		this.scannerSettings = scannerSettings;
	}
	
	class ThreadListen implements Runnable {
		Client client;
		
		public ThreadListen(Client client) {
			this.client = client;
		}
		
		public void run() {
			String data = "";
			String dataArray[] = null;
			int lineAt = -1;
			boolean transmissionComplete = false;
			
			while (true) {
				try {
					Thread.sleep(1);
				} catch (Exception e) {
					System.out.println(e);
				}
				
				data = client.queueGet();
				
				if (data == null) {
					continue;
				}
				
				dataArray = data.split(" ");
				
							
				if (dataArray[0].matches("image")) {
					if (dataArray[1].matches("finished")) {
						boolean missingPixel = false;
						
						for (int y = 0;y < lineAt;y++) {
							for(int x = 0; x < img.getWidth(); x++) {
								int[] pixel = img.getPixel(x, y);
								if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
									System.out.println("Missing pixel: " + x + " " + y);
									client.send("scan resend " + x + " " + y);
									missingPixel = true;
								}
							}
						}
												
						if (!missingPixel && transmissionComplete == false) {
							
							transmissionComplete = true;
							System.out.println("Transmission completed.");
							
							img.autoContrast();
							lineAt = -1;
							window.reDraw(img);
							
							String fileName = "saved";
							img.toFile(fileName);
							System.out.println("Image saved as: " + fileName);
							
						} else {
							try {
								Thread.sleep(800);
							} catch(Exception e) {
								System.out.println(e);
							}
						}
					} else if (dataArray[1].matches("stop")) {
						scannerSettings.setStopScan(true);
					} else if (dataArray.length == 3) {
						img = new ImageArray(Integer.parseInt(dataArray[1]),Integer.parseInt(dataArray[2]));
						System.out.println("Image Dimensions: " + Integer.parseInt(dataArray[1]) + "x" + Integer.parseInt(dataArray[2]) + "px");
						scannerSettings.setPxWidth(Integer.parseInt(dataArray[1]));
						scannerSettings.setPxHeight(Integer.parseInt(dataArray[2]));
						lineAt = 0;
						transmissionComplete = false;
						
						//setting window size
						Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
						double aspect = (double) Integer.parseInt(dataArray[2])/Integer.parseInt(dataArray[1]);
						double width = screenSize.getWidth() - 40;
						double height = screenSize.getHeight() - 40;
						int windowWidth, windowHeight;
						
						System.out.println(width + " " + height + " " + aspect);
						
						if (width*aspect > height/aspect) {
							windowWidth = (int) (height/aspect);
							windowHeight = (int) height;
						} else {
							windowWidth = (int) (width);
							windowHeight = (int) (height*aspect);
						}
						System.out.println(windowWidth + " " + windowHeight);
						
						window = new Window("Scan", img, windowWidth, windowHeight);
						window.setVisible(true);
						
					} else if (dataArray.length == 6) {
						int[] xyrgb = new int[5];
						
						for (int i = 0;i < 5;i++) {
							xyrgb[i] = Integer.parseInt(dataArray[i + 1]);
						}
						
						if (lineAt <= xyrgb[1]) {
							lineAt++;
							scannerSettings.setLineAt(lineAt);
							System.out.println("Line " + xyrgb[1]);
							window.reDraw(img);
						}
						img.setPixel(xyrgb[0], xyrgb[1], xyrgb[2], xyrgb[3], xyrgb[4]);
					}
				} else if (dataArray[0].matches("Width:")) {
					scannerSettings.setWidth(Integer.parseInt(dataArray[1]));
				} else if (dataArray[0].matches("Height:")) {
					scannerSettings.setHeight(Integer.parseInt(dataArray[1]));
				} else if (dataArray[0].matches("DPI:")) {
					scannerSettings.setDPI(Integer.parseInt(dataArray[1]));
				} else if (dataArray[0].matches("Offset:")) {
					scannerSettings.setBackOffset(Integer.parseInt(dataArray[1]));
				} else if (data != null) {
					System.out.println(data);
					// for (int i = 0;i < dataArray.length;i++) {
						// System.out.println(i + " " + dataArray[i]);
					// }
				}
			} 
		}
	}
	
	private void threadStarter(Client client) {
		ThreadListen tListen = new ThreadListen(client);
		new Thread(tListen).start();
		
	}
	
	public static void main(String[] args) {
		Input input = new Input();
		
		ImageArray img = new ImageArray("saved.png");
		
		Window window = new Window("Scan", img, 800, 600);
		window.setVisible(true);
		
		String inputString = "";
		
		while (true) {
			img.fixImage(input.getInt("offset", -50, 50));
			window.reDraw(img);
		}
	}
}