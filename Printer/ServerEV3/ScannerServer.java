import lejos.hardware.motor.*;
import lejos.robotics.RegulatedMotor;
import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.hardware.port.Port;
import lejos.hardware.ev3.EV3;
import lejos.hardware.Keys;
import lejos.hardware.Key;
import lejos.hardware.lcd.*;
import lejos.robotics.SampleProvider;
import lejos.hardware.sensor.*;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;
import lejos.robotics.filter.MeanFilter;

//Imports for managing images
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.io.*;
import javax.imageio.*;
import java.awt.Color;

//Imports for socks server
import java.net.*;

class LightSensors {
	EV3ColorSensor colorSensor;
	SampleProvider colorReader;
	float[] colorSample;

	/**
	Constructor to make light sensor control object.
	*/
	public LightSensors(String rightSensorPort) {
		Brick brick = BrickFinder.getDefault();
		//laster sensorer

		Port rightSensor = brick.getPort(rightSensorPort);

		this.colorSensor = new EV3ColorSensor(rightSensor);
		this.colorReader = colorSensor.getRGBMode();
		this.colorSample = new float[colorReader.sampleSize()];
	}

	/**
	Returns RGB color values from EV3-color sensor.
	The format is an array with int values from 0 to 255 {red, green, blue}.
	*/
	public int[] getPixel() {
		int[] rgb = new int[3];
		colorReader.fetchSample(colorSample, 0);
		rgb[0] = (int) (colorSample[0] * 255 * 2.0);
		rgb[1] = (int) (colorSample[1] * 255 * 2.1);
		rgb[2] = (int) (colorSample[2] * 255 * 2.4);
		return rgb;
	}
}

class MotorControl {
	RegulatedMotor xMotor, yMotor;
	SampleProvider trykksensor;
	float[] trykkSample;
	double yDistance;

	/* 
	Et samlet objekt som innholder all direkte kontroll av motorer.
	All kontroll av motorer foregår gjennom denne klassen.
	*/
	
	/**
	
	
	*/
	public MotorControl(RegulatedMotor xMotor, RegulatedMotor yMotor, String resetSensor, double yDistance) {
		this.xMotor = xMotor;
		this.yMotor = yMotor;
		xMotor.setSpeed(800);
		yMotor.setSpeed(800);
		xMotor.setAcceleration(1500);
		yMotor.setAcceleration(6000);
		
		this.yDistance = yDistance;
		
		Brick brick = BrickFinder.getDefault();
		Port s1 = brick.getPort(resetSensor); 
		this.trykksensor = new NXTTouchSensor(s1);
		this.trykkSample = new float[trykksensor.sampleSize()];
		
	}
	
	/**
	Moves scannerhead one mm in negative Y direction(positive Y direction on scanned image).
	*/
	public void yPlus() {
		yMotor.setSpeed(900);
		yMotor.rotate((int) -yDistance);
	}

	/**
	Moves the scanner head to x position 0 and resets the tachometer when touch sensor is triggered.
	*/
	public void resetX() {
		xMotor.setSpeed(800);
		xMotor.backward();
		trykksensor.fetchSample(trykkSample, 0);
		while (trykkSample[0] == 0) {
			trykksensor.fetchSample(trykkSample, 0);
		}
		stopX();
		//Delay.msDelay(300);
		resetDistanceX();
	}
	
	/**
	Runs X direction motor with speed as an argument. Automaticly translates negative speed to backwards movement.
	*/
	public void setSpeedX(int speed) {
		if (speed >= 0) {
			xMotor.setSpeed(speed);
			xMotor.forward();
		} else {
			xMotor.setSpeed(-speed);
			xMotor.backward();
		}
	}
	
	/**
	Stops the X direction motor.
	*/
	public void stopX(){
		xMotor.setSpeed(0);
		xMotor.stop();
	}
	
	/**
	Returns how far the motor controlling the scanner head in X direction have moved since last reset. (In degrees clockwise).
	*/
	public int getDistanceX() {
		return xMotor.getTachoCount();
	}

