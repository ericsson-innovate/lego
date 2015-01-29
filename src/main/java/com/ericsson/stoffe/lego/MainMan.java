package com.ericsson.stoffe.lego;

import java.io.File;
import java.io.IOException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.hardware.BrickInfo;
import lejos.hardware.Sound;
import lejos.hardware.device.DLights;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.I2CPort;
import lejos.hardware.sensor.I2CSensor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.robotics.EncoderMotor;
import lejos.utility.Delay;

public class MainMan {
	private static MqttClient mqtt;
	private static boolean engineRunning = false;
	private static boolean fansActive = false;
	private static boolean alarmOn = false;
	private static boolean trunkOpen = false;
	private static boolean doorsLocked = true;

	private static DLights lights = null;
	private static int lightN = 1;
	private static int[] lightRGB = { 255, 255, 255 };
	private static EV3MediumRegulatedMotor ma,mc,md;
	private static EV3LargeRegulatedMotor mb;
	private static MusicMaker alarmMaker, engineSoundMaker;

	private static String mqtthost =  "mafalda.hack.att.io";
	private static String mqttport = "11883";

	private static String[] pickupDirections = {"forward-5000", "rightTurn", "forward-2000", "leftTurn", "forward-5000", "stop-2000", "backward-2000", "stop-2000"};
	private static String[] parkDirections = {"forward-2000", "stop-2000", "rightTurn", "backward-2000", "leftTurn", "backward-5000", "stop-3000"};
	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws MqttException
	 * @throws IOException
	 */
	public static void main(String[] args) throws InterruptedException,
			MqttException, IOException {

		// BrickInfo[] info = BrickFinder.discover();
		// Brick brick = BrickFinder.getDefault();
		// Port p1 = brick.getPort("S1");

		if (args.length == 0) {
			;
		}
		else if (args.length == 1) {
			mqtthost = args[0];
		} else if (args.length == 2) {
			mqtthost = args[0];
			mqttport = args[1];
		} else {
			System.out.println("Invalid number of arguments. Usage lego-1.0-standalone.jar [host] [mqttport] [default: mafalda.hack.att.io, ]");
			System.exit(0);
		}


		System.out.println("Using host " + mqtthost + " and port " + mqttport + "...");


		Port p1 = LocalEV3.get().getPort("S1");

		//System.out.println("Port =" + p1.getPortType() + " sensor ="  + p1.getSensorType());

		MainMan.connectToMQTT();
		// myPort.setTypeAndMode(arg0, arg1)
		//lights = new DLights(port1);

		System.out.println("Laser = " + lights);

		while (true) {
			// connectToMQTT();
			Delay.msDelay(5000);
			// Thread.sleep(5000);
			System.out.println("Tick ---");
		}
	}

