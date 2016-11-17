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
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;
import lejos.robotics.filter.MeanFilter;

//image
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.io.*;
import javax.imageio.*;
import java.awt.Color;

//server
import java.net.*;

class LightSensors {
	float leftSample, rightSample;
	float rightLightThreshold, leftLightThreshold;

	EV3ColorSensor fargeSensorR, fargeSensorL;
	SampleProvider fargeLeserR, fargeLeserL;
	float[] fargeSampleR, fargeSampleL;

	public LightSensors(String rightSensorPort) {
		Brick brick = BrickFinder.getDefault();
		Screen screen = new Screen();
		//laster sensorer
		screen.printClear("Laster sensorer", 0, 1);

		Port rightSensor = brick.getPort(rightSensorPort);

		this.fargeSensorR = new EV3ColorSensor(rightSensor);
		this.fargeLeserR = fargeSensorR.getRGBMode();
		this.fargeSampleR = new float[fargeLeserR.sampleSize()];
	}

	public int[] getPixel() {
		int[] rgb = new int[3];
		fargeLeserR.fetchSample(fargeSampleR, 0);
		rgb[0] = (int) (fargeSampleR[0] * 255 * 2.0);
		rgb[1] = (int) (fargeSampleR[1] * 255 * 2.1);
		rgb[2] = (int) (fargeSampleR[2] * 255 * 2.4);
		return rgb;
	}

}

class Screen {
	public Screen() {
		Brick brick = BrickFinder.getDefault();
		EV3 ev3 = (EV3) BrickFinder.getLocal();
		TextLCD lcd = ev3.getTextLCD();
	}

	public void print(String text, int x, int y) {
		LCD.drawString(text, x, y);
	}

	public void printClear(String text, int x, int y) {
		LCD.clear();
		LCD.drawString(text, x, y);
	}

	public void printClear(String text) {
		LCD.clear();
		LCD.drawString(text, 0, 0);
	}

	public void clear() {
		LCD.clear();
	}
}

class BrickKeys {
	Keys keys;

	public BrickKeys() {
		Brick brick = BrickFinder.getDefault();
		EV3 ev3 = (EV3) BrickFinder.getLocal();
		this.keys = ev3.getKeys();
	}

	public void waitKey() {
		keys.waitForAnyPress();
	}
}

class MotorControl {
	RegulatedMotor xMotor, yMotor;
	SampleProvider trykksensor;
	float[] trykkSample;
	double yDistance;

	public MotorControl(RegulatedMotor xMotor, RegulatedMotor yMotor, String resetSensor, double yDistance) {
		this.xMotor = xMotor;
		this.yMotor = yMotor;
		xMotor.setSpeed(800);
		yMotor.setSpeed(800);
		
		this.yDistance = yDistance;
		
		Brick brick = BrickFinder.getDefault();
		Port s1 = brick.getPort(resetSensor); 
		this.trykksensor = new NXTTouchSensor(s1);
		this.trykkSample = new float[trykksensor.sampleSize()];
	}
	
	public void xPlus() {
		xMotor.rotate(45);
	}
	
	public void yPlus() {
		yMotor.setSpeed(900);
		yMotor.rotate((int) -yDistance);
	}

	public int getX() {
		return xMotor.getTachoCount();
	}
	
	public int getY() {
		return yMotor.getTachoCount();
	}

	public void resetX() {
		xMotor.setSpeed(800);
		xMotor.backward();
		trykksensor.fetchSample(trykkSample, 0);
		while (trykkSample[0] == 0) {
			trykksensor.fetchSample(trykkSample, 0);
		}
		stopX();
		Delay.msDelay(100);
		resetDistanceX();
	}
	
	public void setSpeedX(int speed) {
		if (speed >= 0) {
			xMotor.setSpeed(speed);
			xMotor.forward();
		} else {
			xMotor.setSpeed(-speed);
			xMotor.backward();
		}
	}
	
	public void stopX(){
		xMotor.setSpeed(0);
		xMotor.stop();
	}
	
	public int getDistanceX() {
		return xMotor.getTachoCount();
	}

	public void resetDistanceX() {
		xMotor.resetTachoCount();
	}
	
	public void stopY(){
		yMotor.stop();
	}
	
	public int getDistanceY() {
		return yMotor.getTachoCount();
	}
	
	public void setYDistance(double yDistance) {
		this.yDistance = yDistance;
	}

	public void resetDistanceY() {
		yMotor.resetTachoCount();
	}
	
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
	
	public ImageArray(int width, int height) {
		bilde = new int[width][height][3];
		img = new BufferedImage(width, height, 1);
	}
	
	public ImageArray() {
		bilde = new int[1][1][3];
		img = new BufferedImage(1, 1, 1);
	}
	