	/**
	Resets the tachometer for the motor controlling the scanner head in X direction.
	*/
	public void resetDistanceX() {
		xMotor.resetTachoCount();
	}
	
	/**
	Stops the motor controlling the scanner head in Y direction.
	*/
	public void stopY(){
		yMotor.stop();
	}
	
	/**
	Returns how far the motor controlling the scanner head in Y direction have moved since the program started. (In degrees).
	*/
	public int getDistanceY() {
		return yMotor.getTachoCount();
	}
	
	/**
	Adjust the number of degrees an mm is for the motor in Y direction.
	*/
	public void setYDistance(double yDistance) {
		this.yDistance = yDistance;
	}

	/**
	Resets the tachometer for the motor controlling the scanner head in X direction.
	*/
	public void resetDistanceY() {
		yMotor.resetTachoCount();
	}
	
	/**
	Moves the scanner head to tachometer position (pos). Slows down when closing in.
	*/
	public void moveToPosX(int pos) {
		xMotor.setSpeed(400);
		while (getDistanceX() != pos) {
			if (getDistanceX() < pos + 10 && getDistanceX() > pos - 10) xMotor.setSpeed(30);
			else if (getDistanceX() < pos + 30 && getDistanceX() > pos - 30) xMotor.setSpeed(100);
			else if (getDistanceX() < pos + 50 && getDistanceX() > pos - 50) xMotor.setSpeed(200);
			if (getDistanceX() > pos) {
				xMotor.backward();
			} else {
				xMotor.forward();
			}
		}
		xMotor.stop();
	}
	
	/**
	Moves the scanner head (pos) mm in the Y direction. (Opposite direction of image Y.)
	*/
	public void moveToPosYMM(int pos) {
		pos *= 250;
		resetDistanceY();
		yMotor.setSpeed(900);
		while (getDistanceY() != pos) {
			if (getDistanceY() < pos + 100 && getDistanceY() > pos - 100) yMotor.setSpeed(50);
			if (getDistanceY() > pos) {
				yMotor.backward();
			} else {
				yMotor.forward();
			}
		}
		yMotor.stop();
	}
}

class ImageArray {
	int[][][] bilde;
	BufferedImage img = null;
	Color color;
	
	/**
	Creates a new pixel array and BufferedImage. With size width times height pixels.
	*/
	public ImageArray(int width, int height) {
		bilde = new int[width][height][3];
		img = new BufferedImage(width, height, 1);
	}
	/**
	Creates a new pixel array and BufferedImage. With size 1x1 pixel.
	*/
	public ImageArray() {
		bilde = new int[1][1][3];
		img = new BufferedImage(1, 1, 1);
	}
	
	/**
	Changes pixel at pos (x, y) in pixel array to new color values (rgb).
	*/
	public void scanPixel(int x, int y, int[] rgb) {
		for (int i = 0;i < 3;i++) bilde[x][y][i] = rgb[i];
	}
	
	/**
	Returns referance to pixel array.
	*/
	public int[][][] getArray() {
		return bilde;
	}
	
	/**
	Converts pixel array to BufferedImage and saves it as a PNG file.
	*/
	public void toFile(String name) {
		for (int y = 0;y < bilde[0].length;y++) {
			for(int x = 0; x < bilde.length; x++) {
				color = new Color(bilde[x][y][0], bilde[x][y][1], bilde[x][y][2]);
				img.setRGB(x, y, color.getRGB());
			}
		}
		
		try {
			File outputfile = new File(name + ".png");
			ImageIO.write(img, "png", outputfile);
		} catch (IOException e) {
			System.out.println(e.toString());
		}
	}

	
	public void setPixel(int x, int y, int[] rgb) {
		bilde[x][y] = rgb;
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
	Returns array containing the RGB values of pixel at pos (x, y).
	*/
	public int[] getPixel(int x, int y) {
		return bilde[x][y];
	}
}

class Scanner {
	int width, height, backOffset;
	double distanceX, distanceY;
	boolean stopScan = false;
	MotorControl motor;
	LightSensors light;
	Server server;
	ImageArray image;
	ScannerSettings scannerSettings;
	
