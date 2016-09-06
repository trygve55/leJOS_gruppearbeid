import lejos.hardware.motor.*;
import lejos.hardware.lcd.*;
import lejos.hardware.BrickFinder;
import lejos.hardware.ev3.EV3;
import lejos.robotics.RegulatedMotor;
import lejos.utility.Delay;
import lejos.robotics.navigation.DifferentialPilot;

public class Firkant
{
	static RegulatedMotor leftMotor = Motor.A;
	static RegulatedMotor rightMotor = Motor.B;

	public static void main(String[] args ) throws Exception
	{
    	float wheelDiameter = 5.7F;
    	float wheelDistance = 16.0F;

    	leftMotor = Motor.B;
    	rightMotor = Motor.A;

    	DifferentialPilot robot = new DifferentialPilot(wheelDiameter,wheelDistance,leftMotor,rightMotor,true);

		EV3 ev3 = (EV3) BrickFinder.getLocal();
		TextLCD lcd = ev3.getTextLCD();
		lcd.drawString("Hello World :)", 4, 4);
		lcd.clear();

		Delay.msDelay(1000);


        robot.setLinearAcceleration(1000);
		robot.setLinearSpeed(40);
		robot.setAngularSpeed(180);
		robot.forward();
		Delay.msDelay(2000);
		robot.stop();
		robot.rotate(90);
		robot.forward();
		Delay.msDelay(2000);
		robot.stop();
		robot.rotate(90);
		robot.forward();
		Delay.msDelay(2000);
		robot.stop();
		robot.rotate(90);
		robot.forward();
		Delay.msDelay(2000);
		robot.stop();
		robot.rotate(90);
		lcd.clear();
		lcd.drawString("Bye World :(", 4, 4);
		Delay.msDelay(2000);
	}
}

