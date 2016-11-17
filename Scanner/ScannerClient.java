import java.io.*;
import java.net.*;
import java.util.Scanner;

//image
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.io.*;
import javax.imageio.*;
import java.awt.Color;

//graphics
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

class Client {
	String[] bufferQueue = new String[5];;
	Socket serverSocket = null;
	DataInputStream inStream;
	PrintStream outStream;
	
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
	
	public String queueGet() {
		String returnString = bufferQueue[0];
		String[] newArray = new String[bufferQueue.length];
		for (int i = 1;i < bufferQueue.length;i++) {
			newArray[i-1] = bufferQueue[i];
		}
		bufferQueue = newArray;
		return returnString;
	}
	
	public String getQueue() {
		String output = "";
		if (bufferQueue[0] == null) output = null;
		for (int i = 0;bufferQueue[i] != null;i++) {
			output += bufferQueue[i] + ", ";
		}
		return output;
	}
	
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
	
	public ImageArray(String file) {
		
		try {
			img = ImageIO.read(new File(file));
			System.out.println("Virker");
		} catch (IOException e) {
			System.out.println("Virker ikke");
		}
		
		System.out.println("" + img.getWidth());
		
		bilde = new int[img.getWidth()][img.getHeight()][3];
		
		for (int x = 0;x < img.getWidth();x++) {
			for (int y = 0;y < img.getWidth();y++) {
				int argb = img.getRGB(x, y);
				bilde[x][y][0] = (argb >> 16) & 0xFF;
				bilde[x][y][1] = (argb >> 8) & 0xFF;
				bilde[x][y][2] = (argb) & 0xFF ;
				//System.out.println(bilde[x][y][0] + " " + bilde[x][y][1] + " " + bilde[x][y][2]);

			}
		}
	}
		
	public ImageArray(int width, int height) {
		this.bilde = new int[width][height][3];
		this.img = new BufferedImage(width, height, 1);
	}
	
	public void setPixel(int x, int y, int r, int g, int b) {
		bilde[x][y][0] = r;
		bilde[x][y][1] = g;
		bilde[x][y][2] = b;
		
	}
	
	public int getWidth() {
		if (img != null) return img.getWidth();
		return bilde.length;
	}
	
	public int getHeight() {
		if (img != null) return img.getHeight();
		return bilde[0].length;
	}
	
	int getPixelBW(int x, int y) {
		return (bilde[x][y][0] + bilde[x][y][1] + bilde[x][y][2])/3;
	}
	
	public int[] getPixel(int x, int y) {
		return bilde[x][y];
	}
	
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
	
	public void toFile(String name) {
		img = toBufferedImage();
		
		try {
			File outputfile = new File(name + ".png");
			ImageIO.write(img, "png", outputfile);
		} catch (IOException e) {
			
		}
	}
	
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

class Input {
	
	public String getString(String dialog) {
		
		String inputString = "";
		Scanner sc = new Scanner(System.in);
		inputString = sc.nextLine();
		
		return inputString;
	}
}

class Window extends JFrame {
	ImageArray img;
	int width, height;
	ImagePanel tegningen;
	private static final long serialVersionUID = 1L;
	
	public Window(String tittel, ImageArray img, int width, int height) {
		setTitle(tittel);
		setSize(width, height);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.img = img;
		this.width = width;
		this.height = height;
		reDraw(img);
	}
	
	public void reDraw(ImageArray img) {
		tegningen = new ImagePanel(img, width, height);
		add(tegningen);
		validate();
		repaint();
	}
	
}

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


class CommandWindow extends JFrame implements ActionListener{
	Commands commands;
	ScannerSettings scannerSettings;
	Client client;
	
	JButton buttonWidth, buttonHeight, buttonDPI, buttonStartScan, buttonStopScan, buttonOffset;
	TextField textWidth = new TextField(10);
	JLabel labelScannerSettings, labelImageInfo;
	
	private static final long serialVersionUID = 3L;
	
	CommandWindow(String title, Client client, Commands commands, ScannerSettings scannerSettings) {
		super(title);
		setLayout(new GridLayout(4, 3, 10, 10));
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
		int totalSeconds = (int) (2.0*commands.getScanWidth()/120*commands.getScanHeight()); 
		int seconds = totalSeconds % 60;
		int minutes = (totalSeconds % 3600 - totalSeconds % 60)/60;
		int hours = (totalSeconds - totalSeconds % 3600)/3600;
		if (hours > 0) text += hours + " timer, ";
		if (minutes > 0) text += minutes + " min, ";
		if (seconds > 0) text += seconds + " sec";
		return text;
	}
	
	public void updateLabelImageInfo() {
		String text = "<html>Connected: " + "<br>Resolution: " + scannerSettings.getPxWidth() +  "x" + scannerSettings.getPxHeight() + 
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
					Thread.sleep(50);
				} catch (Exception e){
					System.out.println(e);
				}
			}
		}
	}
}

class ScannerSettings {
	int width, height, dpi, backOffset, pxWidth, pxHeight, lineAt;
	boolean connected;
	
	public ScannerSettings(int width, int height, int dpi, int backOffset) {
		this.width = width;
		this.height = height;
		this.dpi = dpi;
		this.backOffset = backOffset;
		this.pxWidth = 0;
		this.pxHeight = 0;
		this.lineAt = 0;
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
	
	public String toString() {
		return "Width: " + width + "mm\nHeight: " + height + "mm\nDPI: " + dpi + "\nOffset: " + backOffset;
	}
	
	public void setConnected(boolean connected) {
		this.connected = connected;
	}
}

class ScannerClient {
	ImageArray img;
	Window window;
	ScannerSettings scannerSettings;
	
	public ScannerClient(ScannerSettings scannerSettings) {
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
						
						for (int y = 0;y < img.getHeight();y++) {
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
							
							window.reDraw(img);
							
							String fileName = "saved";
							img.toFile(fileName);
							System.out.println("Image saved as: " + fileName);
							
						}
					} else if (dataArray.length == 3) {
						img = new ImageArray(Integer.parseInt(dataArray[1]),Integer.parseInt(dataArray[2]));
						System.out.println("Image Dimensions: " + Integer.parseInt(dataArray[1]) + "x" + Integer.parseInt(dataArray[2]) + "px");
						scannerSettings.setPxWidth(Integer.parseInt(dataArray[1]));
						scannerSettings.setPxHeight(Integer.parseInt(dataArray[2]));
						lineAt = 0;
						transmissionComplete = false;
						window = new Window("Enkel grafikk", img, 1400, 1000);
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
		ScannerSettings scannerSettings = new ScannerSettings(50, 50, 40, 40);
		
		
		Client client = null;
		String data = "";
		
		Commands commands = new Commands(client, scannerSettings);
		CommandWindow commandWindow = new CommandWindow("Control", client, commands, scannerSettings);
		
		client = new Client("10.0.1.1",9999);
		//client = new Client("localhost",9999);
		
		commandWindow.setClient(client);
		
		ScannerClient thread = new ScannerClient(scannerSettings);
		thread.threadStarter(client);
		
		while (true) {
			client.send(input.getString(""));
		}
	}
}