	/**
	Sets all settings for the scanner and referances to controler objects.
	*/
	public Scanner(MotorControl motor, LightSensors light, Server server, ImageArray image, ScannerSettings scannerSettings, double widthMM, double heightMM, double dpi, int backOffset) {
		this.motor = motor;
		this.light = light;
		this.server = server;
		this.scannerSettings = scannerSettings;
		this.width = (int) (widthMM * dpi / 25.4);
		this.height =  (int) (heightMM * dpi / 25.4);
		this.distanceX = (220.0 / dpi); 
		this.distanceY = (6000.0 / dpi);
		this.backOffset = backOffset;
		
		image = new ImageArray(width, height);
		this.image = image;
		scannerSettings.setImage(image);
		motor.setYDistance(distanceY);
	}
	
	class ThreadSendLine implements Runnable {
		//Server server;
		int y;
		
		public ThreadSendLine(int y) {
			//this.server = server;
			this.y = y;
		}
		
		public void run() {
			
			for (int x = 0; x < image.getWidth();x++) {
				int[] pixel = image.getPixel(x, y);
				server.send("image " + x + " " + y + " " + pixel[0] + " " + pixel[1] + " " + pixel[2]);
			}
		}
	}
	
	class ThreadMovePlusY implements Runnable {
		
		public void run() {
			motor.yPlus();
		}
	}

	/**
	Stops the scan.
	*/
	public void stop() {
		stopScan = true;
	}
	
	/**
	Starts the scanning.
	*/
	public ImageArray scan() {
		scannerSettings.setIsScanning(true);
		String out = "";
		server.send("image " + image.getWidth() + " " + image.getHeight());
		motor.resetX();
		for (int y = 0;y < height && !scannerSettings.getStopScan();y++) {	
			motor.setSpeedX(800);
			for (int x = 0;x < width;) {
				if ((x * distanceX) <= motor.getDistanceX()) {
					image.scanPixel(x, y, light.getPixel());
					x++;
				}	
			}		

			sendLine(y);
			motor.stopX();
			
			if (y < height - 1) {
				y++;
				Thread tMovePlusY = plusY();
				motor.moveToPosX((int) (width*distanceX));
				threadWait(tMovePlusY);
				motor.setSpeedX(-800);
				for (int x = width -1;x >= 0;) {
					if ((x * distanceX - backOffset) >= motor.getDistanceX()) {
						image.scanPixel(x, y, light.getPixel());
						x--;
					}	
				}
				sendLine(y);
			}
			
			Thread tMovePlusY = plusY();
			motor.resetX();
			threadWait(tMovePlusY);
		}
		scannerSettings.setIsScanning(false);
		scannerSettings.setStopScan(false);
		image.toFile("saved");
		return image;
	}
	
	/**
	Starts a thread that sends the pixels from the last scanned line to the client.
	*/
	private void sendLine(int line) {
		ThreadSendLine tSendLine = new ThreadSendLine(line);
		new Thread(tSendLine).start();
	}
	
	/**
	Makes a new thread and moves scanner head 1 pixel in positive image Y direction.
	*/
	private Thread plusY() {
		ThreadMovePlusY tMovePlusY  = new ThreadMovePlusY();
		Thread thread = new Thread(tMovePlusY);
		thread.start();
		return thread;
	}
	
	/**
	Waits for all threads.
	*/
	private void threadWait(Thread thread) {
		try {
			thread.join();
		} catch (Exception e) {
			System.out.print(e);
		}
	}
}

/**
Primitive socket server
*/
class Server {
	String[] bufferQueue = new String[5];;
	ServerSocket serverSocket = null;
	DataInputStream inStream;
	PrintStream outStream;
	Socket clientSocket = null;
	