	static void connectToMQTT() throws MqttException {
		MemoryPersistence persistence = new MemoryPersistence();
		// MqttDefaultFilePersistence persistence = new
		// MqttDefaultFilePersistence(dir);

		mqtt = new MqttClient("tcp://" + mqtthost + ":" + mqttport, "LegoClient",
				// mqtt = new MqttClient("tcp://12.144.186.180:1883", "LegoClient",
				persistence);
		MqttConnectOptions connOpt = new MqttConnectOptions();
		connOpt.setConnectionTimeout(60 * 10);
		connOpt.setKeepAliveInterval(60 * 5);
		// mqtt.setTimeToWait(60000);
		mqtt.connect(connOpt);

		mqtt.setCallback(new MqttCallback() {

			@Override
			public void connectionLost(Throwable cause) {
				System.out.println("connectionLost : "
						+ cause.getLocalizedMessage());
				cause.printStackTrace();
				try {
					MainMan.connectToMQTT();
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// Log.d("WingMan", );
			}

			// Start engine with condition preheat (backwards rotation)
			public void startEngine(boolean preheat){
				// Engine running condition
				if (engineRunning)
					// Keeps engine running in any direction - fans active or not
					System.out.println("Engine On! Engine already running. Doing nothing.");
				else{
					engineRunning = true;
				    playSample("engine_on.wav");
					System.out.println("Engine On! Starting engine on motorport D.");
					md = new EV3MediumRegulatedMotor(MotorPort.D);
					// Set down speed
					md.setSpeed(100);
					md.forward();
				}
				// Preheat condition
				if (!fansActive && preheat){
					//Delay and activate fans
					System.out.println("Preheat! As we know engine is running lets wait a little then make sure to activate fans ");
					Delay.msDelay(2000);
					md.backward();
					fansActive = true;
				}
			}

			//Stops engine
			public void stopEngine(){
				// Stop engine and preheat
				if (engineRunning){
					System.out.println("Engine Off! Stopping engine and fans. Stopping engine on motorport D.");
					engineRunning = false;
					fansActive = false;
					md.stop();
					md.close();
				}
				else {
					System.out.println("Engine Off! Engine already off. Doing nothing.");
				}
			}

			// Does one blink on sensor port "addr" duration "milliseconds"
			public void blink(int addr, int delay){
				I2CSensor dummy = new I2CSensor(SensorPort.S4, I2CPort.TYPE_HIGHSPEED);

				try {
					// Blinking white. Assume daisy chain is set to individual
					lights = new DLights(dummy.getPort());
					lights.enable(addr);

					lights.setColor(1, 254, 254, 254);
					lights.setColor(2, 254, 254, 254);
					lights.setColor(3, 254, 254, 254);
					lights.setColor(4, 254, 254, 254);

					// keep light on delay ms
					Delay.msDelay(delay);

				} catch (Throwable t) {
					System.err.println("Failed with Lights in blink function ... "
							+ t.getLocalizedMessage());
				}
				lights.disable(addr);
				System.out.println("Off = " + lights.isEnabled(addr));
				dummy.close();
			}

			// Opens trunk
			public void openTrunk(){
				if (trunkOpen){
					System.out.println("Open Trunk! Trunk already open. Blinking lights.");
					blink(4,200);
				} else {
					System.out.println("Open Trunk! Opening trunk.");
					// Trunk engine
					try {
						mc = new EV3MediumRegulatedMotor(MotorPort.C);
						mc.forward();
						Delay.msDelay(5000);
						mc.stop();
						mc.close();
					} catch (Throwable t) {
						System.err.println("Failed with opeing trunk - motor C... "
								+ t.getLocalizedMessage());
						t.printStackTrace();
					}
					trunkOpen = true;
				}
			}

			// Close trunk
			public void closeTrunk(){
				if (trunkOpen){
					System.out.println("Close Trunk! Closing trunk.");
					// Trunk engine
					try {
						mc = new EV3MediumRegulatedMotor(MotorPort.C);
						mc.backward();
						Delay.msDelay(5000);
						mc.stop();
						mc.close();
					} catch (Throwable t) {
						System.err.println("Failed with closing trunk - motor C... "
								+ t.getLocalizedMessage());
						t.printStackTrace();
					}
					trunkOpen = false;
				} else {
					System.out.println("Close Trunk! Trunk already closed. Blinking lights.");
					blink(4,200);
				}
			}

			// Locks door
			public void doorLock(){
				if (doorsLocked){
					System.out.println("Lock! Doors already locked. Blinking lights.");
					blink(4,200);
				} else {
					System.out.println("Lock! Locking doors. Blink lights make sound.");
					doorsLocked = true;
					playSample("lock.wav");
					blink(4,200);

				}
			}

			// Unlocks door
			public void doorUnlock(){
				if (doorsLocked){
					System.out.println("Door unlock! Unlocking doors. Blink lights make sound.");
					doorsLocked = false;
					blink(4,200);
					playSample("unlock.wav");

				} else {
					System.out.println("Door unlock! Doors already unlocked. Blinking lights.");
					blink(4,200);
				}
			}

			// Honks
			public void honk(){
				System.out.println("Honk! Make sound.");
				playSample("honk.wav");
			}

			// Honks
			public void blink(){
				System.out.println("Blink! Blinking lights.");
				blink(4,200);
			}

			// Preheat
			public void preheat(){
				System.out.println("Preheat! Pass this along to startEnginefunction");
				startEngine(true);
			}

			// Drive
			public void drive(String[] directions){
				try {
					// Propulsion
					ma = new EV3MediumRegulatedMotor(MotorPort.A);
					// Steering
					mb = new EV3LargeRegulatedMotor(MotorPort.B);

					// Stop engine if running
					ma.stop();

					if (!engineRunning) {
						//Start engine without preheat
						startEngine(false);
					}
					// Set steering in neutral
					if (mb.getPosition() != 0) {
						// Setting steeting angle to 0
						mb.rotateTo(0);
					}

					for (int i = 0; i < directions.length; i++) {
						String command = "";
						int duration = 0;

						if (directions[i].contains("-")) {
							String[] parts = directions[i].split("-");
							command = parts[0];
							duration = Integer.parseInt(parts[1]);
						} else {
							command = directions[i];
							duration = 0;
						}

						System.out.println("Running command: " + command + " for duration: " + duration);

						switch (command) {
							case "stop":
								ma.stop();
								break;
							case "wait":
								break;
							case "forward":
								ma.forward();
								break;
							case "backward":
								ma.backward();
								break;
							case "rightTurn":
								mb.rotateTo(65);
								break;
							case "leftTurn":
								mb.rotateTo(-65);
								break;
							case "neutral":
								mb.rotateTo(0);
								break;
						}

						Delay.msDelay(duration);
						// end for-loop
					}

					// Set steering back to neutral
					mb.rotateTo(0);

					// Stopping propulsion and steering
					ma.stop();
					mb.stop();

					ma.close();
					mb.close();

				} catch (Throwable t) {
				System.err.println("Failed in drive command. Exception: "
						+ t.getLocalizedMessage());
				t.printStackTrace();
				}

				// Ending route
				if (engineRunning){
					//Start engine without preheat
					stopEngine();
				}

			}

			public void playSample(String sample){
				File f = new File("/home/lejos/programs/sounds/" + sample);
				System.out.println("Playing sound: " + f.toString());
				int returnCode = Sound.playSample(f, 100);
				System.out.println("Play sample return code is: " + returnCode);
			}

			@Override
			public void messageArrived(String topic, final MqttMessage message)
					throws Exception {
				System.out.println("Got message = " + message + " in topic - "
						+ topic);
				LCD.clearDisplay();
				LCD.drawString(message.toString(), 0, 4);
				// lejos.hardware.Sound.playSample(arg0, arg1, arg2, arg3, arg4)
				String cmd = new String(message.getPayload()).trim();
				System.out.println("CMD -----" + cmd + "-----");

				if ("door_lock".equals(cmd)) {
                  	doorLock();
				}
				else if ("door_unlock".equals(cmd))
					doorUnlock();
				else if ("engine_on".equals(cmd)) {
					startEngine(false);
				}
				else if ("engine_off".equals(cmd)) {
					stopEngine();
				}
				else if ("honk_blink".equals(cmd)) {
					System.out.println("Honk/Blink! Blink lights make sound.");
					playSample("honk.wav");
					blink(4,200);
				}
				else if ("honk".equals(cmd)) {
					System.out.println("Honk! Make sound.");
					playSample("honk.wav");
				}
				else if ("blink".equals(cmd)) {
					System.out.println("Blink! Blinking lights.");
					blink(4,200);
				}

				else if ("alarm_on".equals(cmd)) {
					if (alarmOn){
						System.out.println("Alarm On! Alarm already engaged. Doing nothing.");
					} else {
						System.out.println("Alarm On! Starting alarm.");
						alarmMaker = new MusicMaker("alarm.wav", 500);
						alarmMaker.start();
						alarmOn = true;
					}
				}
				else if ("alarm_off".equals(cmd)) {
					if (alarmOn){
						System.out.println("Alarm Off! Stopping alarm.");
						alarmMaker.stop();
						alarmOn = false;
					} else {
						System.out.println("Alarm Off! Alarm already off. Doing nothing.");
					}
				}

				else if ("open_trunk".equals(cmd)) {
					openTrunk();
				}
				else if ("close_trunk".equals(cmd)) {
					closeTrunk();
				}
				else if ("preheat".equals(cmd)) {
					preheat();
				}
				else if ("pickup".equals(cmd)) {
					drive(pickupDirections);
				}
				else if ("park".equals(cmd)) {
					drive(parkDirections);
				}
			}

				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {
					System.out.println("deliveryComplete() : " + token);
				}
			});
			mqtt.subscribe("toy/luigi");
		}



		// Inner music player class to keep playing sounds
		public static class MusicMaker extends Thread {
					String sample;
					int loopDelay;

			public  MusicMaker(String sample, int loopDelay){
				this.sample = sample;
				this.loopDelay = loopDelay;
			}


			public void run() {

				File f = new File("/home/lejos/programs/sounds/" + sample);
				System.out.println("File = " + f.toString());

				while (true) {
					try {
						int returnCode = Sound.playSample(f, 100);
						System.err.println("Play sample return code is: " + returnCode);
						Thread.sleep(loopDelay);
					}
					catch (InterruptedException ie) {
						System.out.println("Exception in main thread: "+ie.getMessage());
					}

				}
			}
		}

	}
