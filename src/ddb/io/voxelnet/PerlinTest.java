package ddb.io.voxelnet;

import ddb.io.voxelnet.util.PerlinOctaves;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class PerlinTest extends Canvas
{
	private static final int MAP_WIDTH = 48;
	private static final int MAP_HEIGHT = 48;
	private static final float DRAW_SCALE = 8.0f;
	private static final double GEN_SCALE_X = 32;
	private static final double GEN_SCALE_Y = 0.5;
	
	final BufferedImage image;
	final byte[] imageData;

	// Generation seed
	private long seed = 0;
	// Permutation map
	short[] permutation = new short[256];
	// Doubled permutation map
	final short[] p = new short[512];
	// Repetition limit
	final double repeatLimit = 16;
	
	final PerlinOctaves perlinNoise;
	
	private PerlinTest()
	{
		image = new BufferedImage(MAP_WIDTH, MAP_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
		imageData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
		
		/// Perlin AlgoInit Begin ///
		perlinNoise = new PerlinOctaves(8, 0.5);
		reseed();
		///  Perlin AlgoInit End  ///
		generate();
	}
	
	private void reseed()
	{
		// Build the permutation table
		seed = System.currentTimeMillis();
		perlinNoise.seed(seed);
		/*Random random = new Random(seed);
		
		for (int i = 0; i < 256; i++) permutation[i] = (short)i;
		for (int i = 0; i < 256; i++)
		{
			int srcIdx = random.nextInt(permutation.length);
			int destIdx = random.nextInt(permutation.length);
			short temp = permutation[destIdx];
			permutation[destIdx] = permutation[srcIdx];
			permutation[srcIdx] = temp;
		}
		
		System.arraycopy(permutation, 0, p,   0, permutation.length);
		System.arraycopy(permutation, 0, p, 256, permutation.length);*/
	}
	
	private final long StartTime = System.currentTimeMillis();
	
	private void generate()
	{
		// Build the generated map
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				double bX = x;// - image.getWidth() / 2d;
				double bZ = y;// - image.getWidth() / 2d;
				
				double noise = perlinNoise.perlinOctaves(
						(GEN_SCALE_X * bX) / 16f,//GEN_SCALE_X * x / (double) image.getWidth(),
						(GEN_SCALE_Y * bZ) / 16f,//GEN_SCALE_Y * y / (double) image.getHeight(),
						(((System.currentTimeMillis() - StartTime)) / 100000d));//,
						//8,
						//0.9);
				imageData[x + y * image.getWidth()] = (byte) (noise * 255);
			}
		}
	}
	
	/// Perlin Noise Algo ///
	private double fade(double d)
	{
		// Original equation is 6t^5 - 15t^4 + 10t^3
		return d * d * d * (d * (d * 6 - 15) + 10);
	}
	
	// Increment & wrap
	private int inc(int val)
	{
		val++;
		if (repeatLimit > 0) val %= repeatLimit;
		return val;
	}
	
	private double grad(int hash, double x, double y, double z)
	{
		// Pick a random unit vector
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
			case 0x9: return -y + z;
			case 0xA: return  y - z;
			case 0xB: return -y - z;
			case 0xC: return  y + x;
			case 0xD: return -y + z;
			case 0xE: return  y - x;
			case 0xF: return -y - z;
			default: return 0; // never happens
		}
	}
	
	private double lerp(double a, double b, double t)
	{
		return a + t * (b - a);
	}
	
	private double perlinNoise(double x, double y, double z)
	{
		if (repeatLimit > 0)
		{
			x %= repeatLimit;
			y %= repeatLimit;
			z %= repeatLimit;
		}
		
		// Get the unit & partial coordinates
		int xA = (int)x & 0xFF;
		int yA = (int)y & 0xFF;
		int zA = (int)z & 0xFF;
		
		double xP = x-(int)x;
		double yP = y-(int)y;
		double zP = z-(int)z;
		
		// Ease partial values
		double u = fade(xP);
		double v = fade(yP);
		double w = fade(zP);
		
		// Compute Hash values
		int aaa, aab, aba, abb, baa, bab, bba, bbb;
		aaa = p[p[p[    xA ]+    yA ]+    zA ];
		aba = p[p[p[    xA ]+inc(yA)]+    zA ];
		aab = p[p[p[    xA ]+    yA ]+inc(zA)];
		abb = p[p[p[    xA ]+inc(yA)]+inc(zA)];
		baa = p[p[p[inc(xA)]+    yA ]+    zA ];
		bba = p[p[p[inc(xA)]+inc(yA)]+    zA ];
		bab = p[p[p[inc(xA)]+    yA ]+inc(zA)];
		bbb = p[p[p[inc(xA)]+inc(yA)]+inc(zA)];
		
		double x1, x2, y1, y2;
		
		// Lerp everything together
		x1 = lerp(grad (aaa, xP-0, yP-0, zP-0),
				  grad (baa, xP-1, yP-0, zP-0),
				  u);
		x2 = lerp(grad (aba, xP-0, yP-1, zP-0),
				  grad (bba, xP-1, yP-1, zP-0),
				  u);
		y1 = lerp(x1, x2, v);
		
		x1 = lerp(grad (aab, xP-0, yP-0, zP-1),
				  grad (bab, xP-1, yP-0, zP-1),
				  u);
		x2 = lerp(grad (abb, xP-0, yP-1, zP-1),
				  grad (bbb, xP-1, yP-1, zP-1),
				  u);
		y2 = lerp (x1, x2, v);
		
		return (lerp (y1, y2, w)+1)/2;
	}
	
	private double perlinOctaves(double x, double y, double z, int octaves, double persistence)
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
	
	@Override
	public void repaint()
	{
		super.repaint();
	}
	
	private void doDraw()
	{
		final long start = System.currentTimeMillis();
		BufferStrategy strat = getBufferStrategy();
		if (strat == null)
		{
			createBufferStrategy(3);
			return;
		}
		
		//(float)(Math.sin(Math.toRadians((System.currentTimeMillis() - start) / 10d)) * 16.0 + 1.0);
		
		Graphics g = strat.getDrawGraphics();
		g.setColor(Color.DARK_GRAY);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.drawImage(image,
				(int)(getWidth()  - image.getWidth() * DRAW_SCALE) / 2,
				(int)(getHeight() - image.getHeight() * DRAW_SCALE) / 2,
				(int)(image.getWidth()  * DRAW_SCALE),
				(int)(image.getHeight() * DRAW_SCALE),
				null);
		g.dispose();
		strat.show();
		
		//reseed();
		generate();
	}
	
	public final boolean doRun = true;
	public static void main(String[] args)
	{
		PerlinTest p = new PerlinTest();
		JFrame frame = new JFrame("Perlin Test");
		frame.setVisible(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(p);
		frame.setPreferredSize(new Dimension(800, 600));
		frame.pack();
		frame.setVisible(true);
		
		while(p.doRun)
		{
			p.doDraw();
			Toolkit.getDefaultToolkit().sync();
			try
			{
				Thread.sleep(30);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	
}
