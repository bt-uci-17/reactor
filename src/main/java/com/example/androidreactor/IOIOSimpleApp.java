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

public class IOIOSimpleApp extends IOIOActivity {
    private TextView leftSensorTextView;
    private TextView rightSensorTextView;

    private ProgressBar leftProgressBar;
    private ProgressBar rightProgressBar;

    private TextView resultTextView;

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

	class Looper extends BaseIOIOLooper {
		private AnalogInput leftAnalogInput;
        private AnalogInput rightAnalogInput;

		private PwmOutput leftPwmOutput;
        private PwmOutput rightPwmOutput;
		private DigitalOutput led;

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
                            IOIOSimpleApp.this,
                            "Connected to IOIO board!",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
		}

		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
			displayNumber(leftAnalogInput.read(), "L");
            displayNumber(rightAnalogInput.read(), "R");

            int leftAnalog = (int) (leftAnalogInput.read() * 1000);
            int rightAnalog = (int) (rightAnalogInput.read() * 1000);

            leftProgressBar.setProgress(leftAnalog);
            rightProgressBar.setProgress(rightAnalog);

            leftPwmOutput.setPulseWidth(500 + (leftAnalog * 2));
            rightPwmOutput.setPulseWidth(500 + (rightAnalog * 2));

            if ((leftAnalog > (rightAnalog - 10)) && (leftAnalog < rightAnalog + 10)) {
                led.write(false); // turn on status LED (counterintuitive)
                resultTextView.setText("yes!");
            } else {
                led.write(true);
                resultTextView.setText("");
            }

			Thread.sleep(10);
		}

		@Override
		public void disconnected() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(
                            IOIOSimpleApp.this,
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
     *             Must be "L", "l', "R", or "r" or else the function will do nothing.
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