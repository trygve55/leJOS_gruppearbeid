import lejos.hardware.motor.*;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.port.Port;
import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.robotics.SampleProvider;

class Clean {
	public Clean() {

	}

	public boolean isUp() {

	}

	public void setPos(boolean) { //up = true, down = false

	}

	public void startClean() {

	}

	public void stopClean() {

	}
}

class Drive {
	//differencial pilot

	public Drive(String LeftM, String RightM) {  //m1 = leftM

	}

	public void stop() {

	}

	public void start(int i) { //i = speed

	}

	public void travel(int i) { //i = cm

	}

	public void rotate(int i) { //i = deg

	}

}

class HitraTunnel {

	public static void main(String[] args) {

		// Definerer sensorer:
		Brick brick = BrickFinder.getDefault();
	    Port s1 = brick.getPort("S1"); // fargesensor
	 	Port s2 = brick.getPort("S2"); // trykksensor

		EV3ColorSensor fargesensor = new EV3ColorSensor(s1); // ev3-fargesensor
		SampleProvider fargeLeser = fargesensor.getMode("RGB");  // svart = 0.01..
		float[] fargeSample = new float[fargeLeser.sampleSize()];  // tabell som innholder avlest verdi

		// Beregn verdi for svart
		int svart = 0;
		for (int i = 0; i<100; i++){
			fargeLeser.fetchSample(fargeSample, 0);
			svart += fargeSample[0]* 100;
		}
		svart = svart / 100 + 5;
		System.out.println("Svart: " + svart);

		boolean fortsett = true;

		while (fortsett){ 	// Fortsett s� lenge roboten ikke treffer noe
		   fargeLeser.fetchSample(fargeSample, 0);
	       if (fargeSample[0]*100 > svart){   // sjekk sort linje
	       	  Motor.A.forward();
	          Motor.B.forward();        // viftearm
	          Motor.C.stop();  		// snu i  200 millisekund
	          Thread.sleep(100);
	          System.out.println("hvit");
	       } else {
			   // Kj�r framover
			   Motor.A.forward();
			   Motor.C.forward();
			   System.out.println("svart");
		   }

	}

}