	public void scanPixel(int x, int y, int[] rgb) {
		for (int i = 0;i < 3;i++) bilde[x][y][i] = rgb[i];
	}
	
	public int[][][] getArray() {
		return bilde;
	}
	
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
			
		}
	}

	
	public void setPixel(int x, int y, int[] rgb) {
		bilde[x][y] = rgb;
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

	public void stop() {
		stopScan = true;
	}
	
	public ImageArray scan() {
		Screen screen = new Screen();
		BrickKeys keys = new BrickKeys();
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
	
	private void sendLine(int line) {
		ThreadSendLine tSendLine = new ThreadSendLine(line);
		new Thread(tSendLine).start();
	}
	
	private Thread plusY() {
		ThreadMovePlusY tMovePlusY  = new ThreadMovePlusY();
		Thread thread = new Thread(tMovePlusY);
		thread.start();
		return thread;
	}
	
	private void threadWait(Thread thread) {
		try {
			thread.join();
		} catch (Exception e) {
			System.out.print(e);
		}
	}
}

class Server {
	String[] bufferQueue = new String[5];;
	ServerSocket serverSocket = null;
	DataInputStream inStream;
	PrintStream outStream;
	Socket clientSocket = null;
	
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

class ScannerSettings {
	int width, height, dpi, backOffset;
	boolean stopScan, isScanning;
	
	public ScannerSettings(int width, int height, int dpi, int backOffset) {
		this.width = width;
		this.height = height;
		this.dpi = dpi;
		this.backOffset = backOffset;
		this.stopScan = false;
		this.isScanning = false;
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
	
	public String toString() {
		return "Width: " + width + " mm\nHeight: " + height + " mm\nDPI: " + dpi + "\nOffset: " + backOffset;
	}
}

class Commands  {
	
	ScannerSettings scannerSettings;
	Server server;
	LightSensors light;
	MotorControl motor;
	ImageArray img;
	
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
	
	public Commands(ScannerSettings scannerSettings, Server server, LightSensors light, MotorControl motor, ImageArray img) {
		this.scannerSettings = scannerSettings;
		this.server = server;
		this.light = light;
		this.motor = motor;
		this.img = img;		
	}
	
	public void scanStart() {
		if (!scannerSettings.getIsScanning()) {
			ThreadCommandScanner tCommandScanner = new ThreadCommandScanner(scannerSettings, server, light, motor, img);
			new Thread(tCommandScanner).start();
			scannerSettings.setIsScanning(true);
			scannerSettings.setStopScan(false);
		}
	}
	
	public void scanStop() {
		scannerSettings.setStopScan(true);
		server.send("image stop");
	}
	
	public void resend(String[] dataArgs) {
		System.out.println("resending");
		int x = Integer.parseInt(dataArgs[2]);
		int y = Integer.parseInt(dataArgs[3]);
		int[] pixel = img.getPixel(x, y);
		server.send("image " + x + " " + y + " " + pixel[0] + " " + pixel[1] + " " + pixel[2]);
		server.send("image finished");
	}
	
	public void finished() {
		server.send("image finished");
	}
	
	public void setHeight(String[] dataArgs) {
		scannerSettings.setHeight(Integer.parseInt(dataArgs[2]));
		server.send(scannerSettings.toString());
	}
	
	public void setWidth(String[] dataArgs) {
		scannerSettings.setWidth(Integer.parseInt(dataArgs[2]));
		server.send(scannerSettings.toString());
	}
	
	public void setDPI(String[] dataArgs) {
		scannerSettings.setDPI(Integer.parseInt(dataArgs[2]));
		server.send(scannerSettings.toString());
	}
	
	public void setOffset(String[] dataArgs) {
		scannerSettings.setBackOffset(Integer.parseInt(dataArgs[2]));
		server.send(scannerSettings.toString());
	}
	
	public void wrongUse(String[] dataArgs) {
		server.send("Wrong use of command: " + dataArgs[0]);
	}
	
	public void move(String[] dataArgs) {
		if (dataArgs[1].matches("x")) {
						
		} else if (dataArgs[1].matches("y")) {
			motor.moveToPosYMM(Integer.parseInt(dataArgs[2]));
		} else {
			server.send("Wrong use of command: " + dataArgs[0]);
		}
	}
	
	public void unknown() {
		server.send("Unknown command");
	}
}

class Printer {
	public static void main(String[] args) {
		ScannerSettings scannerSettings = new ScannerSettings(50, 50, 40, 40);
		Server server = new Server(9999);
		LightSensors light = new LightSensors("S4");
		MotorControl motor = new MotorControl(Motor.D, Motor.A, "S1", 88);
		ImageArray img = new ImageArray();
		
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