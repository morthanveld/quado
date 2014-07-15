package org.grahn.quado;

/**
 * @author Andreas
 *
 * Designed for 4 propeller flight.
 * 
 * http://blog.oscarliang.net/quadcopter-pid-explained-tuning/
 * https://www.youtube.com/watch?v=YNzqTGEl2xQ
 * 
 * 
 * 
 */

public class FlightSystem 
{
	private static float pi = (float) Math.PI;
	private static float pi2 = pi * pi;
	private static float P = 1.0f;
	private static float I = 1.0f;
	
	private float[] thrust = null;
	
	public FlightSystem()
	{
		thrust = new float[4];
	}
	
	float[] getThrust()
	{
		return thrust;
	}
	
	/*
	 * orientation [-3.141592, 3.141592]
	 */
	void update(Vector3 orientation, Vector3 acceleration, Vector3 targetPose)
	{
		// Normalize offset between -1.0 and 1.0.
		float xOffset = (orientation.x - targetPose.x) / pi2;
		float yOffset = (orientation.y - targetPose.y) / pi2;
		
		thrust[0] = calculateProportionalGain(xOffset);
		thrust[1] = -calculateProportionalGain(xOffset);
		thrust[2] = calculateProportionalGain(yOffset);
		thrust[3] = -calculateProportionalGain(yOffset);
	}
	
	/**
	 * Calculates thrust given the value.
	 * @param value [0.0 - 1.0]
	 * @return thrust % [0.0 - 1.0]
	 */
	float calculateProportionalGain(float value)
	{
		assert(value > 1.0f && value < -1.0f);
		return (value * value * value) * P;
	}
	
	float calculateIntegralGain(float value)
	{
		return 0.0f;
	}
	
	float calculateDerivativeGain(float value)
	{
		return 0.0f;
	}
}