	/**
	Adds new line to server recieved buffer.
	*/
	public void queueAdd(String command) {
		if (bufferQueue[bufferQueue.length - 1] != null) {
			String[] newArray = new String[bufferQueue.length + 1];
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
	Sends line to connected client.
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
	Sets port for socket server.
	*/
	public Server(int port) {
		
		//Input input = new Input();
		
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("Kunne ikke starte server.\n" + e);
		}
		
		System.out.println("Server started");
		
		try {
			this.clientSocket = serverSocket.accept();
			
			this.inStream = new DataInputStream(clientSocket.getInputStream());
			this.outStream = new PrintStream(clientSocket.getOutputStream());
			
			System.out.println("Connected");
			outStream.println("Connected");
			
			ThreadNetworkListen tNetworkListen = new ThreadNetworkListen(inStream, outStream);
			new Thread(tNetworkListen).start();
			
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}

/**
Object containing all control variables and scanner settings.
*/
class ScannerSettings {
	int width, height, dpi, backOffset;
	boolean stopScan, isScanning;
	ImageArray image;
	
	public ScannerSettings(int width, int height, int dpi, int backOffset) {
		this.width = width;
		this.height = height;
		this.dpi = dpi;
		this.backOffset = backOffset;
		this.stopScan = false;
		this.isScanning = false;
		this.image = null;
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
	
	public boolean getStopScan() {
		return stopScan;
	}
	
	public boolean getIsScanning() {
		return isScanning;
	}
	
	public ImageArray getImage() {
		return image;
	}
	
	public void setWidth(int width) {
		this.width = width;
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
	
	public void setStopScan(boolean stopScan) {
		this.stopScan = stopScan;
	}
	
	public void setIsScanning(boolean isScanning) {
		this.isScanning = isScanning;
	}
	
	public void setImage(ImageArray image) {
		this.image = image;
	}
	
	public String toString() {
		return "Width: " + width + " mm\nHeight: " + height + " mm\nDPI: " + dpi + "\nOffset: " + backOffset;
	}
}

/**
Class containing different commands.
*/
class Commands  {
	
	ScannerSettings scannerSettings;
	Server server;
	LightSensors light;
	MotorControl motor;
	ImageArray img;
	
	public Commands(ScannerSettings scannerSettings, Server server, LightSensors light, MotorControl motor, ImageArray img) {
		this.scannerSettings = scannerSettings;
		this.server = server;
		this.light = light;
		this.motor = motor;
		this.img = img;		
	}
	
	class ThreadCommandScanner implements Runnable{
		ScannerSettings scannerSettings;
		Server server;
		LightSensors light;
		MotorControl motor;
		ImageArray img;
		
		public ThreadCommandScanner(ScannerSettings scannerSettings, Server server, LightSensors light, MotorControl motor, ImageArray img) {
		this.scannerSettings = scannerSettings;
		this.server = server;
		this.light = light;
		this.motor = motor;
		this.img = img;	
		}
		
		public void run() {
			Scanner scanner = new Scanner(motor, light, server, img, scannerSettings, scannerSettings.getWidth(), scannerSettings.getHeight(), scannerSettings.getDPI(), scannerSettings.getBackOffset());	
			img = scanner.scan();
			server.send("image finished");
		}
	}
	
	/**
	Starts a scan in a new thread.
	*/
	public void scanStart() {
		if (!scannerSettings.getIsScanning()) {
			ThreadCommandScanner tCommandScanner = new ThreadCommandScanner(scannerSettings, server, light, motor, img);
			new Thread(tCommandScanner).start();
			scannerSettings.setIsScanning(true);
			scannerSettings.setStopScan(false);
		}
	}
	
	/**
	Stops the scan.
	*/
	public void scanStop() {
		scannerSettings.setStopScan(true);
		server.send("image stop");  
	}
	
	/**
	Resends the pixel at pos (x, y)
	*/
	public void resend(String[] dataArgs) {
		System.out.println("resending");
		int x = Integer.parseInt(dataArgs[2]);
		int y = Integer.parseInt(dataArgs[3]);
		int[] pixel = scannerSettings.getImage().getPixel(x, y);
		server.send("image " + x + " " + y + " " + pixel[0] + " " + pixel[1] + " " + pixel[2]);
		//server.send("image finished");
	}
	
	/**
	Tells the client the scan has finished.
	*/
	public void finished() {
		server.send("image finished");
	}
	
	/**
	Sets the height in the scannerSettings object and sends updated settings to client.
	*/
	public void setHeight(String[] dataArgs) {
		scannerSettings.setHeight(Integer.parseInt(dataArgs[2]));
		server.send(scannerSettings.toString());
	}
	
	/**
	Sets the width in the scannerSettings object and sends updated settings to client.
	*/
	public void setWidth(String[] dataArgs) {
		scannerSettings.setWidth(Integer.parseInt(dataArgs[2]));
		server.send(scannerSettings.toString());
	}
	
	/**
	Sets the DPI in the scannerSettings object and sends updated settings to client.
	*/
	public void setDPI(String[] dataArgs) {
		scannerSettings.setDPI(Integer.parseInt(dataArgs[2]));
		server.send(scannerSettings.toString());
	}
	
	/**
	Sets the offset in the scannerSettings object and sends updated settings to client.
	*/
	public void setOffset(String[] dataArgs) {
		scannerSettings.setBackOffset(Integer.parseInt(dataArgs[2]));
		server.send(scannerSettings.toString());
	}
	
	/**
	Return wrong use of command to client.
	*/
	public void wrongUse(String[] dataArgs) {
		server.send("Wrong use of command: " + dataArgs[0]);
	}
	
	/**
	Moves scanner head specified mm in positive Y direction(opposite of image Y direction.).
	*/
	public void move(String[] dataArgs) {
		if (dataArgs[1].matches("x")) {
						
		} else if (dataArgs[1].matches("y")) {
			motor.moveToPosYMM(Integer.parseInt(dataArgs[2]));
		} else {
			server.send("Wrong use of command: " + dataArgs[0]);
		}
	}
	
	/**
	Send "Unknown command" to client. 
	*/
	public void unknown() {
		server.send("Unknown command");
	}
}

/**
Main class for the program.
*/
public class ScannerServer {
	public static void main(String[] args) {
		ScannerSettings scannerSettings = new ScannerSettings(50, 50, 40, 40);
		Server server = new Server(9999);
		LightSensors light = new LightSensors("S4");
		MotorControl motor = new MotorControl(Motor.D, Motor.A, "S1", 88);
		ImageArray img = new ImageArray();
		scannerSettings.setImage(img);
		
		String data = "";
		String[] dataArgs;
		
		boolean noExit = true;

		Commands commands = new Commands(scannerSettings, server, light, motor, img);
		
		while (noExit) {
			try {
				Thread.sleep(20);
			} catch (Exception e) {
				System.out.println(e);
			}
			
			data = server.queueGet();
			if (data == null) {
				continue;
			}
			
			System.out.println(data);
			dataArgs = data.split(" ");
			try {
				if (dataArgs[0].matches("scan")) {
					if (dataArgs[1].matches("start")) {
						commands.scanStart();
					} else if (dataArgs[1].matches("stop")) {
						commands.scanStop();
					} else if (dataArgs[1].matches("resend")) {
						commands.resend(dataArgs);
					} else if (dataArgs[1].matches("finished")) {
						commands.finished();
					} else if (dataArgs[1].matches("width")) {
						commands.setWidth(dataArgs);
					} else if (dataArgs[1].matches("height")) {
						commands.setHeight(dataArgs);
					} else if (dataArgs[1].matches("dpi")) {
						commands.setDPI(dataArgs);
					} else if (dataArgs[1].matches("offset")) {
						commands.setOffset(dataArgs);
					} else {
						commands.wrongUse(dataArgs);
					}
				} else if (dataArgs[0].matches("exit")) {
					noExit = false;
				} else if (dataArgs[0].matches("move")) {
					commands.move(dataArgs);
				} else {
					commands.unknown();
				}
			} catch (NumberFormatException e) {
				server.send("Feil med argumenter.");
			} catch (ArrayIndexOutOfBoundsException e) {
				server.send(e.toString());
			} catch (Exception e) {
				server.send(e.toString());
			} 
		}
	}
}