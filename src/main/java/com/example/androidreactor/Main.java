package com.example.androidreactor;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This class was renamed to Main for simplicity and understandability.
 *
 * Make sure you have the IOIO board wiring as follows:
 *      - Distance Sensors x2:
 *          - Red: 3.3v
 *          - Black: Ground
 *          - Yellow: Pins 39 (L), 40 (R).
 *      - Servo motors x 2:
 *          - Red: 5v
 *          - Black: Ground
 *          - Yellow: Pins 11 (L), 12 (R).
 * Then, BE SURE TO USE A RELATIVELY NEW BATTERY OR YOU WILL HAVE PROBLEMS!!!!
 * (Trust me on this one -- I banged my head against the wall for 45 minutes solving it).
 */
public class Main extends IOIOActivity {
    private TextView leftSensorTextView;
    private TextView rightSensorTextView;

    private ProgressBar leftProgressBar;
    private ProgressBar rightProgressBar;

    private TextView resultTextView;

    /**
     * Called when the main view is created.
     * @param savedInstanceState
     */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        leftSensorTextView = (TextView) findViewById(R.id.left_textview);
        rightSensorTextView = (TextView) findViewById(R.id.right_textview);

        leftProgressBar = (ProgressBar) findViewById(R.id.left_progressbar);
        rightProgressBar = (ProgressBar) findViewById(R.id.right_progressbar);

        resultTextView = (TextView) findViewById(R.id.result_textview);
	}

    /**
     * Class for looping while the IOIO board is connected.
     */
	class Looper extends BaseIOIOLooper {
		private AnalogInput leftAnalogInput;
        private AnalogInput rightAnalogInput;

		private PwmOutput leftPwmOutput;
        private PwmOutput rightPwmOutput;
		private DigitalOutput led;

        /**
         * Called when the IOIO board is connected
         * @throws ConnectionLostException
         */
		@Override
		public void setup() throws ConnectionLostException {
			led = ioio_.openDigitalOutput(IOIO.LED_PIN, true);

			leftAnalogInput = ioio_.openAnalogInput(39);
            rightAnalogInput = ioio_.openAnalogInput(40);

			leftPwmOutput = ioio_.openPwmOutput(11, 100);
            rightPwmOutput = ioio_.openPwmOutput(12, 100);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(
                            Main.this,
                            "Connected to IOIO board!",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
		}

        /**
         * Loops while the IOIO board is connected. This is where all the action happens.
         * @throws ConnectionLostException
         * @throws InterruptedException
         */
		@Override
		public void loop() throws ConnectionLostException, InterruptedException {

            // Read the sensors only once per loop for increased performance
            float leftAnalogReading = leftAnalogInput.read();
            float rightAnalogReading = rightAnalogInput.read();

            displayNumber(leftAnalogReading, "L");
            displayNumber(rightAnalogReading, "R");

            // Rather than converting from float to int a million times,
            // let's just do it once here
            int leftAnalog = (int) (leftAnalogReading * 1000);
            int rightAnalog = (int) (rightAnalogReading * 1000);

            leftProgressBar.setProgress(leftAnalog);
            rightProgressBar.setProgress(rightAnalog);

            leftPwmOutput.setPulseWidth(500 + (leftAnalog * 2));
            rightPwmOutput.setPulseWidth(500 + (rightAnalog * 2));

            if ((leftAnalog > (rightAnalog - 75)) && (leftAnalog < rightAnalog + 75)) {
                led.write(false); // turn on status LED (counterintuitive)
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resultTextView.setText("yes!");
                    }
                });
            } else {
                led.write(true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resultTextView.setText("");
                    }
                });
            }

			Thread.sleep(10);
		}

		@Override
		public void disconnected() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(
                            Main.this,
                            "Disconnected from IOIO board!",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
		}
	}

	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}

    /**
     * Displays a number on a given side of the screen.
     *
     * @param f The number to display
     * @param side The side of the screen on which to display the number.
     *             Must be "L", "l", "R", or "r" or else the function will do nothing.
     */
	private void displayNumber(float f, String side) {
		final String numberString = String.format("%.2f", f);

        if (side.equals("l") || side.equals("L")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    leftSensorTextView.setText(numberString);
                }
            });
            return;
        }

        if (side.equals("r") || side.equals("R")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    rightSensorTextView.setText(numberString);
                }
            });
        }
	}
}