package ddb.io.voxelnet.client.render.util;

import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.util.Frustum;
import org.joml.Math;
import org.joml.Matrix4f;

public class Camera
{
	// Position related
	public float x = 0.0f;
	public float y = 0.0f;
	public float z = 0.0f;
	
	public float pitch = 0.0f;
	public float yaw = 0.0f;
	
	// Camera offset
	public float xOff = 0.0f;
	public float yOff = 0.0f;
	public float zOff = 0.0f;
	
	// Perspective Matrix
	public float fov = 0.0f;
	public float zNear = 0.0f;
	public float zFar = 0.0f;
	
	// Matrices
	public final Matrix4f perspectiveMatrix;
	public final Matrix4f viewMatrix;
	private final Matrix4f pvMatrix;
	
	// View frustum
	public Frustum viewFrustum;
	
	public Camera(float fov, float zNear, float zFar)
	{
		this.fov = fov;
		this.zNear = zNear;
		this.zFar = zFar;
		
		this.perspectiveMatrix = new Matrix4f();
		this.viewMatrix = new Matrix4f();
		this.pvMatrix = new Matrix4f();
		viewFrustum = new Frustum();
	}
	
	public Camera(int width, int height)
	{
		this.perspectiveMatrix = new Matrix4f();
		this.viewMatrix = new Matrix4f();
		this.pvMatrix = new Matrix4f();
		
		updateOrtho(width, height);
	}
	
	public void setPosition(float x, float y, float z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void setOffset(float x, float y, float z)
	{
		this.xOff = x;
		this.yOff = y;
		this.zOff = z;
	}
	
	public void setOrientaion(float pitch, float yaw)
	{
		this.pitch = pitch;
		this.yaw = yaw;
	}
	
	/**
	 * Updates the perspective matrix with the given aspect ratio
	 * @param aspect The new aspect ratio for the screen
	 */
	public void updatePerspective(float aspect)
	{
		this.perspectiveMatrix.identity();
		this.perspectiveMatrix.setPerspective((float) Math.toRadians(fov), aspect, zNear, zFar);
		this.viewFrustum.updateShape((float) Math.toRadians(fov), aspect, zNear, zFar);
	}
	
	public void updateOrtho(int width, int height)
	{
		this.perspectiveMatrix.identity();
		this.perspectiveMatrix.ortho(0, width, height, 0, 1, -1);
	}
	
	/**
	 * Updates the view matrix
	 * Also updates the view frustum
	 */
	public void updateView()
	{
		this.viewMatrix.identity();
		this.viewMatrix.rotate((float) -Math.toRadians(pitch), 1.0f, 0.0f, 0.0f);
		this.viewMatrix.rotate((float) -Math.toRadians(yaw), 0.0f, 1.0f, 0.0f);
		this.viewMatrix.translate(-(x + xOff), -(y + yOff), -(z + zOff));
		this.viewFrustum.updateFrustum(viewMatrix, x + xOff, y + yOff, z + xOff);
	}
	
	/**
	 * Updates the camera position to be the same as the player
	 * @param player The player to set the camera position to
	 */
	public void asPlayer(EntityPlayer player, double pt)
	{
		this.x = (float) (player.xPos + player.xVel * pt);
		this.y = (float) (player.yPos + player.yVel * pt);
		this.z = (float) (player.zPos + player.zVel * pt);
		
		this.pitch = player.pitch;
		this.yaw = player.yaw;
	}
	
	/**
	 * Gets the actual transform of the camera
	 * The matrix returned is the combined perspective-view matrix
	 * @return The actual transformation matrix of the camera
	 */
	public Matrix4f getTransform()
	{
		return pvMatrix.identity().mul(perspectiveMatrix).mul(viewMatrix);
	}
	
	public Frustum getViewFrustum() { return viewFrustum; }
}
