package ddb.io.voxelnet.util;

import java.util.Random;

/**
 * Implementation of the Perlin Noise algorithm, with octaves
 * Generates noise in 3 dimensions
 *
 * From: http://flafla2.github.io/2014/08/09/perlinnoise.html
 */
public class PerlinOctaves
{
	// Permutation table
	private final short[] permutation = new short[256];
	// Double permutation table
	private final short[] p2 = new short[512];
	
	// Persistence of higher octaves;
	private final double persistence;
	// Number of octaves to use
	private final int octaves;
	
	// Whether to enable repeating or not
	private boolean repeat = false;
	private double repeatAt = 0;
	
	public PerlinOctaves(int octaves, double persistence)
	{
		this.persistence = persistence;
		this.octaves = octaves;
	}
	
	/**
	 * Seeds the noise generator
	 * @param seed The seed to use
	 */
	public void seed(long seed)
	{
		Random random = new Random(seed);
		
		// Build the permutation table
		for (int i = 0; i < 256; i++)
			permutation[i] = (short)i;
		
		for (int i = 0; i < 256; i++)
		{
			int srcIdx = random.nextInt(permutation.length);
			int destIdx = random.nextInt(permutation.length);
			short temp = permutation[destIdx];
			permutation[destIdx] = permutation[srcIdx];
			permutation[srcIdx] = temp;
		}
		
		// Build the p2 array
		System.arraycopy(permutation, 0, p2,   0, permutation.length);
		System.arraycopy(permutation, 0, p2, 256, permutation.length);
	}
	
	/**
	 * Enables or disables the use of repeating noise
	 * To enable repeating, repeatAt should be positive
	 * To disable repeating, repeatAt should be zero or negative
	 * @param repeatAt The coordinate to repeat at
	 */
	public void setRepeat(double repeatAt)
	{
		if (repeatAt <= 0)
		{
			// Invalid repeatAt, disable repeating
			this.repeat = false;
			return;
		}
		
		this.repeatAt = repeatAt;
		this.repeat = true;
	}
	
	public double perlinNoise(double x, double y, double z)
	{
		if (repeat)
		{
			x %= repeatAt;
			y %= repeatAt;
			z %= repeatAt;
		}
		
		// Get the unit & partial coordinates
		int xA = (int)x & 0xFF;
		int yA = (int)y & 0xFF;
		int zA = (int)z & 0xFF;
		
		double xP = x-(int)x;
		double yP = y-(int)y;
		double zP = z-(int)z;
		
		// Handle negative coordinates
		if (xP < 0) { xP += 1; xA = (xA + 255) & 255; }
		if (yP < 0) { yP += 1; yA = (yA + 255) & 255; }
		if (zP < 0) { zP += 1; zA = (zA + 255) & 255; }
		
		// Ease partial values
		double u = fade(xP);
		double v = fade(yP);
		double w = fade(zP);
		
		// Compute Hash values
		int aaa, aab, aba, abb, baa, bab, bba, bbb;
		aaa = p2[p2[p2[    xA ]+    yA ]+    zA ];
		aba = p2[p2[p2[    xA ]+inc(yA)]+    zA ];
		aab = p2[p2[p2[    xA ]+    yA ]+inc(zA)];
		abb = p2[p2[p2[    xA ]+inc(yA)]+inc(zA)];
		baa = p2[p2[p2[inc(xA)]+    yA ]+    zA ];
		bba = p2[p2[p2[inc(xA)]+inc(yA)]+    zA ];
		bab = p2[p2[p2[inc(xA)]+    yA ]+inc(zA)];
		bbb = p2[p2[p2[inc(xA)]+inc(yA)]+inc(zA)];
		
		double x1, x2, y1, y2;
		
		// Lerp everything together
		x1 = MathUtil.lerp(grad (aaa, xP-0, yP-0, zP-0),
				grad (baa, xP-1, yP-0, zP-0),
				u);
		x2 = MathUtil.lerp(grad (aba, xP-0, yP-1, zP-0),
				grad (bba, xP-1, yP-1, zP-0),
				u);
		y1 = MathUtil.lerp(x1, x2, v);
		
		x1 = MathUtil.lerp(grad (aab, xP-0, yP-0, zP-1),
				grad (bab, xP-1, yP-0, zP-1),
				u);
		x2 = MathUtil.lerp(grad (abb, xP-0, yP-1, zP-1),
				grad (bbb, xP-1, yP-1, zP-1),
				u);
		y2 = MathUtil.lerp (x1, x2, v);
		
		return (MathUtil.lerp (y1, y2, w)+1)/2;
	}
	
	public double perlinOctaves(double x, double y, double z)
	{
		double total = 0;
		double frequency = 1;
		double amplitude = 1;
		double maxValue = 0;
		for(int i=0;i<octaves;i++) {
			total += perlinNoise(x * frequency, y * frequency, z * frequency) * amplitude;
			
			maxValue += amplitude;
			
			amplitude *= persistence;
			frequency *= 2;
		}
		
		return total/maxValue;
	}
	
	private double fade(double d)
	{
		// Original equation is 6t^5 - 15t^4 + 10t^3
		return d * d * d * (d * (d * 6 - 15) + 10);
	}
	
	// Increment & wrap
	private int inc(int val)
	{
		val++;
		if (repeat)
			val %= repeatAt;
		return val;
	}
	
	private double grad(int hash, double x, double y, double z)
	{
		// Calculate the gradient vector from the hash
		switch(hash & 0xF)
		{
			case 0x0: return  x + y;
			case 0x1: return -x + y;
			case 0x2: return  x - y;
			case 0x3: return -x - y;
			case 0x4: return  x + z;
			case 0x5: return -x + z;
			case 0x6: return  x - z;
			case 0x7: return -x - z;
			case 0x8: return  y + z;
			case 0x9:
			case 0xD:
				return -y + z;
			case 0xA: return  y - z;
			case 0xB:
			case 0xF:
				return -y - z;
			case 0xC: return  y + x;
			case 0xE: return  y - x;
			default: return 0; // never happens
		}
	}
	
}
